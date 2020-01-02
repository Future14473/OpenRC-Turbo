package org.futurerobotics.botsystem

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier


/**
 * An element in a [BotSystem]. It is basically a thing that can be added to a [BotSystem], and later retrieved
 * using [BotSystem.get]. It is identified by a [identifierClass] which is _usually_ the class of the element
 * itself. There can only be one element for one given [identifierClass].
 *
 * An element may also retrieve other elements through the [BotSystem] to interact with them during [init].
 * Related, each may also [dependsOn] other elements; if something that something depends on does not exist, a default
 * will try to be made or else an exception will be thrown. In this way by simply depending on another element that
 * other element will be added, and the element is guaranteed to exist during [init].
 *
 * Defaults can either be made through:
 * - _a class's_ static method with signature exactly `static T createDefault()` where `T` is the same or subclass of
 *   the current class, and any access modifier.
 * - A public no parameters constructor.
 *
 * I would use companion objects instead of classes but we want to make this more java-friendly.
 */
interface Element {

    /**
     * The class used to identify this [Element].
     *
     * This is what is used to identify dependencies in [dependsOn].
     */
    @JvmDefault
    val identifierClass: Class<out Element>?
        get() = this::class.java

    /**
     * A list of elements that this depends on, identified through their [identifierClass]. These will be guaranteed
     * to exist when [init] is called.
     *
     * There can be no circular dependencies.
     *
     * If a dependency does not exist, a default will try to be made. see [Element] doc. If A depends on B, B will
     * have [init] called before A.
     */
    val dependsOn: Set<Class<out Element>>

    /**
     * Initializes. This function should be quick and should not be a long operation, but it can kick off something
     * else.
     *
     * Other elements can be retrieved by the given [BotSystem], and will contain everything
     * this [dependsOn].
     *
     * Other [Element]s are only guaranteed to already have been [init]ed if this [dependsOn] it.
     */
    fun init(botSystem: BotSystem)

    companion object {

        internal fun <T : Element> tryCreateDefault(elementClass: Class<T>): T? =
            if (Modifier.isAbstract(elementClass.modifiers)) null
            else try {
                elementClass.getConstructor().newInstance()
            } catch (e: NoSuchMethodException) {
                null
            } catch (e: InvocationTargetException) {
                throw e.cause!!
            }
    }

    /**
     * Called on start. Should not be a long operation (can kick off something else).
     */
    @JvmDefault
    fun start() {
    }
}

