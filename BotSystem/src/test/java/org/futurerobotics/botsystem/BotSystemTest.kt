package org.futurerobotics.botsystem

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

class BotSystemTest {
    @UseExperimental(ExperimentalCoroutinesApi::class)
    @Test
    fun successfulStart() = runBlockingTest {
        DebugProbes.install()
        val system = BotSystem.create(
            this,
            DependsOn(Dependency1::class),
            LoopAFewTimes(),
            Receive()
        )
        val launchJob = launch {
            system.init()
            system.start()
        }
        try {
            withTimeout(3000) {
                launchJob.join()
            }
        } catch (e: TimeoutCancellationException) {
            println("Timed out:")
            DebugProbes.dumpCoroutines()
            launchJob.cancel()
        }
    }
}

private class Dependency1 : BaseElement() {
    private val aThing by dependency(Dependency2::class) { thing }

    override fun init() {
        println(aThing)
    }
}

private class Dependency2 : BaseElement(/*Dependency1::class*/) {
    val thing = 0
    override fun init() {
    }
}


private class LoopAFewTimes : LinearElement() {

    override fun runElement() {
        repeat(5) {
            delay(50)
            println(it)
        }
    }
}

private class Send : CoroutineElement() {
    val channel = Channel<String>()

    @UseExperimental(ExperimentalCoroutinesApi::class)
    override suspend fun runElement() = coroutineScope<Unit> {
        repeat(10) {
            channel.send("Hey $it")
            delay(25)
        }
        channel.close()
    }
}

private class Receive : CoroutineElement() {
    private val send: Send by dependency()

    override suspend fun runElement() {
        for (s in send.channel) {
            println(s)
        }
    }
}
