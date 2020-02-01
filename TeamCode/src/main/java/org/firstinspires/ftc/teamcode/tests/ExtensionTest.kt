package org.firstinspires.ftc.teamcode.tests

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.firstinspires.ftc.teamcode.system.Extension
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode

@TeleOp(name = "Extension Test", group = "Test")
@Disabled
class ExtensionTest : BotSystemsOpMode() {

    override fun getElements(): Array<Element> = arrayOf(additional)

    private val additional = object : LoopElement() {
        private val extension: Extension by dependency()

        val controlLoop by loopOn<ControlLoop>()

        override fun loop() {
            val power = -gamepad1.left_stick_y
            val signal = power * controlLoop.elapsedSeconds / 2
            /* * Output.maxExtendSpeed*/
            extension.targetAngle += signal
            telemetry.run {
                addLine("Signal  : $power")
                addLine("Target A: ${extension.targetAngle}")
                addLine("Meas   A: ${extension.currentAngle}")
            }
        }
    }
}
