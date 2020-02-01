package org.firstinspires.ftc.teamcode.system.drive

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.jargon.control.TimeOnlyMotionProfileFollower
import org.futurerobotics.jargon.math.MotionState
import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.pathing.Path
import org.futurerobotics.jargon.profile.*
import org.futurerobotics.jargon.util.replaceIf

enum class FieldSide(val mirrored: Boolean) {
    Blue(false),
    Red(true)
}

enum class BotSide(val backwards: Boolean) {
    Intake(false),
    Output(true)
}

//TODO: unify drive
class DrivePathFollower(
    fieldSide: FieldSide,
    initialPose: Pose2d = Pose2d.ZERO,
    private val debug: Boolean = false
) : LoopElement() {

    private val reversed = fieldSide.mirrored

    private val controlLoop by loopOn<ControlLoop>()
    private val motionProfileFollower = TimeOnlyMotionProfileFollower<MotionState<Pose2d>>()
    private val telemetry by onInit { tryGet<OpModeElement>()?.opMode?.telemetry }


    fun followTrajectory(traj: TimeProfiled<MotionState<Pose2d>>): Job {
        val withCallbacks = traj.withCallbacks()
        val job = withCallbacks.addJob(AtEnd())
        motionProfileFollower.queueProfile(withCallbacks)
        return job
    }

    fun followPath(path: Path): Job = botSystem.coroutineScope.launch {
        val trajectory = motionConstraints.generateTrajectory(path, MotionProfileGenParams.DEFAULT)
        followTrajectory(trajectory).join()
    }

    @Volatile
    var targetState: MotionState<Pose2d> =
        MotionState(initialPose.replaceIf(reversed) { it.mirrored() }, Pose2d.ZERO, Pose2d.ZERO)
        private set

    override fun init() {
        motionProfileFollower.reset(targetState)
    }

    override fun loop() {
        motionProfileFollower.update(controlLoop.elapsedNanos)
        targetState = motionProfileFollower.output.let {
            if (reversed) {
                it.map { p -> p.mirrored() }
            } else it
        }
        if (debug) telemetry?.apply {
            addLine("Target pose: ${targetState.value}")
        }
    }
}

fun Pose2d.mirrored() = Pose2d(x, -y, -heading)
