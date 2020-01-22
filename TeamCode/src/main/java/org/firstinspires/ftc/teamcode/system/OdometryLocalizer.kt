package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.hardware.getMotorAngles
import org.futurerobotics.botsystem.get
import org.futurerobotics.jargon.control.EncoderGyroLocalizer
import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.model.*

// ODO

@Suppress("UNREACHABLE_CODE")
private val localizerModel = run {
    val dummyMotor = MotorModel.fromMotorData(12.0, 1.0, 1.0, 1.0, 1.0)
    val transmission = TransmissionModel.ideal(dummyMotor, 2.0)
    val wheels =
        listOf(
            WheelPosition(TODO("MEASURE ODOMETRY ACTUAL POSITIONS"), Vector2d(1.0, 0.0), TODO()),
            WheelPosition(TODO(), Vector2d(0.0, 1.0), TODO())
        )
    val wheelsAboutCenter = wheels.map { a -> FixedWheelModel(a, transmission) }
    KinematicsOnlyDriveModel(wheelsAboutCenter)
}


class OdometryLocalizer : LoopValueElement<Pose2d>() {
    private val theBulkData: TheBulkData by dependency()
    private val hardware: Hardware by dependency()

    private val localizer = EncoderGyroLocalizer(localizerModel, useAbsoluteHeading = false)

    override fun init() {
        localizer.resetHeadingMeasurement(botSystem.get<Hardware>().gyro!!.angle)
    }

    override fun loopValue(): Pose2d {
        val wheelPositions = theBulkData.value.getMotorAngles(hardware.odometryMotors!!)
        val angle = hardware.gyro!!.angle
        return localizer.update(wheelPositions, angle)
    }
}
