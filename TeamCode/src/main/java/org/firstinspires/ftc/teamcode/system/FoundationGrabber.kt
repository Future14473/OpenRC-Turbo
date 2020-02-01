package org.firstinspires.ftc.teamcode.system

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode

class FoundationGrabber : LoopElement() {
    init {
        loopOn<ControlLoop>()
    }

    private val buttons by dependency(ButtonsElement::class) { buttons1 }
    private val grabbers by dependency(Hardware::class) {
        grabbers ?: error("Foundation grabber servos not found!! (check config)")
    }
    private val grabSignal get() = buttons.b.isClicked
    private val releaseSignal get() = buttons.a.isClicked
    override fun loop() {
        if (grabSignal) {
            grabbers.forEach {
                it.close()
            }
        } else if (releaseSignal) {
            grabbers.forEach {
                it.open()
            }
        }
    }
}

@TeleOp(group = "Test")
@Disabled
class GrabberOnly : BotSystemsOpMode() {

    override fun getElements(): Array<out Element> {
        return arrayOf(FoundationGrabber())
    }
}
