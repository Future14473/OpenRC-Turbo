package org.futurerobotics.botsystem

import kotlin.reflect.KClass

/**
 * An element which automatically subscribes to a [SyncedLoop] upon [start], always looping [loop].
 * Each loop can produce [T] values every loop. If no meaningful value is to be produced. Set a [T] value of `Nothing?`
 * and a return value of `null`.
 *
 * The value can then be polled from other loops subscribed to the same [SyncedLoop] or simply
 * be waited for.
 *
 * @see SyncedLooperJava
 */
abstract class SyncedLooper<T>(clazz: KClass<out SyncedLoop>) : BaseElement() {

    private val manager by dependency(clazz)
    /** The [LoopAction] of this [SyncedLooper]. */
    lateinit var loopAction: LoopAction<T>
        private set

    /**
     * Awaits the current loop to complete, and also returns its produced value which can
     * optionally be used.
     *
     * ***This can only be called from within a LoopAction body within the same [SyncedLoop].***
     */
    @JvmSynthetic
    suspend fun await(): T = loopAction.await()

    /**
     * Awaits the current loop to complete, and also returns its produced value which can
     * optionally be used.
     *
     * ***This can only be called from within a LoopAction body within the same [SyncedLoop].***
     */
    fun awaitBlocking(): T = loopAction.awaitBlocking()

    final override fun start() {
        loopAction = manager.addLoopAction { loop() }
    }

    /**
     * Loops, suspending, and maybe produces a value.
     */
    protected abstract suspend fun loop(): T
}

/**
 * An element which automatically subscribes to a [SyncedLoop] upon [start], always looping [loopSuspend].
 * Each loop can produce [T] values every loop. If no meaningful value is to be produced. Set a [T] value of `Nothing?`
 * and a return value of `null`.
 *
 * The value can then be polled from other loops subscribed to the same [SyncedLoop] or simply
 * be waited for.
 *
 * @see [SyncedLooper]
 */
abstract class SyncedLooperJava<T>(clazz: Class<out SyncedLoop>) : BaseElement() {

    private val manager by dependency(clazz)
    private lateinit var loopAction: LoopAction<T>

    /**
     * Awaits the current loop to complete, and also returns its produced value which can
     * optionally be used.
     *
     * ***This can only be called from within a LoopAction body within the same [SyncedLoop].***
     */
    fun awaitBlocking(): T = loopAction.awaitBlocking()

    override fun start() {
        loopAction = manager.addLoopAction { loop() }
    }

    /**
     * Loops and maybe produces a value.
     */
    protected abstract fun loop(): T
}
