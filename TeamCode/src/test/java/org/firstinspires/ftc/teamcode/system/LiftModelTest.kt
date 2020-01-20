package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.ROBOT_LOOP_PERIOD
import org.futurerobotics.jargon.linalg.createVec
import org.futurerobotics.jargon.linalg.zeroVec
import org.futurerobotics.jargon.statespace.ExperimentalStateSpace
import org.junit.Test
import kotlin.math.roundToInt

@UseExperimental(ExperimentalStateSpace::class)
internal class LiftModelTest {

    @Test
    fun runnerTest() {
        val runner = LiftModel.getRunner()
        runner.reset(zeroVec(2))
        var time = 0L
        repeat(20) {
            runner.update(createVec(0.0, 0.0), createVec(0.1, 0.0), null, time)
            println(runner.currentState)
            time += (ROBOT_LOOP_PERIOD * 1e9).roundToInt()
        }
    }
}
