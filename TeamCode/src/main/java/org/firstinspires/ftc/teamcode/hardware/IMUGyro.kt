package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.hardware.bosch.BNO055IMU
import org.futurerobotics.jargon.hardware.Gyro

private val imuParams = BNO055IMU.Parameters().apply {
    angleUnit = BNO055IMU.AngleUnit.RADIANS
}

/**
 * A [Gyro] using an [imu].
 */
class IMUGyro(private val imu: BNO055IMU, initOnStart: Boolean) : Gyro {

    init {
        if (initOnStart)
            initialize()
    }

    fun initialize() {
        imu.initialize(imuParams)
    }

    fun isInitialized() = imu.isGyroCalibrated
    override val angle: Double
        get() = fullOrientation.firstAngle.toDouble()

    val fullOrientation
        get() = imu.angularOrientation
    //            imu.getAngularOrientation(
//            AxesReference.INTRINSIC,
//            AxesOrder.ZYX,
//            AngleUnit.RADIANS
//        )
    override val angularVelocity: Double
        get() = -imu.angularVelocity.zRotationRate.toDouble()
}
