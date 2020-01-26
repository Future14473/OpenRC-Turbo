package org.firstinspires.ftc.teamcode.real

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.system.FoundationGrabber
import org.firstinspires.ftc.teamcode.system.LiftController
import org.firstinspires.ftc.teamcode.system.OutputMechanism
import org.firstinspires.ftc.teamcode.system.drive.DriveVelocityController
import org.firstinspires.ftc.teamcode.system.drive.ManualDriveVelocitySignal
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode


@TeleOp(name = "THE TELEOP", group = "Awesome group that starts with the letter 'A' so it's first on the list")
class TheTeleOp : BotSystemsOpMode() {

    override fun getElements() = arrayOf(
        OutputMechanism(),
        LiftController(),
        //
        ManualDriveVelocitySignal(),
        DriveVelocityController(),
        //
        FoundationGrabber()
    )
}
