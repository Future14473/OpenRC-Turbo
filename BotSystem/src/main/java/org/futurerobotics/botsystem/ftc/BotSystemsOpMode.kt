package org.futurerobotics.botsystem.ftc

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.futurerobotics.botsystem.BotSystem
import org.futurerobotics.botsystem.Element
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * An op mode which runs a [BotSystem], given some `initialElements`.
 *
 * It will call [BotSystem.init] on init, and [BotSystem.start] on start.
 *
 * - [OpModeElement] will be added to the given elements that contains this op mode.
 *
 * One can also override the [additionalRun] function to run additional actions, and use like [CoroutineOpMode].
 *
 * The [botSystem] will then be available.
 */
abstract class BotSystemsOpMode(
    initialElements: Collection<Element>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) : CoroutineOpMode(coroutineContext) {

    constructor(vararg initialElements: Element) : this(initialElements.asList())

    private val elements = (initialElements.asSequence() + OpModeElement(this))
        .groupBy { it.identifierClass }
        .entries.map { (cls, list) ->
        if (cls != null)
            require(list.size == 1) { "Cannot have two elements with the same identifier" }
        list.first()
    }

    protected lateinit var botSystem: BotSystem
        private set


    final override suspend fun runOpMode() = coroutineScope {
        botSystem = BotSystem.create(this, elements)
        botSystem.init()
        launch {
            additionalRun()
        }
        waitForStart()
        botSystem.start()
    }

    protected open suspend fun additionalRun() {
    }
}
