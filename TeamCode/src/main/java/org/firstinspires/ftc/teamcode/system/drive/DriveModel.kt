package org.firstinspires.ftc.teamcode.system.drive

import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.model.*
import kotlin.math.pow
import kotlin.math.sqrt

object DriveModel {

    val wheels: List<FixedWheelModel>
    @Suppress("UNREACHABLE_CODE")
    val model: FixedWheelDriveModel

    init {
        val length = 18 * inches
        val mass: Double = 34 * lbs
        val moi = 1.0 / 2 * mass * length.pow(2)

        val motor = MotorModel.fromMotorData(
            12.0,
            260 * ozf * `in`,
            9.2 * A,
            435 * rev / mins,
            0.25 * A
        )
        val transmission =
            TransmissionModel.fromTorqueMultiplier(
                motor,
                2.0 * sqrt(2.0),
                10 * ozf * `in`,
                0.95
            )
        //inlined...
        val horizontalRadius = 16 * `in` / 2
        val verticalRadius = 13.25 * `in` / 2
        val orientations = arrayOf(
            -45 * deg, 44.99 * deg, //problems with singular matrices...
            45 * deg, -45 * deg
        ).map { Vector2d.polar(1.0, it) }
        val locations = arrayOf(
            Vector2d(verticalRadius, horizontalRadius), Vector2d(verticalRadius, -horizontalRadius),
            Vector2d(-verticalRadius, horizontalRadius), Vector2d(-verticalRadius, -horizontalRadius)
        )
        wheels = orientations.zip(locations) { o, l ->
            val location = WheelPosition(l, o, 2 * `in`/* * sqrt(2.0)*/)
            FixedWheelModel(location, transmission)
        }
        model = FixedWheelDriveModel(
            mass,
            moi,
            wheels
        )
    }

    const val voltPerAccel = 0.1
    const val voltPerVel = 0.23
}

