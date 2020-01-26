package org.firstinspires.ftc.teamcode.system.drive

import org.futurerobotics.jargon.pathing.Path
import org.futurerobotics.jargon.pathing.backwards
import org.futurerobotics.jargon.pathing.graph.PathGraph

class GraphTracker(private val graph: PathGraph, node: String) {

    var node = node
        private set

    fun pathTo(name: String, botSide: BotSide): Path =
        graph.getPath(node, name).also {
            node = name
        }.let {
            if (botSide.backwards) it.backwards() else it
        }
}
