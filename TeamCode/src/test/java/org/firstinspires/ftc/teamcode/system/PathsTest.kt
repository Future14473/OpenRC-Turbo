package org.firstinspires.ftc.teamcode.system

import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.pathing.*
import org.junit.Test

class PathsTest {
    @Test
    fun test(){

        val path = Line(Vector2d.ZERO, Vector2d(1.0, 0.0)).addHeading(LinearlyInterpolatedHeading(0.0, 2.0))
        path.startPoint().pose
        path.endPoint().pose

        val reverse = path.backwards()
        val startPoint = reverse.startPoint()
        startPoint.pose
        reverse.endPoint().pose
    }
}
