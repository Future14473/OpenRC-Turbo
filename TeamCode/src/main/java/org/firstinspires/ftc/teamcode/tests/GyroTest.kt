package org.firstinspires.ftc.teamcode.tests

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode

@TeleOp(name = "Gyro Test", group = "Test")
@Disabled
class GyroTest : BotSystemsOpMode() {

    override fun getElements(): Array<Element> = arrayOf(additional)

    private val additional = object : LoopElement() {
        init {
            loopOn<ControlLoop>()
        }

        private val hardware: Hardware by dependency()
        override fun loop() {
            telemetry.addLine("Angle: ${hardware.gyro!!.fullOrientation}")
        }
    }
}
