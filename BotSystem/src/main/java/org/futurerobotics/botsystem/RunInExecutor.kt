package org.futurerobotics.botsystem

import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Switches from suspend to blocking world. Runs the given [callable] in the the given executor,
 * and suspends current coroutine until it is done, even after cancelled.
 *
 * This was made exclusively to emulate LinearOpMode's behavior on stop.
 *
 * Any [InterruptedException] thrown will be rethrown with [CancellationException]
 *
 * When this coroutine's job is cancelled:
 * - The future is cancelled _with interrupt_, and this will WAIT for completion after cancellation.
 * - If an [InterruptedException] or [CancellationException] is thrown after interrupt, it is ignored.
 * - Any other exception thrown by the callable during waiting for cancellation will be rethrown.
 * - Otherwise after successful cancellation this will throw the appropriate [CancellationException].
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
suspend fun <T> runInExecutorAndWait(executor: ExecutorService, callable: () -> T): T {
    val canStart = AtomicBoolean(true)
    val completed = CompletableDeferred<T>()
    //we want to WAIT for completion -- so we have both a job and suspend coroutine.
    try {
        return suspendCancellableCoroutine { cont ->
            val future = executor.submit {
                if (!canStart.compareAndSet(true, false)) return@submit
                val result = runCatching { callable() }.let {
                    val exception = it.exceptionOrNull()
                    if (exception is InterruptedException)
                        Result.failure(CancellationException(null, exception))
                    else it
                }
                completed.completeWith(result)
                cont.resumeWith(result)
            }
            cont.invokeOnCancellation {
                future.cancel(true)
            }
        }
    } catch (e: CancellationException) {
        //may throw twice but that's fine
        if (!canStart.compareAndSet(true, false))
            try {
                withContext(NonCancellable) {
                    completed.await()
                }
            } catch (ignored: CancellationException) {
            }
        throw e
    }
}
