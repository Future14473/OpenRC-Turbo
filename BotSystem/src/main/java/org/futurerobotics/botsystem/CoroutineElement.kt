package org.futurerobotics.botsystem

import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

/**
 * An element that runs a suspend function, and has utilities for wait for start, etc, just like
 * CoroutineOpMode.
 *
 * @see LinearElement for a variant like LinearOpMode instead (uses blocking, and has a dedicated thread).
 */
abstract class CoroutineElement : BaseElement() {

    /**
     * Override this method and place your coroutine code here.
     *
     * The coroutine may be cancelled if stop is pressed. In this case,`CancellationException` will be
     * thrown whenever suspend functions that wait are called. ***This is different from LinearOpMode***, it is
     * better practice with coroutines, so be mindful whenever you have a suspending function.
     *
     * One may typically start this function using `= coroutineScope { ... }` to launch a series of coroutines that
     * will live and die together (see [coroutineScope]).
     */
    protected abstract suspend fun runElement()

    /**
     * Suspends the current coroutine op mode until start has been pressed.
     *
     * Can be called from _any_ coroutine.
     *
     * @throws CancellationException if coroutine is cancelled.
     */
    protected suspend fun waitForStart() {
        botSystem.waitForStart()
    }

    /**
     * Allows other coroutines to run a bit, when you have nothing to do (calls [yield]).
     *
     * Spin-waiting is generally discouraged for coroutines, but sometimes you have no better option.
     *
     * @throws CancellationException if coroutine is cancelled.
     */
    @Throws(CancellationException::class)
    protected suspend fun idle() {
        yield()
    }

    /**
     * Sleeps for the given amount of milliseconds, or until the coroutine is cancelled.
     *
     * This simply calls [delay].
     *
     * @throws CancellationException if coroutine is cancelled.
     */
    @Throws(CancellationException::class)
    protected suspend fun sleep(milliseconds: Long) {
        delay(milliseconds)
    }

    /**
     * If is started and still running.
     *
     * This will [idle] (call [yield]) if is active, as this is intended for use in loops.
     *
     * *This wil __NOT__ throw cancellation exception if cancelled.*
     */
    protected suspend fun isActive(): Boolean {
        val isActive = isStarted && coroutineContext.isActive
        if (isActive)
            try {
                idle()
            } catch (_: CancellationException) {
            }
        return isActive
    }

    /** Requests to stop the entire system, __not just this element__. To stop just this element, exit [runElement]. */
    protected fun requestOpModeStop() {
        botSystem.stop()
    }

    /**
     * Have we started yet?
     * @see waitForStart
     */
    protected val isStarted: Boolean get() = botSystem.isStarted


    final override fun init() {
        botSystem.coroutineScope.launch {
            runElement()
        }
    }
}
