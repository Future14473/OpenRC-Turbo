package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.futurerobotics.botsystem.LoopElement

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
