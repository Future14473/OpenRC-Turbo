package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.robotcore.util.RobotLog
import org.futurerobotics.jargon.hardware.Servo
import com.qualcomm.robotcore.hardware.Servo as InnerServo

typealias DoubleRange = ClosedFloatingPointRange<Double>

private val DoubleRange.size get() = endInclusive - start

/**
 * A wrapper around a [Ftc RobotCore Servo][InnerServo] that maps angles to servo positions.
 *
 * A specified [servoRange] must be given that corresponds to an [angleRange].
 */
class FtcServo(
    /** The servo this wraps around. */
    private val servo: InnerServo,
    private val servoRange: DoubleRange,
    private val angleRange: DoubleRange
) : Servo {


    private fun mapToAngle(servoValue: Double): Double =
        (servoValue - servoRange.start) / servoRange.size *
                angleRange.size + angleRange.start

    private fun mapToServo(angleValue: Double): Double =
        (angleValue - angleRange.start) / angleRange.size *
                servoRange.size + servoRange.start

    override var position: Double
        get() = mapToAngle(servo.position)
        set(value) {
            val servoPos = mapToServo(value)
            RobotLog.d("Setting to angle %4.2f, or pos %4.2f", value, servoPos)
            servo.position = servoPos
        }
}
