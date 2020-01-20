package org.futurerobotics.botsystem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
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


class Slow : CoroutineLoopElement() {
    init {
        loopOn<TestLoopSystem>()
    }

    override suspend fun loopSuspend() {
        println("Slow start")
        delay(1000)
        println("Slow end")
        println()
    }

    override fun init() {
        botSystem.scope.launch {
            delay(8000)
            botSystem.stop()
        }
    }
}

class Printer : LoopElement() {
    init {
        loopOn<TestLoopSystem>()
    }

    override fun loop() {
        println("   Printer")
    }
}

class Medium : CoroutineLoopElement() {
    init {
        loopOn<TestLoopSystem>()
    }

    @Volatile
    var value = 0
        private set

    override suspend fun loopSuspend() {
        delay(500)
        value = Math.random().times(400).roundToInt().also {
            println("       Sending $it")
        }
    }
}


class Waiter : LoopElement() {
    init {
        loopOn<TestLoopSystem>()
    }

    private val medium: Medium by dependency()
    override fun loop() {
        val value = medium.value
        println("           got $value")
    }
}
