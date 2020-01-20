package org.futurerobotics.botsystem.util

fun assertionError(message: Any? = null): Nothing = throw AssertionError(message?.toString())
