package org.firstinspires.ftc.teamcode.tests

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.system.ButtonsElement
import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode

@TeleOp(name = "Dropper Test", group = "Test")
@Disabled
class DropperTest : BotSystemsOpMode() {

    override fun getElements(): Array<out Element> {
        return arrayOf(additional)
    }

    private val additional = object : LoopElement() {
        init {
            loopOn<ControlLoop>()
        }

        private val buttons by dependency(ButtonsElement::class) { buttons1 }
        private val dropper by dependency(Hardware::class) { dropper!! }
        override fun loop() {
            if (buttons.b.isClicked)
                dropper.open()
            if (buttons.a.isClicked)
                dropper.close()
        }
    }
}
