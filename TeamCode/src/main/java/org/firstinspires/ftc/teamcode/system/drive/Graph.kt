package org.firstinspires.ftc.teamcode.system.drive

import org.firstinspires.ftc.teamcode.system.drive.dimensions.*
import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.pathing.graph.LinearInterpolator
import org.futurerobotics.jargon.pathing.graph.PathGraph
import org.futurerobotics.jargon.pathing.graph.WaypointConstraint

val graph = PathGraph().apply {
    defaultInterpolator = LinearInterpolator
    addNode(startLoading).name("start loading")
    //not neutral bridge
    addNode(bridgeEnter).name("bridge enter")
        .lineTo(bridgeExit).name("bridge exit")() {
        reverse.weight = Int.MAX_VALUE
    }

    //neutral bridge
    addNode(neutralEnter).name("neutral enter")
        .lineTo(neutralExit).name("neutral exit")() {
        reverse.weight = Int.MAX_VALUE
    }

    "neutral exit"().splineTo("bridge enter")
    "bridge exit"().splineTo("neutral enter")

    addNode(look1).name("look 1")() {
        "start loading"().splineTo(this)
    }
    "look 1"().splineTo(-3 * tile, 3 * tile)
        .name("align").addInConstraint(WaypointConstraint(heading = right))

    "start loading"().splineTo("bridge enter")

    addNode(2.5 * tile, 0.8 * tile)() {
        name = "prepare move"
        "bridge exit"().splineTo(this).addConstraint(WaypointConstraint(direction = right, derivMagnitude = 1.0))
    }

    addNode(1.5 * tile, 2.0 * tile)() {
        name = "move"
        addConstraint(WaypointConstraint(heading = down))
    }
    "prepare move"().lineTo(0.5 * tile, 2.5 * tile)() {
        //intermediate
        addOutConstraint(WaypointConstraint(heading = 135 * deg))
    }.splineTo("move")

    addNode(2.5 * tile, 2.0 * tile).name("stack")() {
        "bridge exit"().splineTo(this.location + Vector2d(-1 * tile, 0.0)).splineTo(this)
        this.splineTo("neutral enter")
    }

    addNode(0.0, 0.5 * tile)() {
        name = "park"

        addConstraint(WaypointConstraint(heading = down))
        "move"().lineTo(.5 * tile, 1.2 * tile)() {
            addOutConstraint(WaypointConstraint(heading = down, direction = down, derivMagnitude = 1.0))
        }.lineTo(this)
    }
}
