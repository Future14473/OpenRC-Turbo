package org.firstinspires.ftc.teamcode.tests

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.system.ControlLoop
import org.firstinspires.ftc.teamcode.system.LiftController
import org.firstinspires.ftc.teamcode.system.LiftTarget
import org.futurerobotics.botsystem.CoroutineLoopElement
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.ftc.BotSystemsOpMode
import org.futurerobotics.jargon.math.convert.*

@TeleOp(name = "Lift Test")
class LiftTest : BotSystemsOpMode() {

    override fun getElements(): Collection<Element> = listOf(
        liftTarget,
        LiftController()
    )


    private val liftTarget = object : CoroutineLoopElement(), LiftTarget {
        private val controlLoop by loopOn<ControlLoop>()
        @Volatile
        override var liftHeight: Double = 0.0
        @Volatile
        override var liftVelocity: Double = 0.0

        override suspend fun loopSuspend() {
            val targetVelocity = -gamepad1.left_stick_y * (5 * `in` / s)
            liftHeight += targetVelocity * controlLoop.elapsedNanos
            liftVelocity = targetVelocity
            telemetry.addLine("Target height: $liftHeight")
            telemetry.addLine("target velocity: $targetVelocity")
        }
    }
}
