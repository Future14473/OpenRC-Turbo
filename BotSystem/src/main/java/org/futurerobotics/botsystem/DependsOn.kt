package org.futurerobotics.botsystem

import kotlin.reflect.KClass

/**
 * An [Element] which only contains [dependsOn] values.
 */
class DependsOn private constructor(override val dependsOn: Set<Class<*>>) : Element {

    constructor(vararg dependsOn: Class<*>) :
            this(dependsOn.toHashSet())

    constructor(vararg dependsOn: KClass<*>) :
            this(dependsOn.mapTo(HashSet()) { it.java } as Set<Class<*>>)

    override fun init(botSystem: BotSystem) {
    }
}
