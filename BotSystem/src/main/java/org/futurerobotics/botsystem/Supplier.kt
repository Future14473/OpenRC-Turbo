package org.futurerobotics.botsystem

/**
 * A supplier interface like [java.util.function.Supplier] For android compatibility.
 */
interface Supplier<T> {

    fun get(): T
}
