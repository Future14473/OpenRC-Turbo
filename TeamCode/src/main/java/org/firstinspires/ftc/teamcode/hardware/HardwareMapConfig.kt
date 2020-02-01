package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.robotcore.hardware.*
import org.futurerobotics.jargon.hardware.Servo as Servo1

/**
 * Something which can be extracted from a [HardwareMap].
 */
interface HardwareMapConfig<T> {

    fun tryGetFrom(map: HardwareMap): T?
}

data class SimpleMotorConfig(
    val name: String,
    val direction: DcMotorSimple.Direction
) : HardwareMapConfig<DcMotorEx> {

    override fun tryGetFrom(map: HardwareMap): DcMotorEx? =
        map.tryGet(DcMotorEx::class.java, name)
            ?.also { it.direction = direction }
}

data class MotorConfig(
    val name: String,
    val direction: DcMotorSimple.Direction,
    val ticksPerRev: Double
) : HardwareMapConfig<FtcMotor> {

    override fun tryGetFrom(map: HardwareMap): FtcMotor? {
        val motor = map.tryGet(DcMotorEx::class.java, name) ?: return null
        motor.direction = direction
        return FtcMotor(motor, ticksPerRev)
    }
}

data class RangedServoConfig(
    val name: String,
    val servoRange: ClosedFloatingPointRange<Double>,
    val angleRange: ClosedFloatingPointRange<Double>
) : HardwareMapConfig<Servo1> {

    override fun tryGetFrom(map: HardwareMap): Servo1? {
        val servo = map.tryGet(ServoImplEx::class.java, name) ?: return null
        return FtcServo(servo, servoRange, angleRange)
    }
}


data class ServoDoorConfig(
    val name: String,
    val closeOpenRange: ClosedFloatingPointRange<Double>,
    val initialIsOpen: Boolean?
) : HardwareMapConfig<ServoDoor> {

    override fun tryGetFrom(map: HardwareMap): ServoDoor? {
        val servo = map.tryGet(ServoImplEx::class.java, name) ?: return null
        return ServoDoor(servo, closeOpenRange, initialIsOpen)
    }
}


fun <T> HardwareMap.tryGet(config: HardwareMapConfig<T>) = config.tryGetFrom(this)
fun <T> HardwareMap.tryGetAll(list: List<HardwareMapConfig<T>>): List<T>? {
    return list.map {
        tryGet(it) ?: return null
    }
}

data class CRServoConfig(
    val name: String,
    val direction: DcMotorSimple.Direction
) : HardwareMapConfig<CRServo> {

    override fun tryGetFrom(map: HardwareMap): CRServo? {
        return map.tryGet(CRServo::class.java, name)?.also {
            it.direction = direction
        }
    }
}
