package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.ROBOT_LOOP_PERIOD
import org.firstinspires.ftc.teamcode.hardware.BulkData
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.hardware.MultipleBulkData
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.LoopManager
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.botsystem.tryGet

class ControlLoop : LoopManager(ROBOT_LOOP_PERIOD) {
    private var telemetry: Telemetry? = null

    override fun init() {
        telemetry = botSystem.tryGet<OpModeElement>()?.opMode?.telemetry
    }

    override fun afterLoop() {
        telemetry?.update()
    }
}

class ButtonsElement : LoopElement() {

    init {
        loopOn<ControlLoop>()
    }

    val buttons1 by dependency(OpModeElement::class) { Buttons(opMode.gamepad1) }
    val buttons2 by dependency(OpModeElement::class) { Buttons(opMode.gamepad2) }

    override fun loop() {
        buttons1.update()
        buttons2.update()
    }
}

/**
 * An element which gets a [value] every loop. And loops on [ControlLoop].
 */
abstract class LoopValueElement<T : Any> : LoopElement() {

    init {
        loopOn<ControlLoop>()
    }

    @Volatile
    lateinit var value: T
        private set

    override fun loop() {
        value = loopValue()
    }

    //cannot name getValue... aww
    protected abstract fun loopValue(): T
}


class TheBulkData : LoopValueElement<BulkData>() {

    private val hubs by dependency(Hardware::class) {
        hubs ?: error(
            """
            |THE EXPANSION HUBS AIN'T HAVE BEEN FOUND 
            |
            |I'm going to regret this error message
            """.trimMargin()
        )
    }

    override fun loopValue(): BulkData = MultipleBulkData(hubs)
}

