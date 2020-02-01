package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.hardware.bosch.BNO055IMU
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.futurerobotics.botsystem.BaseElement
import org.futurerobotics.botsystem.Property
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.jargon.math.convert.*
import org.openftc.revextensions2.ExpansionHubEx

private const val imuName = "imu"

private val wheelConfigs = run {
    val tpr = 383.6
    arrayOf(
        MotorConfig("FrontLeft", DcMotorSimple.Direction.REVERSE, tpr),
        MotorConfig("FrontRight", DcMotorSimple.Direction.FORWARD, tpr),
        MotorConfig("BackLeft", DcMotorSimple.Direction.REVERSE, tpr),
        MotorConfig("BackRight", DcMotorSimple.Direction.FORWARD, tpr)
    )
}

internal const val liftTpr = 537.6
private val liftConfigs = run {
    arrayOf(
        MotorConfig("LiftLeft", DcMotorSimple.Direction.REVERSE, -liftTpr),
        MotorConfig("LiftRight", DcMotorSimple.Direction.FORWARD, liftTpr)
    )
}
@Suppress("UNREACHABLE_CODE")
private val intakeConfigs = run {
    val tpr = 360.0 * 4
    arrayOf(
        MotorConfig("IntakeLeft", DcMotorSimple.Direction.FORWARD, tpr),
        MotorConfig("IntakeRight", DcMotorSimple.Direction.REVERSE, tpr)
    )
}
//servos
private val extensionConfigs = arrayOf(
    RangedServoConfig("LinkageLeft", 0.59..0.0, 0.0..1.0)
//    , //1.0 == out
//    RangedServoConfig("LinkageRight", 0.31..0.78, 0.0..1.0)
)
private val rotaterConfig = RangedServoConfig("Rotater", 0.83..0.1, 0 * deg..180 * deg)

private val clawConfig = ServoDoorConfig("Claw", 1.0..0.0, true)

private val dropperConfig = ServoDoorConfig("Dropper", 0.715..0.25, false)

private val grabberConfigs = arrayOf(
    ServoDoorConfig("GrabberLeft", 0.71..0.25, true), //one using software,
    ServoDoorConfig("GrabberRight", 0.0..1.0, true) //the other one using limits. Wonderful.
)

private val tapeConfig = CRServoConfig("Tape", DcMotorSimple.Direction.REVERSE)

class Hardware : BaseElement() {

    private inline fun <R> hardwareMap(crossinline getter: HardwareMap.() -> R): Property<R> =
        dependency(OpModeElement::class) { opMode.hardwareMap.getter() }

    private fun <T : Any> Array<out HardwareMapConfig<T>>.getAllOrNull() =
        hardwareMap { tryGetAll(asList()) }

    private fun <T : Any> HardwareMapConfig<T>.getOrNull() =
        hardwareMap { tryGet(this@getOrNull) }

    val hardwareMap: HardwareMap by dependency(OpModeElement::class) { opMode.hardwareMap }

    val wheelMotors by wheelConfigs.getAllOrNull()

    val liftsMotors by liftConfigs.getAllOrNull()

    val intakeMotors by intakeConfigs.getAllOrNull()

    val odometryMotors get() = intakeMotors

    val gyro by hardwareMap { tryGet(BNO055IMU::class.java, imuName)?.let { IMUGyro(it, true) } }

    val hubs: List<ExpansionHubEx>? by hardwareMap { getAll(ExpansionHubEx::class.java).takeIf { it.size == 2 } }

    val extensionServos by extensionConfigs.getAllOrNull()

    val rotater by rotaterConfig.getOrNull()

    val claw by clawConfig.getOrNull()

    val dropper by dropperConfig.getOrNull()

    val grabbers by grabberConfigs.getAllOrNull()

    val tape by tapeConfig.getOrNull()
}
