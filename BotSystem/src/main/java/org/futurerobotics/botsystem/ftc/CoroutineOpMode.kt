package org.futurerobotics.botsystem.ftc

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.TimestampedI2cData
import com.qualcomm.robotcore.util.RobotLog
import kotlinx.coroutines.*
import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryInternal
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Base class for user defined coroutine op modes for awesome concurrency stuff.
 * This is meant to be similar to `LinearOpMode`, while using coroutines.
 *
 * A [CoroutineContext] can be specified to be used.
 *
 * Instead of `Thread.interrupted`, the coroutine will be cancelled when
 * the op mode is stopped. **In this case,`CancellationException` will be thrown whenever (most)
 * suspend functions are called.** If cleanup is wanted, use either a try/finally block,
 * or explicitly check for coroutines being active using [CoroutineContext.isActive].
 *
 * This can only be used once -- a new lifecycle needs a new class. If you are using a opModeRegistrar use
 * the class rather than an instance.
 */
abstract class CoroutineOpMode(coroutineContext: CoroutineContext = EmptyCoroutineContext) : OpMode() {

    private val startJob = Job()
    private var exception: Throwable? = null
    private val scope = CoroutineScope(coroutineContext)
    private val job get() = scope.coroutineContext[Job]!!


    /**
     * Override this method and place your awesome coroutine code here.
     *
     * The coroutine may be cancelled if stop is pressed. In this case,`CancellationException` will be
     * thrown whenever suspend functions that wait are called. ***This is different from LinearOpMode***, it is
     * better practice with coroutines, so be mindful whenever you have a suspending function.
     *
     * One may typically start this function using `= coroutineScope { ... }` to launch a series of coroutines that
     * will live and die together (see [coroutineScope]).
     */
    protected abstract suspend fun runOpMode()

    /**
     * Suspends the current coroutine op mode until start has been pressed.
     *
     * Can be called from _any_ coroutine.
     *
     * @throws CancellationException if coroutine is cancelled.
     */
    protected suspend fun waitForStart(): Unit = startJob.join()

    /**
     * Allows other coroutines to run a bit, when you have nothing to do (calls [yield]).
     *
     * Spin-waiting is generally discouraged for coroutines, but sometimes you have no better option.
     *
     * @throws CancellationException if coroutine is cancelled.
     */
    protected suspend inline fun idle(): Unit = yield()

    /**
     * Sleeps the given amount of milliseconds, or until the coroutine is cancelled.
     *
     * This simply calls [delay].
     *
     * @throws CancellationException if coroutine is cancelled.
     */
    protected suspend inline fun sleep(milliseconds: Long): Unit = delay(milliseconds)

    /**
     * Answer as to whether this opMode is active.
     *
     * **This may not be the same as if the current coroutine is active. For that, use `coroutineContext.isActive`.**
     *
     * Note that internally this method calls [idle], _but will __NOT__ throw [CancellationException] when cancelled.
     * This emulates the behavior in [LinearOpMode].
     */
    protected suspend fun opModeIsActive(): Boolean {
        val isActive = !isStopRequested && isStarted
        if (isActive)
            withContext(NonCancellable) {
                yield()
            }
        return isActive
    }

    /**
     * Has the op mode been started (start button is pressed)?
     * @see waitForStart
     */
    protected val isStarted: Boolean get() = startJob.isCompleted

    /**
     * Has the the stopping of the opMode been requested?
     *
     * _Note that this can differs from the __current coroutine__ being active or not; it only checks if the
     * main op mode coroutine is running._
     *
     * @return whether stopping opMode has been requested or not
     * @see isStarted
     */
    protected val isStopRequested: Boolean get() = job.isCancelled

    private fun throwNotRunning(): Nothing = error("CoroutineOpMode is not running!")

    /** From the normal op mode */
    final override fun init() {
        // Since this loops forever in the background doing nothing useful
        val curThread = Thread.currentThread()
        curThread.priority = (curThread.priority - 1).coerceAtLeast(Thread.MIN_PRIORITY)
        launchOpMode()
    }

    /** From the normal op mode */
    final override fun init_loop() {
        doLoop()
    }

    /** From the normal op mode */
    final override fun start() {
        startJob.complete()
    }

    /** From the normal op mode */
    final override fun loop() {
        doLoop()
    }

    /** From the normal op mode */
    final override fun stop() {
        job.cancel("Op mode stop")
        runBlocking {
            job.join()
        }
        exception = null
    }

    private fun doLoop() {
        Thread.yield()
        exception?.let { throw it }
    }

    private fun launchOpMode() {
        exception = null
        scope.launch {
            RobotLog.vv(TAG, "CoroutineOpMode starting...")
            try {
                runOpMode()
                requestOpModeStop()
            } catch (e: CancellationException) {
                RobotLog.dd(
                    TAG, "CoroutineOpMode received an CancellationException; shutting down this coroutine op mode"
                )
                //may have manually cancelled scope.
                requestOpModeStop()
                throw e //normal.
            } catch (e: Exception) {
                exception = e
            } finally {
                //from linear op mode
                //flush telemetry
                TimestampedI2cData.suppressNewHealthWarningsWhile {
                    val telemetry = telemetry
                    if (telemetry is TelemetryInternal) {
                        telemetry.msTransmissionInterval = 0
                        telemetry.tryUpdateIfDirty()
                    }
                }
                RobotLog.vv(TAG, "...terminating CoroutineOpMode")
            }
        }
    }

    /***/
    final override fun internalPostInitLoop() {
        (telemetry as? TelemetryInternal)?.tryUpdateIfDirty()
    }

    /***/
    final override fun internalPostLoop() {
        (telemetry as? TelemetryInternal)?.tryUpdateIfDirty()
    }

    private companion object {
        const val TAG = "CoroutineOpMode"
    }
}
