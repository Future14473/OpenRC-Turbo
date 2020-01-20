package org.firstinspires.ftc.teamcode

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.coroutineContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("NOTHING_TO_INLINE")
inline fun illArg(message: String): Nothing = throw IllegalArgumentException(message)


suspend fun recoverScope() = CoroutineScope(coroutineContext)

suspend inline fun <T> withCoroutineScope(block: CoroutineScope.() -> T): T = recoverScope().run(block)


@JvmName("lateinitWithMessage")
inline fun <T : Any> customLateinit(crossinline lazyMessage: () -> String): ReadWriteProperty<Any, T> = customLateinit {
    throw IllegalStateException(lazyMessage())
}

fun <T : Any> customLateinit(orElse: () -> Nothing): ReadWriteProperty<Any, T> = LateinitCustom(orElse)


private class LateinitCustom<T : Any>(private var orElse: (() -> Nothing)?) : ReadWriteProperty<Any, T> {
    private var value: T? = null
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return value ?: orElse!!.invoke()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
        orElse = null
    }
}
