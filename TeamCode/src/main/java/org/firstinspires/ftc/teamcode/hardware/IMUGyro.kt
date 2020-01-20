package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.hardware.bosch.BNO055IMU
import org.futurerobotics.jargon.hardware.Gyro

private val imuParams = BNO055IMU.Parameters().apply {
    mode = BNO055IMU.SensorMode.GYRONLY
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
        get() = -imu.angularOrientation.firstAngle.toDouble()
    override val angularVelocity: Double
        get() = -imu.angularVelocity.zRotationRate.toDouble()
}
