package org.futurerobotics.botsystem

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test

@Suppress("EXPERIMENTAL_API_USAGE")
internal class RandomTests {

    @Test
    fun whatHappensInRunBlockingThrow() {
        val job = Job()
        job.cancel()
        runBlocking {
            job.join()
        }
    }

    @Test
    fun exceptional() {
        val handler = CoroutineExceptionHandler { _, e ->
            e.printStackTrace(System.out)
        }
        val scope = CoroutineScope(handler)
        scope.launch {
            delay(Long.MAX_VALUE)
        }

        scope.launch {
            throw Exception()
        }
        runBlocking {
            scope.coroutineContext[Job]!!.join()
        }
    }

    @Test//init
    fun runFromAnotherScope() = runBlockingTest {
        withTimeout(5000) {
            launch {
                //runOpMode
                coroutineScope {
                    val scopeElement = this

                    //start
                    runBlocking(scopeElement.coroutineContext) {
                        //start that runs task
                        launch {
                            println("Launching background task...")
                            scopeElement.launch {
                                delay(Long.MAX_VALUE)
                            }
                        }.invokeOnCompletion {
                            println("Background task done")
                        }
                    }
                    println("runBlocking done")
                    yield()
                    println("Cancelling...")
                    this.cancel()

                    println("Did cancel...")
                }
                println("Scope done")
            }
        }

    }

    @Test
    fun unstartedJob() = runBlockingTest {
        coroutineScope {
            val parent = coroutineContext[Job]!!
            val creationJob = Job(parent)
            val parentJob = Job(parent)

            fun createJob(block: () -> Int): Deferred<Int> {
                val first = async(creationJob, start = CoroutineStart.LAZY) {
                    withContext(parentJob) {
                        block()
                    }
                }
                return first
            }

            val first = createJob{
                println(1)
                1
            }
            val second = createJob {
                println(2)
                2
            }
            val third = createJob {
                println(3)
                second.start()
                3
            }
            third.start()
            parentJob.complete()
            parentJob.join()
            creationJob.cancel()
        }


    }


    @Test
    fun awaitLazyParent() = runBlockingTest {
        val job = launch {
            launch(start=CoroutineStart.LAZY) {
                println("hey")
            }
        }
        job.join()
    }


// yes, it does.
//    @Test
//    fun doesRunBlockingWait() = runBlocking<Unit> {
//        launch {
//            delay(10000)
//        }
//    }
}
