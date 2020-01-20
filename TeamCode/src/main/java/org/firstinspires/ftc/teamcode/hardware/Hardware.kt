package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.hardware.bosch.BNO055IMU
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.futurerobotics.botsystem.BaseElement
import org.futurerobotics.botsystem.Property
import org.futurerobotics.botsystem.ftc.OpModeElement
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

private val liftConfigs = run {
    val tpr = 537.6
    arrayOf(
        MotorConfig("LiftLeft", DcMotorSimple.Direction.REVERSE, -tpr),
        MotorConfig("LiftRight", DcMotorSimple.Direction.FORWARD, tpr)
    )
}
private val intakeConfigs = arrayOf(
    SimpleMotorConfig("IntakeLeft", DcMotorSimple.Direction.FORWARD),
    SimpleMotorConfig("IntakeRight", DcMotorSimple.Direction.REVERSE)
)

//TODO
private val linkageConfigs = arrayOf(
    RangedServoConfig("LinkageLeft", 0.0..1.0, 0.0..1.0),
    RangedServoConfig("LinkageRight", 0.0..1.0, 0.0..1.0)
)
//TODO
val rotaterConfig = RangedServoConfig("Rotater", 0.0..1.0, 0.0..1.0)

//TODO
val clawConfig = ServoDoorConfig("Claw", 0.0..1.0, true)

//TODO
val dropperConfig = ServoDoorConfig("Dropper", 0.0..1.0, false)

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

    val gyro by hardwareMap { tryGet(BNO055IMU::class.java, imuName)?.let { IMUGyro(it, true) } }

    val hubs: List<ExpansionHubEx>? by hardwareMap { getAll(ExpansionHubEx::class.java).takeIf { it.size == 2 } }

    val linkageServos by linkageConfigs.getAllOrNull()

    val rotater by rotaterConfig.getOrNull()

    val claw by clawConfig.getOrNull()

    val dropper by dropperConfig.getOrNull()
}
