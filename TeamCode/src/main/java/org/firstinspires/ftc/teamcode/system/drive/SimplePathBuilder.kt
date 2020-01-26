package org.firstinspires.ftc.teamcode.system.drive

import org.futurerobotics.jargon.pathing.Path
import org.futurerobotics.jargon.pathing.endPoint
import org.futurerobotics.jargon.pathing.graph.*
import org.futurerobotics.jargon.pathing.multiplePath
import org.futurerobotics.jargon.pathing.startPoint

//class SimplePathBuilder(
//    val startPoint: Waypoint
//) {
//
//    private val points = mutableListOf<Waypoint>()
//
//    fun addPoint(waypoint: Waypoint) = builder{
//        points += waypoint
//    }
//
//
//
//}

fun createPath(vararg waypoints: Waypoint, headingInterpolator: HeadingInterpolator = LinearInterpolator): Path {
    val splines = heuristicSplineCurves(waypoints.asList(), CurveGenParams.DEFAULT)
    val paths = splines.mapIndexed { index, curve ->
        val startHeading = waypoints[index].constraint.heading ?: curve.startPoint().tanAngle
        val endHeading = waypoints[index + 1].constraint.heading ?: curve.endPoint().tanAngle
        curve.addHeading(headingInterpolator, startHeading, endHeading)
    }
    return multiplePath(paths)
}
