package org.firstinspires.ftc.teamcode.system.drive

import org.firstinspires.ftc.teamcode.system.ButtonsElement
import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.firstinspires.ftc.teamcode.system.GyroAngle
import org.firstinspires.ftc.teamcode.system.OffsetGyro
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.jargon.control.ExtendedPIDCoefficients
import org.futurerobotics.jargon.control.HeadingPIDController
import org.futurerobotics.jargon.math.Interval
import org.futurerobotics.jargon.math.MotionState
import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.math.convert.*

val headingCoefficients = ExtendedPIDCoefficients(
    3.0, 0.1, 0.0,
    errorBounds = Interval.symmetric(30 * deg)
)

class ManualDriveVelocitySignal
@JvmOverloads constructor(
    private val debug: Boolean = false
) : LoopElement(), DriveVelocitySignal {

    companion object {
        const val maxAxisSpeed = 1.4 * m / s
        const val maxAngularSpeed = 140 * deg / s
        const val slowSpeedMultiplier = 0.3
    }

    private val controlLoop by loopOn<ControlLoop>()
    private val opMode by dependency(OpModeElement::class) { opMode }
    private val buttons by dependency(ButtonsElement::class) { buttons1 }
    private val gamepad get() = opMode.gamepad1
    private val telemetry get() = opMode.telemetry
    private val speedMultiplier get() = (slowSpeedMultiplier) + (gamepad.left_trigger) * (1.0 - slowSpeedMultiplier)

    private val gyro: GyroAngle by dependency()

    private val headingController =
        HeadingPIDController(
            headingCoefficients
        )

    private val angle = OffsetGyro()
    @Volatile
    override var targetVelocity: MotionState<Pose2d> = MotionState.ofAll(Pose2d.ZERO)

    private var targetHeading = 0.0

    override fun loop() {
        val rawMeasured = gyro.value
        angle.update(rawMeasured)
        val actualAngle = buttons.run {
            when {
                dpad_up.isClicked -> 0.0
                dpad_left.isClicked -> 90 * deg
                dpad_right.isClicked -> -90 * deg
                dpad_down.isClicked -> 180 * deg
                else -> Double.NaN
            }
        }
        if (!actualAngle.isNaN()) {
            angle.resetOffset(actualAngle)
        }
        val vx = -gamepad.right_stick_y * maxAxisSpeed
        val vy = -gamepad.right_stick_x * maxAxisSpeed
        val actualHeading = angle.angle

        val angularVelocity = -gamepad.left_stick_x * maxAngularSpeed
        targetHeading += angularVelocity * controlLoop.elapsedSeconds
        val headingVelocity =
            headingController.update(targetHeading, actualHeading, controlLoop.elapsedNanos) + angularVelocity

        val targetVelocity =
            Pose2d(vx, vy, headingVelocity) * speedMultiplier

        this.targetVelocity = MotionState(targetVelocity, Pose2d.ZERO, Pose2d.ZERO)

        if (debug) {
            telemetry.addLine("Raw measured eading: %4.2f".format(actualHeading))
            telemetry.addLine("Measured Heading: %4.2f".format(actualHeading))
            telemetry.addLine("Target Heading  : %4.2f".format(targetHeading))
        }
    }
}
