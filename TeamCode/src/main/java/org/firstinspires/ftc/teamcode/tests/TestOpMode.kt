package org.firstinspires.ftc.teamcode.tests

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.util.RobotLog
import org.futurerobotics.botsystem.BaseElement
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.botsystem.get

@TeleOp
class TestOpMode : BotSystemsOpMode(Something()) {

    override suspend fun additionalRun() {
        botSystem.get<Something>().hey()
    }
}

private class Something : BaseElement() {

    private val opMode by dependency(OpModeElement::class) { opMode }
    private val hardwareMap by lazy { opMode.hardwareMap!! }

    fun hey() {
        hardwareMap.forEach {
            RobotLog.d("%s: %s", hardwareMap.getNamesOf(it).first(), it.deviceName)
        }
    }
}
