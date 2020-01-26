package org.firstinspires.ftc.teamcode

import org.futurerobotics.jargon.math.distTo
import kotlin.math.sign


/**
 * Moves [this] double value towards the [target] value, with a given [maxDelta],
 * without overshoot.
 *
 * Or, gets the closest value to [target] that is at most [maxDelta] away from [this].
 */
fun Double.moveTowards(target: Double, maxDelta: Double): Double = when {
    target distTo this <= maxDelta -> target
    else -> this + maxDelta * sign(target - this)
}
