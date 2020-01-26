package org.firstinspires.ftc.teamcode.system


import detectors.OpenCvDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.futurerobotics.botsystem.BaseElement
import org.futurerobotics.botsystem.ftc.OpModeElement

class SkystoneDetector : BaseElement() {

    private val detector: OpenCvDetector by onInit { OpenCvDetector(get<OpModeElement>().opMode.hardwareMap.appContext) }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun startLooking(scope: CoroutineScope) = scope.produce {
        detector.begin()
        try {
            while (this.isActive) {
                val skystones = detector.skyStones
                if (skystones.isNotEmpty())
                    send(skystones)
                else
                    delay(20)
            }
        } finally {
            detector.end()
        }
    }
}
