package org.futurerobotics.botsystem

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking

/**
 * An adapter of a [Deferred] that also adds [awaitBlocking] and [joinBlocking] functions
 * so that it is also possible to do blocking code in java.
 */
class DeferredAdapter<out T>(deferred: Deferred<T>) : Deferred<T> by deferred {

    /**
     * [await]s, but blocking.
     */
    @Throws(InterruptedException::class)
    fun awaitBlocking(): T = runBlocking { await() }

    /**
     * [join]s, but blocking.
     */
    @Throws(InterruptedException::class)
    fun joinBlocking() {
        if (!isCompleted)
            runBlocking { join() }
    }
}

fun <T> Deferred<T>.adapted(): DeferredAdapter<T> = when (this) {
    is DeferredAdapter -> this
    else -> DeferredAdapter(this)
}
