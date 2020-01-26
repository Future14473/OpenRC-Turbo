package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.hardware.Hardware

class GyroAngle : LoopValueElement<Double>() {
    private val gyro by dependency(Hardware::class) { gyro ?: error("IMU not found! (check config)") }

    override fun loopValue(): Double = gyro.angle
    fun getValueNow(): Double = gyro.angle
}
