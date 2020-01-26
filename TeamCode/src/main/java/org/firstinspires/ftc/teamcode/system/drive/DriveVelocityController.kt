package org.firstinspires.ftc.teamcode.system.drive

import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.hardware.getMotorVelocities
import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.firstinspires.ftc.teamcode.system.TheBulkData
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.jargon.control.ExtendedPIDCoefficients
import org.futurerobotics.jargon.control.PIDControllerArray
import org.futurerobotics.jargon.linalg.*
import org.futurerobotics.jargon.math.MotionState
import org.futurerobotics.jargon.math.Pose2d

interface DriveVelocitySignal {
    val targetVelocity: MotionState<Pose2d>
}

//This simply uses a PID controller for each wheel.
class DriveVelocityController
@JvmOverloads constructor(
    private val debug: Boolean = false
) : LoopElement() {

    private val controlLoop by loopOn<ControlLoop>()
    private val bulkData: TheBulkData by dependency()
    private val hardware: Hardware by dependency()
    private val driveVelocity: DriveVelocitySignal by dependency()
    private val telemetry by onInit { tryGet<OpModeElement>()?.opMode?.telemetry }

    //D causes problems
    private val controller = PIDControllerArray(
        4, ExtendedPIDCoefficients(
            0.22, 0.2, 0.00,
            maxIntegralContribution = 2.0
        )
    )

    override fun init() {
        controller.reset()
    }

    override fun loop() {
        val driveModel = DriveModel.model
        val bulkData = bulkData.value
        val wheels = hardware.wheelMotors!!
        val targetMotion = driveVelocity.targetVelocity

        val botVel = targetMotion.value.toVec()
        val botAccel = targetMotion.velocity.toVec()
        val wheelVel = driveModel.motorVelFromBotVel * botVel
        val wheelAccel = driveModel.motorAccelFromBotAccel * botAccel

        val measuredWheelVel = bulkData.getMotorVelocities(wheels)
        val rawSignal = controller.update(wheelVel, measuredWheelVel, controlLoop.elapsedNanos)

        val feedForward = DriveModel.voltPerAccel * wheelAccel + DriveModel.voltPerVel * wheelVel
        val finalSignal = rawSignal + feedForward

        wheels.forEachIndexed { i, motor ->
            motor.voltage = finalSignal[i]
        }

        if (debug) telemetry?.run {
            fun Double.format() = "%4.2f".format(this)
            fun Vec.format() = buildString {
                append('[')
                repeat(size) {
                    append(this@format[it].format())
                    append(' ')
                }
                append(']')
            }
            addLine("Target posev: ${botVel.format()}")
            addLine("Target wheel: ${wheelVel.format()}")
            addLine("Actual wheel: ${measuredWheelVel.format()}")
            addLine("Raw signal  : ${rawSignal.format()}")
            addLine("Signal      : ${finalSignal.format()}")
        }
    }
}
