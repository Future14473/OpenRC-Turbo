package org.firstinspires.ftc.teamcode.system.drive.dimensions

import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.math.Vector2d
import org.futurerobotics.jargon.math.convert.*
import kotlin.math.PI


const val up = 0.0
const val left = PI / 2
const val right = -PI / 2
const val down = PI
//this is on the BLUE side (y is positive).
const val tile = (23 + 7.0 / 8) * inches
const val tileNub = (7 / 16.0) * inches
const val robot = 18 * `in`

const val block = 7.5 * `in`


val startLoading = Pose2d(x = -(1 * tile + .5 * robot), y = (3 * tile - .5 * robot), heading = right)

val bridgeEnter = Vector2d(-.5 * tile - .5 * robot, 1.4 * tile)
val bridgeExit = Vector2d(1.0 * tile + .5 * robot, 1.4 * tile)

val neutralEnter = Vector2d(tile, .5 * robot)
val neutralExit = Vector2d(tile, .5 * robot)

val look1 = Pose2d(-2.8 * tile, startLoading.y, right)
val grabAngle = -145 * deg
