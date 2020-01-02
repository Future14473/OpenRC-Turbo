package org.futurerobotics.botsystem

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import org.futurerobotics.botsystem.SyncedLoop.Companion.getSelfLoopAction
import org.futurerobotics.jargon.running.MaxSpeedRegulator
import org.futurerobotics.jargon.running.getDelayMillis
import java.util.*


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
 * Loop actions can be added by using [addLoopAction], and they will return an [LoopAction]
 * that represents it. Loops actions can be removed by calling [LoopAction.stop],
 * or from within the body of the loop calling [stopSelf].
 *
 * From ***within a loop action body***:
 * - the method can call [getSelfLoopAction] to get the current loopAction instance or [stopSelf] to stop itself
 *  from looping.
 * - One can wait for other [LoopAction]s running _in the same [SyncedLoop]_ by using [LoopAction.await],
 *   or [SyncedLooper.await]
 *
 * Example use cases:
 * - A control loop where data reads must be made. A [SyncedLooper] can be made that always creates the data,
 * and other [Element]s who use it can be added to the same loop, add that element as a dependency,
 * and call [SyncedLooper.await] on it.
 *
 * @see SyncedLooper
 */
abstract class SyncedLoop(
    minPeriod: Double = 0.0
) : BaseElement() {


    private val regulator = MaxSpeedRegulator(minPeriod)
    private val loopActionChannel = Channel<LoopAction<*>>(Channel.UNLIMITED)
    /**
     * The elapsed nanos of the previous loop. This kinda only makes sense to be polled from within a loop.
     */
    var elapsedNanos: Long = 0L
        private set

    /**
     * Adds a _suspend_ loop action, and returns a [LoopAction] representing it.
     */
    @UseExperimental(ExperimentalCoroutinesApi::class)
    @JvmSynthetic
    fun <T> addLoopAction(
        action: suspend () -> T
    ): LoopAction<T> {
        val loopAction = LoopAction(this, action)
        try {
            loopActionChannel.offer(loopAction)
        } catch (e: ClosedSendChannelException) {
            throw IllegalStateException("This looper is done looping.")
        }
        return loopAction
    }

    /**
     * Adds a loop action, and returns a [LoopAction] representing it.
     */
    fun <T> addLoopAction(
        action: Supplier<T>
    ): LoopAction<T> = addLoopAction {
        action.get()
    }

    /**
     * Adds a loop action, and returns a [LoopAction] representing it.
     */
    fun addLoopAction(
        action: Runnable
    ): LoopAction<Nothing?> = addLoopAction {
        action.run()
        null
    }

    /**
     * When called from within a loop, stops itself. It will not be looped a second time.
     */
    @JvmSynthetic
    @JvmName("stopSelfMember")
    fun stopSelf() {
        Companion.stopSelf()
    }

    final override fun init() {
        botSystem.scope.doLoop()
    }

    private fun CoroutineScope.doLoop() = launch {
        elapsedNanos = 0L
        //run loop
        regulator.start()
        val actions = LinkedList<LoopAction<*>>()
        val jobs = ArrayList<Job>()
        try {
            botSystem.waitForStart()
            while (isActive) {
                //first remove stopped
                actions.iterator().let {
                    while (it.hasNext()) {
                        val action = it.next()
                        if (action.isStopped) {
                            action.finalizeStop()
                            it.remove()
                        }
                    }
                }
                //then make sure we have some, waiting if necessary.
                if (actions.isEmpty()) {
                    actions.add(loopActionChannel.receive())
                    regulator.start()
                }
                //add the rest, no waiting.
                while (true) {
                    val action = loopActionChannel.poll() ?: break
                    actions.add(action)
                }
                //run them.
                jobs.clear()
                //coroutineScope waits for children
                coroutineScope {
                    actions.iterator().let {
                        while (it.hasNext()) {
                            val action = it.next()
                            if (action.isStopped) {
                                action.finalizeStop()
                                it.remove() //check again for stopped
                            } else {
                                jobs += action.lazyInvokeAsync(this@coroutineScope)
                            }
                        }
                    }
                    jobs.forEach { it.start() }
                }
                //delay appropriate time.
                delay(regulator.getDelayMillis())
                elapsedNanos = regulator.endLoopAndGetElapsedNanos()
            }
        } finally {
            //stop everything
            loopActionChannel.close()
            for (o in actions) o.finalizeStop()
            for (o in loopActionChannel) o.finalizeStop()
        }
    }

    companion object {
        internal val localLoopAction = ThreadLocal<LoopAction<*>>()
        /**
         * **When called from within the body of a loop**, returns the current loopAction.
         */
        @JvmStatic
        fun getSelfLoopAction(): LoopAction<*> = localLoopAction.get()
            ?: throw UnsupportedOperationException("Cannot call removeSelf from not within a loop")

        /**
         * **When called from within the body of a loop**, stops itself.
         *
         * returns true if successfully stopped, false if already stopped somewhere else.
         */
        @JvmStatic
        fun stopSelf(): Boolean = getSelfLoopAction().stop()
    }
}

/**
 * Represents a loop action that may return a value [T], or [Nothing]? if no meaningful value is
 * to be returned. This can only be produced by a [SyncedLoop]
 *
 * You can use this to:
 * - Stop the action form looping via [stop],
 * - Wait or check the loop to be stopped using [awaitStop]/[isStopped]
 *
 * If the loop manager stops (its main coroutine is cancelled) then all [LoopAction]s it is running
 * will be stopped.
 */
class LoopAction<out T> internal constructor(
    private val owner: SyncedLoop,
    private val function: suspend () -> T
) {

    private var isFirst = true

    private var deferred: Deferred<T> = CompletableDeferred()
        set(value) {
            if (isFirst) {

                isFirst = false
            }


            field = value
        }

    /**
     * Awaits the current loop of this [LoopAction] to complete, and also returns its produced value which can
     * optionally be used.
     *
     * ***This can only be called from within a LoopAction body with the same [SyncedLoop].***
     */
    @JvmSynthetic
    suspend fun await(): T = checkCallerFirst { deferred.await() }

    /**
     * Awaits the current loop of this [LoopAction] to complete, and also returns its produced value which can
     * optionally be used.
     *
     * ***This can only be called from within a LoopAction body with the same [SyncedLoop].***
     */
    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun awaitBlocking(): T = checkCallerFirst {
        runBlocking { deferred.await() }
    }

    /**
     * If the current loop is completed.
     *
     * ***This can only be called from within a LoopAction body with the same [SyncedLoop].***
     */
    val isCompleted: Boolean
        get() = checkCallerFirst { deferred.isCompleted }

    private inline fun <T> checkCallerFirst(block: () -> T): T {
        if (getSelfLoopAction().owner !== this.owner) throw UnsupportedOperationException(
            "Cannot wait for current loop from a different loop manager!"
        )
        return block()
    }

    private val stopped = Job()

    /**
     * Stops this loop action from looping.
     *
     * Returns true if successfully stopped, false if already stopped.
     */
    fun stop(): Boolean {
        return stopped.complete()
    }

    internal fun finalizeStop() {
        stop()
        deferred = CompletableDeferred<T>().also {
            it.completeExceptionally(IllegalStateException("This Loop Action has been stopped"))
        }
    }

    /**
     * If this loop has been stopped via [stop].
     */
    val isStopped get() = stopped.isCompleted

    /**
     * Waits until loop is stopped.
     */
    @JvmSynthetic
    suspend fun awaitStop() {
        stopped.join()
    }

    /**
     * Waits until loop is removed, blocking.
     *
     * Throws [InterruptedException] if thread is interrupted while waiting.
     */
    @Throws(InterruptedException::class)
    fun awaitStopBlocking() = runBlocking { awaitStop() }

    internal fun lazyInvokeAsync(scope: CoroutineScope): Job =
        scope.async(
            SyncedLoop.localLoopAction.asContextElement(value = this),
            start = CoroutineStart.LAZY
        ) {
            function()
        }.also {
            deferred = it
        }
}

