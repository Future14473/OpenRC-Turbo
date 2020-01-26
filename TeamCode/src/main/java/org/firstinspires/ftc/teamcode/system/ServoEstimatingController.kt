package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.moveTowards
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.jargon.hardware.Servo
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.math.epsEq

abstract class ServoEstimatingController(
    private val speed: Double,
    private val angleRange: ClosedFloatingPointRange<Double>,
    initialTargetAngle: Double
) : LoopElement() {

    private val controlLoop by loopOn<ControlLoop>()

    protected abstract val servos: List<Servo>

    var currentAngle = 0.0
        private set
    private var inited = false

    var targetAngle = initialTargetAngle //larger angle is less extension
        set(inputAngle) {
            val angle = inputAngle.coerceIn(angleRange)
            if (inited && angle epsEq field) return
            inited = true
            servos.forEach {
                //FIXME: rename from "position" to "angle"
                it.position = angle
            }
            field = angle
        }
    val isAtTarget: Boolean get() = currentAngle == targetAngle

    override fun loop() {
        currentAngle =
            currentAngle.moveTowards(targetAngle, speed * controlLoop.elapsedSeconds)
    }
}


class Rotater : ServoEstimatingController(270 * deg / s, 0 * deg..180 * deg, 0.0) {
    override val servos: List<Servo> by dependency(Hardware::class) {
        listOf(rotater ?: error("Rotater servo not found!! (check config)"))
    }
}

class Extension : ServoEstimatingController(1.0, 0.0..1.0, 0.0) {

    override val servos by dependency(Hardware::class) {
        extensionServos ?: error("Linkage servos not found (check config)")
    }
}
