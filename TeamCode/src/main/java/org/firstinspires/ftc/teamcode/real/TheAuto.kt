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
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.pathing.backwards
import org.futurerobotics.jargon.pathing.graph.Waypoint
import org.futurerobotics.jargon.util.replaceIf

private val startLoading = Pose2d(x = -(1 * tile + .5 * robot), y = (3 * tile - .5 * robot), heading = right)

abstract class AbstractAuto(
    private val fieldSide: FieldSide = FieldSide.Blue
) : BotSystemsOpMode() {

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

    override fun getElements() = arrayOf(
        ControlLoop(false),
        hardware,
        extension,
        rotater,
        liftTarget,
        LiftController(),
        Localizer(startLoading.replaceIf(fieldSide.mirrored) { it.mirrored() }, true),
        pathFollower,
        DrivePositionController(true),
        DriveVelocityController()
    )

    private lateinit var previousEnd: Waypoint

    private fun move(side: BotSide, vararg waypoints: Waypoint): Job {
        previousEnd = waypoints.last()
        return pathFollower.followPath(
            createPath(*waypoints)
                .replaceIf(side.backwards) {
                    it.backwards()
                })
    }

    private val Any.d: Unit
        get() {
            RobotLog.dd("DEBUG~~~", toString())
        }

    private val bridgePositions = arrayOf(
        Waypoint(-.8 * tile, 1.2 * tile, direction = up, heading = up, secondDeriv = Vector2d.ZERO),
        Waypoint(.8 * tile, 1.2 * tile, direction = up, heading = up, secondDeriv = Vector2d.ZERO)
    )


    private val grabFoundationPlace = Vector2d(2.3 * tile, 0.5 * tile)

    override suspend fun additionalRun() = coroutineScope {
        //THE ACTUAL RUNNING STARTS HERE
        extension.targetAngle = 0.0
        rotater.targetAngle = 0.0
        waitForStart()
        grabSkystone()
        move(
            BotSide.Output,
            previousEnd.reverseDirection(),
            *bridgePositions,
            Waypoint(grabFoundationPlace + Vector2d(0.0, 0.4 * tile), direction = right),
            Waypoint(grabFoundationPlace, direction = right)
        ).join()
        intake.power = 0.0
        hardware.claw!!.close()
        hardware.grabbers!!.forEach { it.close() }
        val releaseJob = launch {
            delay(800)
            extension.targetAngle = OutputMechanism.extendMaxAngle
            delay(1000)
            hardware.claw!!.open()
            delay(300)
            liftTarget.liftHeight += 5 * `in`
            delay(600)
            extension.targetAngle = 0.0
            delay(600)
            liftTarget.liftHeight = -2 * `in`
        }
        delay(1500)
        move(
            BotSide.Intake,
            Waypoint(grabFoundationPlace, heading = left),
            Waypoint(1.3 * tile, 1.7 * tile, heading = down),
            Waypoint(1.3 * tile, 2.3 * tile, heading = down),
            Waypoint(2.5 * tile, 2.3 * tile, direction = down)
        ).join()
        hardware.grabbers!!.forEach { it.open() }
        delay(400)
        releaseJob.join()
        move(
            BotSide.Intake,
            previousEnd,
            Waypoint(
                2.0 * tile,
                1.0 * tile,
                direction = down,
                secondDeriv = Vector2d.ZERO
            ), Waypoint(
                0.2 * tile,
                1.35 * tile,
                heading = down,
                direction = down
            )
        ).join()
        delay(1000)
        requestOpModeStop()
        extension.targetAngle = 0.5
    }

    protected open suspend fun grabSkystone(): Unit = OpenCvDetector(hardwareMap.appContext).use { cvDetector ->
        cvDetector.begin()
        val number: Int = withTimeoutOrNull(5000) {
            var nothingFoundFrames = 0
            while (true) {
                delay(50)
                val elements: Array<SkyStone> = cvDetector.skyStones
                if (elements.isEmpty()) {
                    telemetry.addData("Nothing found", "For " + nothingFoundFrames + "frames")
                    nothingFoundFrames++
                } else { //detected
                    val xpos = elements.min()!!.x
                    telemetry.addData("Position", xpos)
                    //The center of the screen has x value of 320
                    val xRelToCenter = (xpos - 320).toInt()

                    return@withTimeoutOrNull when {
                        xRelToCenter < -70 -> 1
                        xRelToCenter in -70..70 -> 0
                        else -> -1
                    }.also {
                        telemetry.addData("Found at", it)
                        telemetry.update()
                    }
                    //higher
                }
                telemetry.update()
            }
            throw AssertionError()
        }?.let {
            if (fieldSide.mirrored) -it else it
        } ?: run {
            telemetry.addLine("Gave up looking")
            telemetry.update()
            0
        }
        //higher - to the bot's right
        val targetPosition = startLoading.vec.copy(y = 0.35 * tile) +
                Vector2d(-9 * `in`, 0.0) * number +
                Vector2d(-0.3 * tile, 0.0)
        intake.power = 1.0
        val endAngle = right + 60 * deg
        val job = move(
            BotSide.Intake,
            Waypoint(startLoading),
            Waypoint(targetPosition, direction = endAngle, derivMagnitude = 1.0)
        )
        job.join()
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

@Autonomous
class BlueLoadingAuto : AbstractAuto(FieldSide.Blue)

@Autonomous
class RedLoadingAuto : AbstractAuto(FieldSide.Red)
