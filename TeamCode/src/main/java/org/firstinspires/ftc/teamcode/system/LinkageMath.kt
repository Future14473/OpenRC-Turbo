package org.firstinspires.ftc.teamcode.system

import org.futurerobotics.jargon.math.convert.*
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

//TODO: find length
val servoDown = 8 * cm
val servoForward = 4 * cm


const val length1 = 8 * cm
const val length2 = 8 * cm


//0  is perfectly to the right, PI is to the left
fun getAngle(distance: Double): Double {
    val y = servoDown
    val x = distance - servoForward
    val l1 = length1
    val l2 = length2
    val distSq = x * x + y * y
    return atan2(y, x) + acos((l1 * l1 + distSq - l2 * l2) / (2 * l1 * sqrt(distSq)))
}
