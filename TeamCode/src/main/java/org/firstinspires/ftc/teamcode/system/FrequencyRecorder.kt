package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.robotcore.external.Telemetry
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.OpModeElement

class FrequencyRecorder : LoopElement() {

    companion object {
        const val updatePeriod = 1000 //millis
    }

    init {
        loopOn<ControlLoop>()
    }

    var cyclesPerSecond: Double = 0.0
        private set
    private var cycleStartTime = Long.MIN_VALUE
    private var cycle = 0L
    private val telemetry: Telemetry by dependency(OpModeElement::class) { opMode.telemetry }

    override fun loop() {
        cycle++
        val curTime = System.currentTimeMillis()
        if (cycleStartTime == Long.MIN_VALUE) {
            cycleStartTime = curTime
            return
        }
        var numPeriods = 0
        while ((curTime - cycleStartTime) > updatePeriod) {
            cycleStartTime += updatePeriod
            numPeriods++
        }
        if (numPeriods > 0) {
            cyclesPerSecond = (cycle / numPeriods).toDouble()
            cycle = 0
        }
        telemetry.addLine("CPS: $cyclesPerSecond")
    }
}
