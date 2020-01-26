package org.firstinspires.ftc.teamcode.system

import org.futurerobotics.jargon.math.angleNorm

/**
 * Manages gyro offset (so that 0 deg points where-ever you want it to)
 */
class OffsetGyro
@JvmOverloads constructor(initialOffset: Double = 0.0) {

    var offset: Double = initialOffset
        private set
    var measuredAngle: Double = 0.0
        private set
    val angle get() = angleNorm(measuredAngle + offset)
    /**
     * Update
     */
    fun update(measuredAngle: Double) {
        require(measuredAngle.isFinite())
        this.measuredAngle = angleNorm(measuredAngle)
    }

    fun resetOffset(angleShouldBe: Double, measuredAngle: Double = this.measuredAngle) {
        require(angleShouldBe.isFinite())
        update(measuredAngle)
        offset = angleNorm(angleShouldBe - measuredAngle)
    }
}
