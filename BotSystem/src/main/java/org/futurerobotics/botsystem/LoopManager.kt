package org.futurerobotics.botsystem

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.toList
import org.futurerobotics.jargon.running.MaxSpeedRegulator
import org.futurerobotics.jargon.running.getDelayMillis
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap


/**
 * An Element that manages the synchronization of several systems within a [BotSystem].
 * To use, make a subclass so that this has a unique class identifier.
 *
 * Then, [SyncedElement]s or [LoopElement]s can add themselves to this [LoopManager].
 * During each loop, all elements will be run (possibly concurrently), and all elements will be waited for to complete before
 * continuing on to the next loop.
 *
 * [SyncedElement]s can chose to await for other [SyncedElement]s. [LoopElement]s are [SyncedElement]s, and will
 * await all other loop elements it [dependsOn] before that loop starts.
 *
 * A `minPeriod` can be specified to limit the speed of the loop to always have at least that period, or 0.0 for
 * as fast as possible.
 *
 * Subclasses can override [beforeLoop] and [afterLoop] to perform additional actions before and after loops,
 * such as setup values that loops can use.
 *
 * @see SyncedElement
 */
abstract class LoopManager(
    private val minPeriod: Double = 0.0
) : BaseElement() {

    private val elementChannel = Channel<SyncedElement>(Channel.UNLIMITED)

    internal fun addElement(element: SyncedElement) {
        try {
            elementChannel.offer(element)
        } catch (e: ClosedSendChannelException) {
            throw IllegalStateException("Cannot add element _after_ start")
        }
    }

    /**
     * The elapsed nanos of the previous loop. This kinda only makes sense to be polled from within a loop.
     */
    @Volatile
    var elapsedNanos: Long = 0L
        private set
    /**
     * The elapsed seconds of the previous loop. This kinda only makes sense to be polled from within a loop.
     */
    val elapsedSeconds: Double get() = elapsedNanos / 1e9

    /**
     * Run before each loop.
     */
    protected open fun beforeLoop() {
    }

    /**
     * Runs after each loop.
     */
    protected open fun afterLoop() {
    }

    init {
        onInit {
            launchLoop()
        }
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private fun launchLoop() = botSystem.coroutineScope.launch {
        //run loop
        botSystem.waitForStart()
        elementChannel.close()
        val elements = elementChannel.toList()
        if (elements.isEmpty()) return@launch
        LoopRunner(elements).run()
    }

    private inner class LoopRunner(elements: List<SyncedElement>) {

        private val identifiedBy = (botSystem as BotSystemImpl).identifiedBy
        private val regulator = MaxSpeedRegulator(minPeriod)
        private val dependantLoopkup = hashMapOf<Class<*>, Collection<Element>>()
        private val elementsMap =
            elements.associateWithTo(HashMap()) { SyncScopeImpl(it) }
        @Volatile
        private var startNextSignal = Job()
        private val numIncomplete = AtomicInteger()
        private val allDoneSignal = Channel<Unit>()

        init {
            elements.forEach { el ->
                el.dependsOn.forEach depLoop@{ dep ->
                    if (dep in dependantLoopkup) return@depLoop
                    val list = ArrayList<Element>()
                    dependantLoopkup[dep] = list
                    elements.forEach { dependant ->
                        if (dep in identifiedBy.getValue(dependant))
                            list += dependant
                    }
                }
            }
        }

        suspend fun run() = coroutineScope {
            //initialize and wait for them all
            numIncomplete.set(elementsMap.size)
            elementsMap.forEach { (element, loop) ->
                launch {
                    loop.endLoop()
                    element.runAccess(loop)
                }
            }
            //wait for them all to init.
            allDoneSignal.receive()

            elapsedNanos = 0L
            regulator.start()
            while (isActive) {
                prepareNextLoop()
                beforeLoop()
                startNextLoop()
                //running
                allDoneSignal.receive()
                afterLoop()
                delay(regulator.getDelayMillis())
                elapsedNanos = regulator.endLoopAndGetElapsedNanos()
            }
        }

        private fun prepareNextLoop() {
            numIncomplete.set(elementsMap.size)
            elementsMap.values.forEach {
                it.currentJob = Job()
            }
        }

        private fun startNextLoop() {
            val oldSignal = startNextSignal
            startNextSignal = Job()
            oldSignal.complete()
        }

        private inner class SyncScopeImpl(val element: Element) : SyncScope {
            @Volatile
            @JvmField
            var currentJob = Job()

            @Suppress("UNCHECKED_CAST")
            override suspend fun <T> await(clazz: Class<T>): T? {
                val dependants = dependantLoopkup[clazz] ?: return null
                dependants.forEach { dependant ->
                    elementsMap[dependant]!!.currentJob.join()
                }
                return dependants.firstOrNull() as T?
            }

            override suspend fun await(syncedElement: SyncedElement) {
                elementsMap[syncedElement]?.currentJob?.join()
            }

            override suspend fun awaitAllDependencies() {
                element.dependsOn.forEach {
                    await(it)
                }
            }

            override suspend fun endLoop() {
                val startNextSignal = startNextSignal
                currentJob.complete()
                if (numIncomplete.decrementAndGet() == 0) {
                    allDoneSignal.send(Unit)
                }
                startNextSignal.join()
            }
        }
    }
}
