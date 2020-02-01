package org.firstinspires.ftc.teamcode.system.drive

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.firstinspires.ftc.teamcode.system.drive.dimensions.tile
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.pathing.Line
import org.futurerobotics.jargon.pathing.TangentHeading
import org.futurerobotics.jargon.pathing.addHeading

abstract class AbstractDriveTest : BotSystemsOpMode() {
    protected val pathFollower = DrivePathFollower(FieldSide.Blue)
    override fun getElements(): Array<out Element> {
        return arrayOf(
            pathFollower,
            Localizer(Pose2d.ZERO, true),
            DrivePositionController(),
            DriveVelocityController()
        )
    }

    abstract override suspend fun additionalRun()
}

@Autonomous(name = "Just drive forward 1.5 floor tiles", group = "eh")
class JustDriveForward : AbstractDriveTest() {

    override suspend fun additionalRun() {
        pathFollower.followPath(
            Line(Vector2d.ZERO, Vector2d(1.5 * tile, 0.0))
                .addHeading(TangentHeading())
        )
    }
}


