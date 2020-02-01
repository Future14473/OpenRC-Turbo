package org.firstinspires.ftc.teamcode.tests

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.system.LiftController
import org.firstinspires.ftc.teamcode.system.OutputMechanism
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode

@TeleOp(name = "Output Test", group = "Test")
@Disabled
class OutputTest : BotSystemsOpMode() {

    override fun getElements(): Array<Element> = arrayOf(
        OutputMechanism(true),
        LiftController()
    )
}
