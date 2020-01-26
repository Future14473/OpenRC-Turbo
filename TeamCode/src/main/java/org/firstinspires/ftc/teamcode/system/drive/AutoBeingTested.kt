package org.firstinspires.ftc.teamcode.system.drive

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.util.RobotLog
import kotlinx.coroutines.*
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.system.*
import org.firstinspires.ftc.teamcode.system.drive.dimensions.*
import org.futurerobotics.botsystem.BaseElement
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.pathing.backwards
import org.futurerobotics.jargon.pathing.graph.Waypoint
import org.futurerobotics.jargon.pathing.graph.WaypointConstraint
import org.futurerobotics.jargon.util.replaceIf
import kotlin.math.PI

//right now: loading side
@Autonomous
class AutoBeingTested
@JvmOverloads constructor(
    fieldSide: FieldSide = FieldSide.Blue
) : BotSystemsOpMode() {

    //    private val tracker = GraphTracker(graph, "start loading")
    private val pathFollower = DrivePathFollower(fieldSide, initialPose = startLoading, debug = true)
    private val hardware = Hardware()
    //    private val skyStoneDetector = SkystoneDetector()
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

    override fun getElements(): Array<out Element> = arrayOf(
        ControlLoop(false),
        hardware,
        pathFollower,
        extension,
        rotater,
//        skyStoneDetector,
        liftTarget,
        LiftController(),
        Localizer(startLoading),
        DriveVelocityController(),
        DrivePositionController()
    )


//    private fun moveTo(name: String, botSide: BotSide): Job {
//        RobotLog.d("Moving to %s", name)
//        return pathFollower.followPath(tracker.pathTo(name, botSide))
//    }

    private lateinit var previousEnd: Waypoint

    fun move(side: BotSide, vararg waypoints: Waypoint): Job {
        side.d
        previousEnd = waypoints.last()
        return pathFollower.followPath(createPath(*waypoints)
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
        Waypoint(bridgeEnter, direction = up, heading = up, secondDeriv = Vector2d.ZERO),
        Waypoint(bridgeExit, direction = up, heading = up, secondDeriv = Vector2d.ZERO)
    )

    override suspend fun additionalRun() = coroutineScope {
        extension.targetAngle = 0.0
        rotater.targetAngle = 0.0
        waitForStart()
        val skystoneLocation = Vector2d(-2 * tile - block - 3 * `in`, tile - 3 * `in`)
        launch {
            delay(500)
            intake.power = 1.0
        }
        move(
            BotSide.Intake,
            Waypoint(startLoading),
            Waypoint(skystoneLocation, direction = grabAngle, heading = grabAngle)
        )
        val grabPlace = Vector2d(1.7 * tile, 0.8 * tile)
        move(
            BotSide.Output,
            Waypoint(skystoneLocation, heading = grabAngle + PI),
            *bridgePositions,
            Waypoint(grabPlace, heading = right)
        ).join()
        intake.power = 0.0
        hardware.claw!!.close()
        hardware.grabbers!!.forEach { it.close() }
        val releaseJob = launch {
            delay(800)
            extension.targetAngle = OutputMechanism.extendMaxAngle
            delay(600)
            hardware.claw!!.open()
            delay(300)
            liftTarget.liftHeight += 5 * `in`
            delay(600)
            extension.targetAngle = 0.0
            delay(600)
            liftTarget.liftHeight = 0.0
        }
        delay(1500)
        move(
            BotSide.Intake,
            Waypoint(grabPlace, heading = left),
            Waypoint(grabPlace + Vector2d(-.3 * tile, 0.0), direction = left, heading = left),
            Waypoint(1.6 * tile, 2.3 * tile, heading = down, direction = down)
        ).join()
        delay(200)
        hardware.grabbers!!.forEach { it.open() }
        delay(400)
        releaseJob.join()
        move(
            BotSide.Intake,
            previousEnd,
            Waypoint(
                1.5 * tile,
                1.0 * tile,
                heading = down,
                direction = down,
                derivMagnitude = 1.0,
                secondDeriv = Vector2d.ZERO
            ),
            Waypoint(-0.2 * tile, 1.0 * tile, heading = down, direction = down)
        ).join()
        extension.targetAngle = 0.5
    }

    private fun updateGrabNode(skyStoneLocation: Vector2d) = graph.run {
        graph.getNodeOrNull("grab")?.let { graph.removeNode(it) }
        graph.getNodeOrNull("grab i")?.let { graph.removeNode(it) }
        val location = skyStoneLocation + grabOffset
        addNode(location)() {
            val it = this
            it.name = "grab"
            addConstraint(WaypointConstraint(direction = grabAngle, heading = grabAngle))
            it.splineTo("bridge enter")
            "align"().splineTo(location + preGrabOffset)() {
                name = "grab i"
                addConstraint(WaypointConstraint(direction = grabAngle))
            }.splineTo(it)
        }
    }


    @UseExperimental(ExperimentalCoroutinesApi::class)
    private fun getSkystoneLocation(): Vector2d {
//        val channel = skyStoneDetector.startLooking(this)
        val number = 1/*channel.consume {
            withTimeoutOrNull(5000) {
                loop@ while (isActive) {
                    val skystones = receive()
                    skystones.forEach {
                        telemetry.addLine(it.toString())
                    }
                    telemetry.update()
//                    if (skystones.size != 1) continue
                    val x = skystones.first().x
                    return@withTimeoutOrNull when {
                        x <= 40 -> continue@loop
                        x in 40.0..180.0 -> 0
                        x in 180.0..318.0 -> 1
                        x in 318.0..461.0 -> 2
                        else -> continue@loop
                    }.also {
                        telemetry.addLine("Detected at $it")
                        telemetry.update()
                    }
                }
                telemetry.addLine("Cancelled")
                telemetry.update()
                1
            } ?: kotlin.run {
                telemetry.addLine("Gave up")
                telemetry.update()
                1
            }
        }.replaceIf(fieldSide.mirrored) { 2 - it }*/
        return Vector2d(-2 * tile - number * block, tile)
    }
}

//todo add this officially
private fun Waypoint.reverseDirection(): Waypoint {
    return Waypoint(position, constraint.reverseDirection())
}

private val preGrabOffset = Vector2d(8 * `in`, 8 * `in`)
private val grabOffset =/* Vector2d.polar(robot / 3, grabAngle) -*/ Vector2d(0 * `in`, -7 * `in`)


private fun Waypoint(pose: Pose2d): Waypoint {
    return Waypoint(pose.vec, heading = pose.heading)
}
