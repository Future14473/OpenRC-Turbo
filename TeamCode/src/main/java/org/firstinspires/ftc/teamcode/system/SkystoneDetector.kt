package org.firstinspires.ftc.teamcode.system


import detectors.FoundationPipeline.Skystone
import detectors.OpenCvDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.futurerobotics.botsystem.BaseElement

class SkystoneDetector : BaseElement() {

    private val detector: OpenCvDetector = OpenCvDetector()

    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun startLooking(scope: CoroutineScope) = scope.produce {
        detector.begin()
        try {
            var previousSkystones: Array<Skystone>? = null
            while (this.isActive) {
                val currentSkystones: Array<Skystone> = this@SkystoneDetector.detector.skyStones
                if (previousSkystones !== currentSkystones) {
                    this.send(currentSkystones)
                    previousSkystones = currentSkystones
                }
                delay(20)
            }
        } finally {
            detector.end()
        }
    }
}
