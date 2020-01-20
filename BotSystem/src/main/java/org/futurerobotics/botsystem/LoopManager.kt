package org.futurerobotics.botsystem

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import org.futurerobotics.botsystem.LoopManager.Companion.getSelfLoopValue
import org.futurerobotics.botsystem.LoopManager.Companion.localLoopAction
import org.futurerobotics.botsystem.LoopManager.Companion.stopSelf
import org.futurerobotics.jargon.running.MaxSpeedRegulator
import org.futurerobotics.jargon.running.getDelayMillis
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


/**
 * An Element that manages the synchronization of several looping systems within a [BotSystem].
 * To use, make a subclass so that this has a unique class identifier.
 *
 * This consists of a number of loop actions, which will each be repeatedly called possibly concurrently
 * in a loop, starting when the bot system starts (and after all other elements have started).
 * During each loop, **all** loop actions will wait before continuing on to the next loop.
 *
 * A `minPeriod` can be specified to limit the speed of the loop to always have at least that period, or 0.0 for
 * as fast as possible.
 *
 * Loop actions can be added by using [addLoop], and they will return an [LoopValue]
 * that represents it. Loops actions can be removed by calling [LoopValue.stop], or
 * From ***within a loop action body***. The method can call [getSelfLoopValue] to get the current loopAction instance
 * or [stopSelf] to stop itself from looping.
 *
 * Subclasses can override [beforeLoop] and [afterLoop] to perform additional actions before and after loops,
 * such as setup values that loops can use.
 *
 * Another loop action or this body can wait for other [LoopValue]s by using [LoopValue.currentValue].
 *
 * @see LoopElement
 */
abstract class LoopManager(
    minPeriod: Double = 0.0
) : BaseElement() {

    private val regulator = MaxSpeedRegulator(minPeriod)
    private val newLoopChannel = Channel<LoopValueImpl<*>>(Channel.UNLIMITED)
    /**
     * The elapsed nanos of the previous loop. This kinda only makes sense to be polled from within a loop.
     */
    var elapsedNanos: Long = 0L
        private set

    /**
     * Adds a loop action, and returns a [LoopValue] representing it.
     *
     * The action can be a suspend function.
     */
    @UseExperimental(ExperimentalCoroutinesApi::class)
    @JvmSynthetic
    fun <T> addLoop(
        action: suspend () -> T
    ): LoopValue<T> {
        val loopAction = LoopValueImpl(action)
        try {
            newLoopChannel.offer(loopAction)
        } catch (e: ClosedSendChannelException) {
            throw IllegalStateException("This looper manager has stopped.")
        }
        return loopAction
    }

    /**
     * Adds a loop action, and returns a [LoopValue] representing it.
     */
    fun <T> addLoop(
        action: Supplier<T>
    ): LoopValue<T> = addLoop {
        action.get()
    }

    /**
     * Adds a loop action, and returns a [LoopValue] representing it.
     */
    fun addLoop(
        action: Runnable
    ): LoopValue<Unit> = addLoop {
        action.run()
    }

    /**
     * Run before each loop.
     *
     * A given [scope] is available to launch coroutines if wanted.
     */
    protected open fun beforeLoop(scope: CoroutineScope) {
    }

    /**
     * Runs after each loop. Maybe cleanup unused values in [beforeLoop].
     */
    protected open fun afterLoop() {
    }

    init {
        onInit {
            scope.launchLoop()
        }
    }

    private fun CoroutineScope.launchLoop() = launch {
        elapsedNanos = 0L
        //run loop
        regulator.start()
        val actions = LinkedList<LoopValueImpl<*>>()
        try {
            botSystem.waitForStart()
            while (isActive) {
                actions.removeItIf {
                    it.isStopped
                        .also { stopped ->
                            if (stopped) it.finalizeStop()
                        }
                }
                //Wait if necessary.
                if (actions.isEmpty()) {
                    actions.add(newLoopChannel.receive())
                    regulator.start()
                }
                //add the rest if any
                while (true) {
                    val action = newLoopChannel.poll() ?: break
                    actions.add(action)
                }
                beforeLoop(this)
                //run them.
                val parentRunningJob = Job(coroutineContext[Job])
                val parentRunningScope = CoroutineScope(coroutineContext + parentRunningJob)
                actions.iterator().let {
                    while (it.hasNext()) {
                        val action = it.next()
                        if (action.isStopped) {
                            action.finalizeStop()
                            it.remove()
                        } else {
                            action.runAsync(parentRunningScope)
                        }
                    }
                }
                parentRunningJob.apply {
                    children.forEach { it.start() }
                    parentRunningJob.complete()
                    parentRunningJob.join()
                }
                afterLoop()
                delay(regulator.getDelayMillis())
                elapsedNanos = regulator.endLoopAndGetElapsedNanos()
            }
        } finally {
            //stop everything
            newLoopChannel.close()
            for (o in actions) o.finalizeStop()
            for (o in newLoopChannel) o.finalizeStop()
        }
    }

    private inline fun <T> MutableIterable<T>.removeItIf(predicate: (T) -> Boolean) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            if (predicate(iterator.next())) iterator.remove()
        }
    }

    companion object {
        internal val localLoopAction = ThreadLocal<LoopValue<*>>()
        /**
         * **When called from within the body of a loop**, returns the current loopAction.
         */
        @JvmStatic
        fun getSelfLoopValue(): LoopValue<*> = localLoopAction.get()
            ?: throw UnsupportedOperationException("Cannot call removeSelf from not within a loop")

        /**
         * **When called from within the body of a loop**, stops itself.
         *
         * returns true if successfully stopped, false if already stopped somewhere else.
         */
        @JvmStatic
        fun stopSelf(): Boolean = getSelfLoopValue().stop()
    }
}

/**
 * Represents a loop action of a [LoopManager] that may return a value [T], or [Nothing]? if no meaningful value is
 * to be returned. This can only be produced by a [LoopManager]
 *
 * You can use this to:
 * - Stop the action from looping via [stop],
 * - Retrieve the currently executing value using [currentValue] or [latestValue]
 *
 * If the loop manager stops (its main coroutine is cancelled) then all [LoopValue]s it is running
 * will be stopped.
 */
interface LoopValue<out T> {

    /**
     * Contains the value produced by this [LoopValue] in the current loop it is running.
     * Other loops can use this to await the value computed by another loop.
     *
     * This is reset every loop. See [latestValue] if you just want the most recent value.
     *
     * May throw IllegalStateException if this loop has not yet run.
     */
    val currentValue: DeferredAdapter<T>

    @JvmDefault
    suspend fun await() = currentValue.await()

    @JvmDefault
    @Throws(InterruptedException::class)
    fun awaitBlocking() = runBlocking { await() }
    /**
     * Gets the latest value produced by this [LoopValue], or `null` if not run.
     */
    val latestValue: T?
    /**
     * If this loop has been stopped via [stop].
     */
    val isStopped: Boolean

    /**
     * Stops this loop action from looping.
     *
     * Returns true if successfully stopped, false if already stopped.
     */
    fun stop(): Boolean
}


private class LoopValueImpl<T>(
    val function: suspend () -> T
) : LoopValue<T> {

    @Volatile
    override lateinit var currentValue: DeferredAdapter<T>

    @Volatile
    override var latestValue: T? = null

    private val stopped = AtomicBoolean(false)

    override fun stop(): Boolean {
        return stopped.compareAndSet(
            false, true
        ).also { stopped ->
            if (stopped)
                currentValue.cancel()
        }
    }

    override val isStopped get() = stopped.get()


    fun finalizeStop() {
        stop()
        val completedValue = CompletableDeferred<T>().also { currentValue = it.adapted() }
        completedValue.completeExceptionally(IllegalStateException("This Loop Action has been stopped"))
    }

    fun runAsync(scope: CoroutineScope) {
        val deferred = scope.async(localLoopAction.asContextElement(value = this)) {
            function().also {
                latestValue = it
            }
        }
        this.currentValue = deferred.adapted()
    }
}

