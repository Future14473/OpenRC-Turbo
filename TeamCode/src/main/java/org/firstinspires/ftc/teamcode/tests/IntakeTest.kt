package org.firstinspires.ftc.teamcode.tests

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.botsystem.ftc.OpModeElement

class IntakeControl : LoopElement() {
    init {
        loopOn<ControlLoop>()
    }

    private val gamepad: Gamepad by dependency(OpModeElement::class) { opMode.gamepad2 }
    private val motors by dependency(Hardware::class) { intakeMotors ?: error("Intake motors not found") }

    override fun loop() {
        val power = gamepad.right_trigger - gamepad.left_trigger
        motors.forEach {
            it.voltage = power * 12.0
        }
    }
}

@TeleOp(name = "Intake test", group = "Test")
@Disabled
class IntakeTest : BotSystemsOpMode() {

    override fun getElements(): Array<Element> = arrayOf(IntakeControl())
}
