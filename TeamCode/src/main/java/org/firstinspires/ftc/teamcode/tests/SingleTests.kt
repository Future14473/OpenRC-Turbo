package org.firstinspires.ftc.teamcode.tests

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.ServoImplEx
import kotlinx.coroutines.delay
import org.firstinspires.ftc.teamcode.system.Buttons
import org.futurerobotics.botsystem.ftc.CoroutineOpMode
import kotlin.math.round


@TeleOp(name = "Single Motor Test", group = "Single Test")
class SingleMotorTest : CoroutineOpMode() {


    override suspend fun runOpMode() {
        val allMotors: List<DcMotorEx> =
            hardwareMap.getAll(DcMotorEx::class.java).sortedBy { hardwareMap.getNamesOf(it).first() }
        val buttons = Buttons(gamepad1)
        var index = 0
        waitForStart()
        var motor = allMotors[index]
        motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        motor.power = 0.0
        fun resetMotor() {
            motor.power = 0.0
            motor = allMotors[index]
            motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
            motor.power = 0.0
        }
        while (opModeIsActive()) {
            delay(100)
            buttons.update()
            when {
                buttons.a.isClicked -> {
                    index = (index + 1) % allMotors.size
                    resetMotor()
                }
                buttons.b.isClicked -> {
                    index = (index + allMotors.size - 1) % allMotors.size
                    motor.power = 0.0
                    resetMotor()
                }
                buttons.x.isClicked -> {
                    motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
                    motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
                }
            }
            val power = -gamepad1.left_stick_y.toDouble()
            motor.power = power
            telemetry.addLine("Cycle: A/B, Power: Left Stick Y, Reset position: X")
            telemetry.addLine("name:     ${hardwareMap.getNamesOf(motor).joinToString()}")
            telemetry.addLine("position: ${motor.currentPosition}")
            telemetry.addLine("power:    $power")
            telemetry.update()
        }
    }
}

const val resolution = 200

@TeleOp(name = "Single Servo Test", group = "Single Test")
class SingleServoTest : CoroutineOpMode() {

    override suspend fun runOpMode() {
        val allServos: List<ServoImplEx> =
            hardwareMap.getAll(ServoImplEx::class.java).sortedBy { hardwareMap.getNamesOf(it).first() }
        val buttons = Buttons(gamepad1)
        var index = 0
        waitForStart()
        var servo = allServos[index]
        var position = servo.position
        fun resetServo() {
            servo.setPwmDisable()
            servo = allServos[index]
            position = servo.position
            servo.setPwmEnable()
        }
        while (opModeIsActive()) {
            delay(100)
            buttons.update()
            when {
                buttons.a.isClicked -> {
                    index = (index + 1) % allServos.size
                    resetServo()
                }
                buttons.b.isClicked -> {
                    index = (index + allServos.size - 1) % allServos.size
                    resetServo()
                }
            }
            if (buttons.dpad_up.isClicked) position += 1.4 / resolution
            if (buttons.dpad_down.isClicked) position -= 1.4 / resolution
            val power = -gamepad1.left_stick_y.toDouble() / 20
            position = (round((position + power) * resolution) / resolution).coerceIn(0.0..1.0)
            servo.position = position
            telemetry.addLine("Cycle: A/B, Move: left stick y, increment: Dpad up/down")
            telemetry.addLine("name:     ${hardwareMap.getNamesOf(servo).joinToString()}")
            telemetry.addLine("position: ${position}")
            telemetry.update()
        }
    }
}
