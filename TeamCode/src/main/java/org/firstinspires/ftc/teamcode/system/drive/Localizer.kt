package org.firstinspires.ftc.teamcode.system.drive

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.hardware.getMotorAngles
import org.firstinspires.ftc.teamcode.system.GyroAngle
import org.firstinspires.ftc.teamcode.system.LoopValueElement
import org.firstinspires.ftc.teamcode.system.TheBulkData
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.jargon.control.GlobalToBot
import org.futurerobotics.jargon.linalg.*
import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.math.angleNorm
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.math.toPose
import org.futurerobotics.jargon.model.*

// left odometry
// WheelPosition(Vector2d(-8.5 * cm, 9.5 * cm), -Vector2d(1.0, 0.0), radius),
internal val botVelFromState = run {
    val dummyMotor = MotorModel.fromMotorData(12.0, 1.0, 1.0, 1.0, 1.0)
    val transmission = TransmissionModel.ideal(dummyMotor, 1 / 2.0)
    val radius = 30 * mm
    //right now I'll just use "weighted" pseudo inverse, since we can't count on the wheels (and therefore the drive
    // model) from being too accurate. So Kalman filter is not too great.
    //we weigh the gyroscope and the one working odometry heavily and the wheel encoders not so much.


    val singleOdoModel =
        FixedWheelModel(WheelPosition(Vector2d(-6.5 * cm, -9 * cm), Vector2d(0.0, 1.0), radius), transmission)
    val wheels =
        DriveModel.wheels + singleOdoModel //right odometry
    val wheelModel = KinematicsOnlyDriveModel(wheels) //wheels, odo

    val stateFromBotVel = concatCol(
        wheelModel.motorVelFromBotVel,
        Mat(0, 0, 1)
    )
    stateFromBotVel.pinv()
}

class Localizer
@JvmOverloads constructor(
    initialPose: Pose2d = Pose2d.ZERO,
    private val debug: Boolean = false
) : LoopValueElement<Pose2d>() {

    private val theBulkData: TheBulkData by dependency()
    private val allMotors by dependency(Hardware::class) {
        (wheelMotors ?: error("Wheel motors not found (check config)")) +
                (intakeMotors ?: error("Intake/odometry not found (check config)"))[1]
    }
    private val gyro: GyroAngle by dependency()
    private val telemetry by onInit { tryGet<OpModeElement>()?.opMode?.telemetry }

    private var lastState: Vec? = null
    private var pose: Pose2d = initialPose

    private fun update(allMotorPositions: Vec, rawHeading: Double): Pose2d {
        val heading = angleNorm(rawHeading)
        val currentState = allMotorPositions.append(heading)
        val lastState = this.lastState
        this.lastState = currentState
        if (lastState == null) return pose

        val diff = (currentState - lastState).also {
            val lastIndex = it.size - 1
            it[lastIndex] = angleNorm(it[lastIndex])
        }

        val botPoseDelta = (botVelFromState * diff).toPose()
        return GlobalToBot.trackGlobalPose(botPoseDelta, pose)
            .copy(heading = pose.heading + diff.let { it[it.size - 1] })
            .also { p ->
                pose = p
                if (debug) telemetry?.apply {
                    addLine("Current pose: $pose")
                    val formatted = currentState.asList()
                        .joinToString(", ") { "%4.2f".format(it) }
                    addLine("Current state: $formatted")
                }
            }
    }


    override fun init() {
        allMotors.forEach { it.resetPosition() }
        update(
            zeroVec(allMotors.size),
            gyro.getValueNow()
        )
    }

    override fun loopValue(): Pose2d {
        return update(
            theBulkData.value.getMotorAngles(allMotors),
            gyro.value
        )
    }
}

@TeleOp(name = "Odometry Test", group = "Test")
class OdometryTest : BotSystemsOpMode() {

    override fun getElements(): Array<Element> = arrayOf(
        Localizer(Pose2d.ZERO, true)
    )
}
