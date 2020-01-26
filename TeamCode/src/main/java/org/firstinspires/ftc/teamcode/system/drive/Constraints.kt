package org.firstinspires.ftc.teamcode.system.drive

import org.futurerobotics.jargon.pathing.trajectory.*

val motionConstraints = run {
    val model = DriveModel.model
    MotionConstraintSet(
        MaxTangentVelocity(1.5),
        MaxAngularVelocity(2.0),
        MaxTotalAcceleration(3.0),
        MaxAngularAcceleration(3.0),
        MaxMotorVoltage(11.0, model, model, model)
    )
}
