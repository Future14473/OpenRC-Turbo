package org.firstinspires.ftc.teamcode.real

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.util.RobotLog
import detectors.FoundationPipeline.SkyStone
import detectors.OpenCvDetector
import kotlinx.coroutines.*
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.system.*
import org.firstinspires.ftc.teamcode.system.drive.*
import org.firstinspires.ftc.teamcode.system.drive.dimensions.*
import org.futurerobotics.botsystem.BaseElement
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.jargon.math.MotionState
import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.pathing.backwards
import org.futurerobotics.jargon.pathing.graph.Waypoint
import org.futurerobotics.jargon.util.replaceIf
import kotlin.math.abs

private val startLoading = Pose2d(x = -(1 * tile + .5 * robot), y = (3 * tile - .5 * robot), heading = right)

abstract class AbstractAuto(
    private val fieldSide: FieldSide = FieldSide.Blue
) : BotSystemsOpMode() {

    //    private val tracker = GraphTracker(graph, "start loading")
    private val pathFollower = DrivePathFollower(fieldSide, initialPose = startLoading, debug = true)
    private val hardware = Hardware()
    private val intake = object {
        var power: Double = 0.0
            set(value) {
                hardware.intakeMotors!!.forEach {
                    it.voltage = value * 12
                }
                field = value
            }
    }
    private val extension = Extension()
    private val rotater = Rotater()
    private val liftTarget = object : LiftTarget, BaseElement() {
        @Volatile
        override var liftHeight: Double = 0.0
        @Volatile
        override var liftVelocity: Double = 0.0
    }
    private val localizer = Localizer(startLoading.replaceIf(fieldSide.mirrored) {
        it.mirrored()
    })

    override fun getElements(): Array<out Element> {

        return arrayOf(
            ControlLoop(false),
            hardware,
            pathFollower,
            extension,
            rotater,
            //        skyStoneDetector,
            liftTarget,
            LiftController(),
            localizer,
            DriveVelocityController(),
            DrivePositionController()
        )
    }


//    private fun moveTo(name: String, botSide: BotSide): Job {
//        RobotLog.d("Moving to %s", name)
//        return pathFollower.followPath(tracker.pathTo(name, botSide))
//    }

    private lateinit var previousEnd: Waypoint

    private fun move(side: BotSide, vararg waypoints: Waypoint): Job {
        side.d
        previousEnd = waypoints.last()
        return pathFollower.followPath(
            createPath(*waypoints)
                .replaceIf(side.backwards) {
                    "BACKWARDS".d
                    it.backwards()
                })
    }

    private val Any.d: Unit
        get() {
            RobotLog.dd("DEBUG~~~", toString())
        }

    private val bridgePositions = arrayOf(
        Waypoint(-.5 * tile - .5 * robot, 1.4 * tile, direction = up, heading = up, secondDeriv = Vector2d.ZERO),
        Waypoint(1.0 * tile + .5 * robot, 1.4 * tile, direction = up, heading = up, secondDeriv = Vector2d.ZERO)
    )


    private val look: Pose2d = Pose2d(-2.5 * tile, -2.5 * tile, right)
    private val grabPlace = Vector2d(1.7 * tile, 0.8 * tile)

    override suspend fun additionalRun() = coroutineScope {
        //THE ACTUAL RUNNING STARTS HERE

        extension.targetAngle = 0.0
        rotater.targetAngle = 0.0
        waitForStart()
        move(
            BotSide.Intake,
            Waypoint(startLoading),
            Waypoint(look)
        ).join()
        grabSkystone()
        launch {
            delay(4000)
            hardware.grabbers!!.forEach { it.close() }
        }
        move(
            BotSide.Output,
            previousEnd.reverseDirection(),
            *bridgePositions,
            Waypoint(grabPlace, heading = right)
        ).join()
        intake.power = 0.0
//        hardware.claw!!.close()
//        val releaseJob = launch {
//            delay(800)
//            extension.targetAngle = OutputMechanism.extendMaxAngle
//            delay(600)
//            hardware.claw!!.open()
//            delay(300)
//            liftTarget.liftHeight += 5 * `in`
//            delay(600)
//            extension.targetAngle = 0.0
//            delay(600)
//            liftTarget.liftHeight = -2 * `in`
//        }
//        delay(1500)
//        move(
//            BotSide.Intake,
//            Waypoint(grabPlace, heading = left),
//            Waypoint(grabPlace + Vector2d(-.3 * tile, 0.0), direction = left, heading = left),
//            Waypoint(1.6 * tile, 2.3 * tile, heading = down, direction = down)
//        ).join()
//        hardware.grabbers!!.forEach { it.open() }
//        delay(200)
//        delay(400)
//        releaseJob.join()
//        move(
//            BotSide.Intake,
//            previousEnd,
//            Waypoint(
//                1.5 * tile,
//                1.0 * tile,
//                heading = down,
//                direction = down,
//                derivMagnitude = 1.0,
//                secondDeriv = Vector2d.ZERO
//            ),
//            Waypoint(-0.1 * tile, 1.2 * tile, heading = down, direction = down)
//        ).join()
//        delay(1000)
//        requestOpModeStop()
//        extension.targetAngle = 0.5
    }

    protected open suspend fun grabSkystone(): Unit = OpenCvDetector(hardwareMap.appContext).use {
        it.begin()
        val positionController = botSystem.get<DrivePositionController>()
        positionController.isActive = false

        fun Pose2d.velToState() = MotionState(this, Pose2d.ZERO, Pose2d.ZERO)

        withTimeoutOrNull(5000) {
            var nothingFoundFrames = 0
            while (true) {
                delay(50)
                val elements: Array<SkyStone> = it.skyStones
                if (elements.isEmpty()) {
                    telemetry.addData("Nothing found", "For " + nothingFoundFrames + "frames")
                    nothingFoundFrames++
                } else { //detected
                    val xpos = elements.min()!!.x
                    telemetry.addData("Position", xpos)
                    //The center of the screen has x value of 320
                    val difference = (xpos - 380).toInt()

                    //75 pixels ought to be close enough
                    if (abs(difference) < 75) {
                        telemetry.addLine("Aligned to SkyStone!")
                        break
                    }
                    if (difference > 0) {
                        telemetry.addData("Moving", "Right")
                        positionController.targetVelocity = Pose2d(0.0, -0.1, 0.0).velToState()
                    } else {
                        telemetry.addData("Moving", "Left")
                        positionController.targetVelocity = Pose2d(0.0, 0.1, 0.0).velToState()
                    }
                }
                telemetry.update()
            }
        } ?: run {
            telemetry.addLine("Gave up looking")
            telemetry.update()
        }
        positionController.targetVelocity = Pose2d.ZERO.velToState()
        delay(100)
        val currentPosition = localizer.value.vec
        val targetPosition = currentPosition.copy(y = 0.9 * tile)
        intake.power = 1.0
        move(BotSide.Intake, Waypoint(currentPosition), Waypoint(targetPosition, heading = right)).join()
        delay(200)
    }
}

//todo add these more officially
private fun Waypoint.reverseDirection(): Waypoint {
    return Waypoint(position, constraint.reverseDirection())
}


private fun Waypoint(pose: Pose2d): Waypoint {
    return Waypoint(pose.vec, heading = pose.heading)
}

private val shortCircuitSkystoneLocation = Vector2d(-2 * tile - block - 3 * `in`, tile - 3 * `in`)

@Autonomous
class BlueLoadingAuto : AbstractAuto(FieldSide.Blue)

@Autonomous
class RedLoadingAuto : AbstractAuto(FieldSide.Red)
