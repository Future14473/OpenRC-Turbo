package org.firstinspires.ftc.teamcode.tests

import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.firstinspires.ftc.teamcode.system.LiftController
import org.firstinspires.ftc.teamcode.system.LiftTarget
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.LoopValue
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.jargon.linalg.Vec

class LiftTest : BotSystemsOpMode(), LiftTarget {
    private val loop = ControlLoop()
    override fun getInitialElements(): Collection<Element> {
        return listOf(
            LiftController()
        )
    }

    override lateinit var pos: LoopValue<Vec>

    override suspend fun additionalRun() {
        loop.addElement {

        }
    }
}
