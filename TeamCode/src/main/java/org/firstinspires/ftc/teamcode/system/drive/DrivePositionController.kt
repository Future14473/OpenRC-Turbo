package org.firstinspires.ftc.teamcode.system.drive

import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.jargon.control.FeedForwardWrapper
import org.futurerobotics.jargon.control.GlobalToBot
import org.futurerobotics.jargon.control.PIDCoefficients
import org.futurerobotics.jargon.control.PosePIDController
import org.futurerobotics.jargon.math.MotionState
import org.futurerobotics.jargon.math.Pose2d

//contains manual set
class DrivePositionController
@JvmOverloads constructor(
    private val debug: Boolean = false
) : LoopElement(), DriveVelocitySignal {

    private val controlLoop by loopOn<ControlLoop>()
    private val driveTarget: DrivePathFollower by dependency()
    private val localizer: Localizer by dependency()
    private val telemetry by onInit { tryGet<OpModeElement>()?.opMode?.telemetry }

    @Volatile
    var isActive = true

    private val controller = FeedForwardWrapper(
        PosePIDController(
            PIDCoefficients(3.0, 0.0, 0.0),
            PIDCoefficients(3.0, 0.0, 0.0),
            headingCoefficients
        )
    ) { a, b -> a + b }

    override fun init() {
        controller.reset()
    }

    @Volatile
    override var targetVelocity: MotionState<Pose2d> = MotionState.ofAll(Pose2d.ZERO)

    override fun loop() {
        if(!isActive) return
        val curPose = localizer.value
        val reference = driveTarget.targetState
        val motion = GlobalToBot.motion(
            controller.update(reference, curPose, controlLoop.elapsedNanos),
            curPose.heading
        )
        targetVelocity = motion.toVelocityMotionState(Pose2d.ZERO)
        if (debug) telemetry?.apply {
            addLine("Target velocity: ${motion.velocity}")
        }
    }
}
