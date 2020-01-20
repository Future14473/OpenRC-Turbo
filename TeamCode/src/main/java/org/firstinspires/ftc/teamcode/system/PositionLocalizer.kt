package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.futurerobotics.botsystem.SyncedElement
import org.futurerobotics.botsystem.get
import org.futurerobotics.jargon.control.EncoderGyroLocalizer
import org.futurerobotics.jargon.linalg.Vec
import org.futurerobotics.jargon.linalg.times
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


class PositionLocalizer : SyncedElement<Pose2d>() {

    init {
        dependsOn<Hardware>()
        loopOn<ControlLoop>()
    }

    private val measurements by dependency<Measurements>()

    private val localizer = EncoderGyroLocalizer(
        localizerModel, useAbsoluteHeading = false
    )

    override fun init() {
        localizer.resetHeadingMeasurement(botSystem.get<Hardware>().gyro!!.angle)
    }

    override suspend fun loop(): Pose2d {
        return localizer.update(measurements.wheelPositions.await(), measurements.heading.await())
    }
}


class VelocityObserver : SyncedElement<Vec>() {

    init {
        dependsOn<Hardware>()
        loopOn<ControlLoop>()
    }

    private val measurements by dependency<Measurements>()


    override suspend fun loop(): Vec {
        val velocityVec = measurements.wheelVelocities.await()
            .append(measurements.angularVelocity.await())
        return localizerModel.botVelFromMotorAndGyroVel * velocityVec
    }
}
