package org.futurerobotics.botsystem

import kotlin.reflect.KClass

/**
 * An [Element] which only contains [dependsOn] values.
 */
class DependsOn private constructor(override val dependsOn: Set<Class<out Element>>) : Element {

    constructor(vararg dependsOn: Class<out Element>) :
            this(dependsOn.toHashSet())

    constructor(vararg dependsOn: KClass<out Element>) :
            this(dependsOn.mapTo(HashSet()) { it.java } as Set<Class<out Element>>)

    override fun init(botSystem: BotSystem) {
    }

    override val identifierClass: Class<out Element>? get() = null
}
