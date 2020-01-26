package org.firstinspires.ftc.teamcode.system.drive

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.util.RobotLog
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consume
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.firstinspires.ftc.teamcode.system.SkystoneDetector
import org.firstinspires.ftc.teamcode.system.drive.dimensions.block
import org.firstinspires.ftc.teamcode.system.drive.dimensions.grabAngle
import org.firstinspires.ftc.teamcode.system.drive.dimensions.startLoading
import org.firstinspires.ftc.teamcode.system.drive.dimensions.tile
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.pathing.graph.WaypointConstraint
import org.futurerobotics.jargon.util.replaceIf

//right now: loading side
@Autonomous
class AutoBeingTested
@JvmOverloads constructor(
    private val fieldSide: FieldSide = FieldSide.Blue
) : BotSystemsOpMode() {

    private val tracker = GraphTracker(graph, "start loading")
    private val pathFollower = DrivePathFollower(fieldSide, initialPose = startLoading, debug = true)
    private val hardware = Hardware()
    private val skyStoneDetector = SkystoneDetector()
    private val intake = object {
        var power: Double = 0.0
            set(value) {
                hardware.intakeMotors!!.forEach {
                    it.voltage = value * 12
                }
                field = value
            }
    }

    override fun getElements(): Array<out Element> {
        return arrayOf(
            ControlLoop(),
            hardware,
            pathFollower,
            skyStoneDetector,
            Localizer(startLoading.angleRotated(90 * deg), true),
            DrivePositionController(true),
            DriveVelocityController()
        )
    }


    private fun moveTo(name: String, botSide: BotSide): Job {
        RobotLog.d("Moving to %s", name)
        return pathFollower.followPath(tracker.pathTo(name, botSide))
    }

    override suspend fun additionalRun() {

//        moveTo("look 1", BotSide.Intake).join()

//        val skystoneLocation = getSkystoneLocation()
//        updateGrabNode(skystoneLocation)
//        intake.power = 0.8
//
//        moveTo("grab", BotSide.Intake)
//        moveTo("prepare move", BotSide.Output).join()
//
//        intake.power = 0.0
////        autoArm.stateChannel.send(AutoArmState.Grab)
//
////        grabber.close()
////        delay(200)
////        autoArm.stateChannel.send(AutoArmState.Drop)
//        moveTo("move", BotSide.Intake).join()
//
////        grabber.open()
////        delay(200)
//        moveTo("park", BotSide.Intake)
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
    private suspend fun getSkystoneLocation(): Vector2d = coroutineScope {
        val channel = skyStoneDetector.startLooking(this)
        val number = channel.consume {
            withTimeoutOrNull(5000) {
                loop@ while (isActive) {
                    val skystones = channel.receive()
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
        }.replaceIf(fieldSide.mirrored) { 2 - it }
        Vector2d(-2 * tile - number * block, tile)
    }
}

private val preGrabOffset = Vector2d(8 * `in`, 8 * `in`)
private val grabOffset =/* Vector2d.polar(robot / 3, grabAngle) -*/ Vector2d(0 * `in`, -7 * `in`)
