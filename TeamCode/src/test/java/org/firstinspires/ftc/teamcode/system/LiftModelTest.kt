package org.firstinspires.ftc.teamcode.system

import org.futurerobotics.jargon.linalg.createVec
import org.futurerobotics.jargon.linalg.zeroVec
import org.futurerobotics.jargon.statespace.ExperimentalStateSpace
import org.junit.jupiter.api.Test

@UseExperimental(ExperimentalStateSpace::class)
internal class LiftModelTest {

    @Test
    fun runnerTest() {
        println("HI")
        val runner = LiftModel.getRunner()
        runner.reset(zeroVec(2))
        repeat(20) {
            runner.update(createVec(0.0, 0.0), createVec(0.1, 0.0), null, 0L)
            println(runner.signal)
        }
    }
}
