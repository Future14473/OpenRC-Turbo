package org.futurerobotics.botsystem

import java.util.*
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Implementation of [Element] with utilities.
 *
 * One can declare dependencies _during construction_ using:
 * - [dependsOn] function, during construction
 * - [dependency] functions, which return [Property]s that will be initialized later.
 *
 * One can call [onInit] to call functions to run upon initialization.
 *
 * The [botSystem] property will also be initialized upon init.
 *
 * If one wants more initialization functionality, it can override [init] _without_ parameter or
 * use [onInit].
 */
abstract class BaseElement : Element {

    private var delegates: MutableList<BotSystemGettingDelegate<*>>? = mutableListOf()
    private val _dependsOn = HashSet<Class<*>>()
    final override val dependsOn: Set<Class<*>> =
        Collections.unmodifiableSet(_dependsOn)
    /**
     * The bot system, initialized on init.
     */
    protected lateinit var botSystem: BotSystem
        private set

    final override fun init(botSystem: BotSystem) {
        if (this::botSystem.isInitialized) throw IllegalStateException("Double initialization!")
        this.botSystem = botSystem
        delegates!!.forEach {
            it.init(botSystem)
        }
        delegates = null
        init()
    }

    /**
     * performs any additional initialization, if necessary.
     */
    protected open fun init() {}


    /** Declares additional dependencies on the given [classes]. */
    protected fun dependsOn(vararg classes: Class<*>) {
        _dependsOn += classes
    }

    /** Declares additional dependencies on the given [classes]. */
    @JvmSynthetic
    protected fun dependsOn(vararg classes: KClass<*>) {
        _dependsOn += classes.map { it.java }
    }

    /** Declares additional dependencies on the given class */
    @JvmSynthetic
    protected inline fun <reified T > dependsOn() {
        dependsOn(T::class.java)
    }

    /**
     * Adds the given elementClass as a dependency, and returns a [Property] where the will be filled with the actual
     * dependency on [init].
     */
    protected fun <T> dependency(clazz: Class<T>): Property<T> {
        dependsOn(clazz)
        return onInit { get(clazz) }
    }


    /**[dependency]  for Kotlin */
    @JvmSynthetic
    protected inline fun <reified T> dependency(): Property<T> =
        dependency(T::class.java)

    /** [dependency] for Kotlin */
    @JvmSynthetic
    protected fun <T : Any> dependency(clazz: KClass<T>): Property<T> =
        dependency(clazz.java)


    /**
     * Adds the given elementClass as a dependency, and returns a [Property] where the will be filled with the actual
     * dependency plus [getValue] on init.
     */
    protected inline fun <T, R> dependency(clazz: Class<T>, crossinline getValue: T.() -> R): Property<R> {
        dependsOn(clazz)
        return onInit { get(clazz).getValue() }
    }

    /** [dependency] for Kotlin */
    @JvmSynthetic
    protected fun <T : Any, R> dependency(clazz: KClass<T>, getValue: T.() -> R): Property<R> =
        dependency(clazz.java, getValue)

    /** [dependency] for Kotlin */
    @UseExperimental(ExperimentalTypeInference::class)
    @JvmSynthetic
    protected inline fun <reified T , R> dependency(@BuilderInference crossinline getValue: T.() -> R): Property<R> =
        dependency(T::class.java, getValue)


    /**
     * Gets a [Property] which will have the value given by the [getValue] on init, with the [dependency] passed.
     *
     * These will run with properties gotten by [dependency] in the same order they were declared.
     */
    protected fun <R> onInit(getValue: BotSystem.() -> R): Property<R> =
        BotSystemGettingDelegate(getValue).also {
            delegates!! += it
        }


    private class BotSystemGettingDelegate<T>(
        private var getValue: (BotSystem.() -> T)?
    ) : Property<T> {

        private object NoValue

        private var _value: Any? = NoValue

        fun init(botSystem: BotSystem) {
            check(_value === NoValue) { "Double initialization" }
            _value = getValue!!.invoke(botSystem)
            getValue = null
        }

        @Suppress("UNCHECKED_CAST")
        override val value: T
            get() = if (_value === NoValue) error("Not initialized") else _value as T
    }
}

/**
 * Something in which you can retrieve a [value].
 *
 * Can also be used as a kotlin property delegate.
 */
interface Property<out T> {

    val value: T

    @JvmDefault
    fun get() = value

    @JvmSynthetic
    @JvmDefault
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}

inline fun <T, R> Property<T>.map(crossinline transform: (T) -> R): Property<R> = object : Property<R> {
    override val value: R
        get() = transform(this@map.value)
}
