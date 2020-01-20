package org.futurerobotics.botsystem

import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass

/**
 * An element which is added to a [LoopManager].
 *
 * One needs to call [loopOn] with a [LoopManager] to specify which loop to loop on.
 *
 * It does so by running a coroutine in [run], given a [SyncScope]. It must call
 * [SyncScope.endLoop] to indicate that is done processing this round.
 *
 * It can also wait for other elements to finish processing via [SyncScope.await], and it can
 * only await elements that this [dependsOn]. It can also await all dependencies that are [SyncedElement]s
 * via [SyncScope.awaitAllDependencies]
 *
 * @see LoopElement
 * @see CoroutineLoopElement
 */
abstract class SyncedElement : BaseElement() {

    private var _manager: Property<LoopManager>? = null

    init {
        onInit {
            if (_manager == null)
                throw IllegalStateException("No loop specified to loop on")
        }
    }

    /**
     * Specifies which [LoopManager] to loop on.
     *
     * It will be added as a dependency.
     *
     * Also returns a [Property] representing that class.
     */
    protected fun <M : LoopManager> loopOn(managerClass: Class<M>): Property<M> {
        if (_manager != null) throw UnsupportedOperationException("Cannot loop on multiple loops")
        return dependency(managerClass).also {
            _manager = it
        }
    }


    /** [loopOn] */
    @JvmSynthetic
    protected fun <M : LoopManager> loopOn(managerClass: KClass<M>): Property<M> = loopOn(managerClass.java)

    /** [loopOn] */
    @JvmSynthetic
    protected inline fun <reified M : LoopManager> loopOn(): Property<M> = loopOn(M::class.java)


    final override fun start() {
        _manager!!.get().addElement(this)
    }

    protected abstract suspend fun SyncScope.run()

    internal suspend fun runAccess(syncScope: SyncScope) {
        with(syncScope) { run() }
    }
}


/**
 * An element which repeatedly calls [loopSuspend], synchronized to a [LoopManager]. The
 * loop is a [suspend] function.
 * This is a [SyncedElement].
 *
 * One needs to call [loopOn] with a [LoopManager] to specify which loop to loop on.
 *
 * Every loop, it will first wait for all other [SyncedElement] that
 * this [dependsOn] before looping.
 *
 * @see SyncedElement
 * @see LoopElement
 */
abstract class CoroutineLoopElement : SyncedElement() {

    override suspend fun SyncScope.run() {
        while (coroutineContext.isActive) {
            awaitAllDependencies()
            loopSuspend()
            endLoop()
        }
    }

    /**
     * Runs the loop.
     *
     * This will first await all other [SyncedElement] or [LoopElement]s that this [dependsOn]
     * to finish looping first.
     */
    protected abstract suspend fun loopSuspend()
}


/**
 * An element which repeatedly calls [loop], synchronized to a [LoopManager].
 * This is a [SyncedElement].
 *
 * One needs to call [loopOn] with a [LoopManager] to specify which loop to loop on.
 *
 * Every loop, it will first wait for all other [SyncedElement] that
 * this [dependsOn] before looping.
 *
 * @see SyncedElement
 * @see LoopElement
 */
abstract class LoopElement : CoroutineLoopElement() {

   final override suspend fun loopSuspend() = loop()

    protected abstract fun loop()
}

/**
 * The interface in which a [SyncedElement] can do sync.
 */
interface SyncScope {

    /**
     * Awaits all [SyncedElement] or [LoopElement] that are a subclass of the
     * given [clazz] before continuing.
     *
     * For example, a dependency.
     */
    suspend fun await(clazz: Class<*>)

    /**
     * Awaits all [SyncedElement]s or [LoopElement]s that are added to the same loop
     * and that this `dependsOn` before continuing.
     */
    suspend fun awaitAllDependencies()

    /**
     * Signals that this is done processing this loop, and awaits the next loop.
     *
     * Any other [SyncedElement]s that are awaiting this element can now continuie.
     */
    suspend fun endLoop()
}
