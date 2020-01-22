package org.firstinspires.ftc.teamcode.system

import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.math.epsEq
import org.junit.Assert
import org.junit.Test
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class LinkageMathKtTest {

    @Test
    fun getAngle() {
        val random = Random(2349782340)
        repeat(30) {
            val angle = random.nextDouble(-45 * deg, 45 * deg)

            Assert.assertTrue(angle epsEq getAngle(getForward(angle)))
        }
    }

    private fun getForward(angle: Double): Double {

        return length1 * cos(angle) + sqrt(length2 * length2 - (length1 * (1 - sin(angle)) - servoDown).pow(2)) + servoForward
    }
}
