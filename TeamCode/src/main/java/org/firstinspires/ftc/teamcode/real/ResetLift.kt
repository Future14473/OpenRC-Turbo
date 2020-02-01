package org.firstinspires.ftc.teamcode.real

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.futurerobotics.botsystem.CoroutineElement
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode

@TeleOp
class ResetLift : BotSystemsOpMode() {

    override fun getElements(): Array<out Element> = arrayOf(object : CoroutineElement() {
        private val hardware by dependency<Hardware>()
        override suspend fun runElement() {
            waitForStart()
            hardware.liftsMotors!!.forEach {
                it.resetPosition()
            }
        }
    })
}
