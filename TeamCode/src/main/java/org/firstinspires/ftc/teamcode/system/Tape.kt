package org.firstinspires.ftc.teamcode.system

import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.OpModeElement

class Tape : LoopElement() {
    companion object {
        const val maxPower = 1
    }

    init {
        loopOn<ControlLoop>()
    }

    private val gamepad: Gamepad by dependency(OpModeElement::class) { opMode.gamepad1 }
    private val servo by dependency(Hardware::class) { tape ?: error("Tape servo not found (check config)") }

    private val Boolean.f get() = if (this) 1.0 else 0.0

    override fun loop() {
        val power = (gamepad.y.f - gamepad.x.f) * maxPower
        servo.power = power
    }
}
