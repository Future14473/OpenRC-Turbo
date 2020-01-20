package org.futurerobotics.botsystem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

@UseExperimental(ExperimentalCoroutinesApi::class)
class LoopSystemTest {

    @Test
    fun test() = runBlockingTest {
        val botSystem = BotSystem.create(
            this, DependsOn(Slow::class, Printer::class, Waiter::class)
        )
        botSystem.init()
        botSystem.start()
    }
}


class TestLoopSystem : LoopManager()


class Slow : LoopElement<Nothing?>(TestLoopSystem::class) {
    override suspend fun loop(): Nothing? {
        println("Slow start")
        delay(1000)
        println("Slow end")
        println()
        return null
    }

    override fun init() {
        botSystem.scope.launch {
            delay(8000)
            botSystem.stop()
        }
    }
}

class Printer : LoopElement<Nothing?>(TestLoopSystem::class) {
    override suspend fun loop(): Nothing? {
        println("   Printer")
        return null
    }
}

class Medium : LoopElement<Int>(TestLoopSystem::class) {
    override suspend fun loop(): Int {
        delay(500)
        return Math.random().times(400).roundToInt().also {
            println("       Sending $it")
        }
    }
}


class Waiter : LoopElement<Int>(TestLoopSystem::class) {
    private val medium: Medium by dependency()
    override suspend fun loop(): Int {
        println("           Awaiting")
        val value = medium.value.await()
        println("           got $value")
        return value
    }
}
