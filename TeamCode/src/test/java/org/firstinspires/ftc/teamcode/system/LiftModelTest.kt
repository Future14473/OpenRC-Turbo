package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.system.LiftModel.spoolRadius
import org.firstinspires.ftc.teamcode.system.LiftModel.ugff
import org.futurerobotics.jargon.linalg.*
import org.futurerobotics.jargon.statespace.ExperimentalStateSpace
import org.junit.Test
import kotlin.math.roundToLong
import kotlin.math.sin

@UseExperimental(ExperimentalStateSpace::class)
internal class LiftModelTest {

    @Test
    fun runnerTest() {
        val periodNanos = (ControlLoop.PERIOD * 1e9).roundToLong()
        val controller = SingleLiftController()
        controller.reset(zeroVec(2))
        repeat(40) {
            //so its .3m in the air, according to measurements.
            controller.update(createVec(0.0, 0.0), createVec(.3 / spoolRadius, 0.0), periodNanos)
            println("Signal: ${controller.signal}")
            println("State : ${controller.state}")
        }
    }

    @Test
    fun observerTest() {
        val periodNanos = (ControlLoop.PERIOD * 1e9).roundToLong()
        val signalFunction = { it: Int ->
            val time = it * ControlLoop.PERIOD
            9 * sin(time)
        }
        val observer = LiftModel.getObserver()
        observer.reset(createVec(0.0, 0.0, -ugff))
        val model = ModelWithGravity()
        var timeNanos = 0L
        repeat(200) {
            val signal = signalFunction(it)
            val measure = model.state[0..1] / spoolRadius //lets say its very accurate
            model.update(signal)
            observer.update(LiftModel.matrices, createVec(signal), measure, timeNanos)
            println("Signal: $signal")
            println("State : ${observer.currentState}")
            println("Actual : ${model.state}")
            println()
            timeNanos += periodNanos
        }
    }

    private class ModelWithGravity {
        var state = createVec(0.0, 0.0, -ugff)

        fun update(u: Double): Vec {
            //TODO: add process function
            val (a, b) = LiftModel.matrices
            state = a * state + b * (createVec(u - ugff))
            return state
        }
    }
}
