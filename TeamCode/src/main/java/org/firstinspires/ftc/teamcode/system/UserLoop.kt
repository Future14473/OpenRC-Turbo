package org.firstinspires.ftc.teamcode.system

import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.teamcode.USER_LOOP_PERIOD
import org.futurerobotics.botsystem.LoopManager
import org.futurerobotics.botsystem.LoopValue
import org.futurerobotics.botsystem.ftc.OpModeElement

class UserLoop : LoopManager(USER_LOOP_PERIOD) {
    private val opMode by dependency(OpModeElement::class) { opMode }

    val gamepad1Buttons by onInit {
        addButtonsLoop(opMode.gamepad1)
    }
    val gamepad2Buttons by onInit {
        addButtonsLoop(opMode.gamepad2)
    }

    private fun addButtonsLoop(gamepad: Gamepad): LoopValue<Buttons> {
        val buttons = Buttons(gamepad)
        return addLoop {
            buttons.updateNow()
            buttons
        }
    }
}
