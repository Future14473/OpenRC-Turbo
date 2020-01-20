package org.futurerobotics.botsystem

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier


/**
 * An element in a [BotSystem]. It is basically a thing that can be added to a [BotSystem], and later retrieved
 * using [BotSystem.get]. It can be identified through any class or interface that that element implements/extends,
 * usually the specific element itself.
 *
 * An element may also retrieve other elements through the [BotSystem] to interact with them during [init].
 * Related, each may also [dependsOn] other elements; if something that something depends on does not exist, a default
 * will try to be made or else an exception will be thrown. In this way by simply depending on another element that
 * other element will be added, and the element is guaranteed to exist during [init].
 *
 * A useful base implementation of [Element] with nice features is available in [BaseElement]
 *
 */
interface Element {

    /**
     * A list of elements that this depends on, identified by class. These can be any classes or interfaces
     * that extend [Element].
     *
     * If a dependency does not exist, a default will try to be made, else an exception will
     * be thrown on [BotSystem] init. If A depends on B, B will have [init] called before A.
     *
     * There can be no circular dependencies.
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

