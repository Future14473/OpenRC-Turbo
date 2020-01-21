package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.hardware.lynx.LynxController
import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.robotcore.external.navigation.Rotation
import org.futurerobotics.jargon.math.TAU
import org.openftc.revextensions2.RevBulkData

/**
 * Interface to represent bulk data.
 */
interface BulkData {

    /** Gets the [motor]'s current position in encoder ticks. */
    fun getMotorCurrentPosition(motor: DcMotor): Int

    /** Gets the [motor]'s current velocity in encoder ticks per second. */
    fun getMotorVelocity(motor: DcMotor): Int

    /** If the motor is at target position. */
    fun isMotorAtTargetPosition(motor: DcMotor): Boolean
}

fun BulkData.getMotorCurrentAngle(motor: FtcMotor): Double {
    return getMotorCurrentPosition(motor.motor) / motor.ticksPerRev * TAU
}
fun BulkData.getMotorAngularVelocity(motor: FtcMotor): Double {
    return getMotorVelocity(motor.motor) / motor.ticksPerRev * TAU
}

/**
 * A [BulkData] that delegates to a [RevBulkData].
 */
class RevBulkDataDelegate(private val data: RevBulkData) : BulkData {

    override fun getMotorCurrentPosition(motor: DcMotor): Int = data.getMotorCurrentPosition(motor)
    override fun getMotorVelocity(motor: DcMotor): Int = data.getMotorCurrentPosition(motor)
    override fun isMotorAtTargetPosition(motor: DcMotor): Boolean = data.isMotorAtTargetPosition(motor)
}

/**
 * [RevBulkData] based off of multiple [RevBulkData]s.
 */
class MultipleBulkData(data: List<RevBulkData>) : BulkData {

    private val allData = data.associateBy { revDataModuleField[it] as LynxModule }
    override fun getMotorCurrentPosition(motor: DcMotor): Int =
        getCorrespondingBulkData(motor)
            .getMotorCurrentPosition(motor.portNumber)
            .correctSign(motor)

    override fun getMotorVelocity(motor: DcMotor): Int =
        getCorrespondingBulkData(motor)
            .getMotorVelocity(motor.portNumber)
            .correctSign(motor)

    override fun isMotorAtTargetPosition(motor: DcMotor): Boolean =
        getCorrespondingBulkData(motor)
            .isMotorAtTargetPosition(motor.portNumber)

    private fun getCorrespondingBulkData(motor: DcMotor): RevBulkData =
        allData[lynxControllerModuleField[motor.controller] as LynxModule]
            ?: throw IllegalArgumentException("Given motor is not in any of the bulk data given.")

    private val DcMotor.operationalDirection: DcMotorSimple.Direction
        get() = direction.let {
            if (motorType.orientation === Rotation.CCW) it.inverted() else it
        }

    private fun Int.correctSign(motor: DcMotor): Int =
        if (motor.operationalDirection === DcMotorSimple.Direction.REVERSE) -this else this

    companion object {
        private val revDataModuleField =
            RevBulkData::class.java.getDeclaredField("module").apply { isAccessible = true }
        private val lynxControllerModuleField =
            LynxController::class.java.getDeclaredField("module").apply { isAccessible = true }
    }
}
