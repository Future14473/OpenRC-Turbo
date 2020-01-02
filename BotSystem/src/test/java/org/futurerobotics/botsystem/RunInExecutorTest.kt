package org.futurerobotics.botsystem

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class RunInExecutorTest {

    @Test
    fun willItWait() = runBlocking {

        val executor = Executors.newSingleThreadExecutor()
        try {
            val backgroundLaunch = launch {
                try {

                    runInExecutorAndWait(executor) {
                        try {
                            println("B: Running and sleeping")
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                            println("B: Interrupted, sleeping again")
                            Thread.sleep(100)
                            println("B: Done sleeping again")
                        } finally {
                            println("B: Done executing")
                        }
                    }
                } catch (e: CancellationException) {
                    println("Cancellation thrown")
                    throw e
                }
            }
            delay(20)
            println("A: Cancelling and joining background")
            backgroundLaunch.cancel()
            val time = measureTimeMillis {
                backgroundLaunch.join()
            }
            println("A: Join done took $time millis ")
        } finally {
            executor.shutdownNow()
        }

    }

    @Test
    fun throwOnCancel() = runBlocking {

        val executor = Executors.newSingleThreadExecutor()
        try {
            val backgroundLaunch = launch {
                try {
                    runInExecutorAndWait(executor) {
                        try {
                            println("B: Running and sleeping")
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                            println("B: Interrupted, throwing")
                            throw RuntimeException()
                        } finally {
                            println("B: Done executing")
                        }
                    }
                } catch (e: RuntimeException) {
                    println("caught $e")
                }
            }
            delay(20)
            println("A: Cancelling and joining background")
            backgroundLaunch.cancel()
            val time = measureTimeMillis {
                backgroundLaunch.join()
            }
            println("A: Join done took $time millis ")
        } finally {
            executor.shutdownNow()
        }

    }
}
