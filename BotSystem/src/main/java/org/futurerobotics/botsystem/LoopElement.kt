package org.futurerobotics.botsystem

import kotlin.reflect.KClass

/**
 * An element which automatically adds a [loop] to a [LoopManager] upon [start].
 * One needs to call [loopOn] with a [LoopManager] to specify which loop to loop on.
 *
 * Each loop can produce [T] values every loop, which other loop elements can await for.
 * If no meaningful value is to be produced, set [T] to [Unit].
 *
 * The value can then be awaited/polled from other loops.
 *
 * @see LoopProducerJava
 * @see LoopElementJava
 */
abstract class LoopElement<T> : BaseElement() {

    private var _manager: Property<LoopManager>? = null

    init {
        onInit {
            if (_manager == null)
                throw IllegalStateException("No loop specified to loop on");
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
    protected fun <M : LoopManager> loopOn(managerClass: KClass<M>): Property<M> = loopOn(managerClass.java)

    /** [loopOn] */
    protected inline fun <reified M : LoopManager> loopOn(): Property<M> = loopOn(M::class.java)

    /** The [LoopValue] of this [LoopElement]. */
    protected lateinit var loopValue: LoopValue<T>
        private set
    /** The [value] produced by this synced looper, different every loop */
    val value get() = loopValue.currentValue
    /** The latest [value] produced by this synced looper. */
    val latestValue get() = loopValue.latestValue

    final override fun start() {
        loopValue = _manager!!.value.addLoop { loop() }
    }

    /** Loops and maybe produces a value. */
    protected abstract suspend fun loop(): T
}


/**
 * An element which automatically adds a [doLoop] to a [LoopManager] upon [start].
 * One needs to call [loopOn] with a [LoopManager] to specify which loop to loop on.
 *
 * Each loop can produce [T] values every loop, which other loop elements can await for.
 * If no meaningful value is to be produced, see [LoopElementJava].
 *
 * This is different from [LoopElement] in that loop is not a suspend function.
 *
 * @see LoopElement
 * @see LoopElementJava
 */
abstract class LoopProducerJava<T> : LoopElement<T>() {

    final override suspend fun loop() = doLoop()

    /** Loops and produces a value. */
    protected abstract fun doLoop(): T
}

/**
 * An element which automatically adds a [doLoop] to a [LoopManager] upon [start].
 * One needs to call [loopOn] with a [LoopManager] to specify which loop to loop on.
 *
 * This does not produces any value. For a loop that can produce values other loops can poll, see [LoopProducerJava].
 *
 * For kotlin, see [LoopElement]
 */
abstract class LoopElementJava : LoopElement<Unit>() {

    final override suspend fun loop() = doLoop()

    /** Loops. */
    protected abstract fun doLoop()
}
