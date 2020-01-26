package org.firstinspires.ftc.teamcode.tests

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.system.FrequencyRecorder
import org.firstinspires.ftc.teamcode.system.drive.DriveVelocityController
import org.firstinspires.ftc.teamcode.system.drive.Localizer
import org.firstinspires.ftc.teamcode.system.drive.ManualDriveVelocitySignal
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.jargon.math.Pose2d

@TeleOp(name = "Manual Drive Test", group = "Test")
class ManualDriveTest : BotSystemsOpMode() {

    override fun getElements(): Array<Element> =
        arrayOf(
            ManualDriveVelocitySignal(true),
            DriveVelocityController(true),
            Localizer(Pose2d.ZERO, false),
            FrequencyRecorder()
        )
}
