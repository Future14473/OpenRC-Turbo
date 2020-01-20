package org.firstinspires.ftc.teamcode.system

import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.hardware.SetValue
import org.futurerobotics.botsystem.BaseElement
import org.futurerobotics.botsystem.LoopManager
import org.futurerobotics.botsystem.LoopValue
import org.futurerobotics.botsystem.SettableLoopValue
import org.futurerobotics.jargon.linalg.Vec
import org.futurerobotics.jargon.linalg.zeroVec
import kotlin.coroutines.coroutineContext


class Output : BaseElement(), LiftTarget {
    internal val controlLoop by dependency<ControlLoop>()
    internal val buttons by dependency(ButtonsElement::class) { gamepad2Buttons }

    internal val claw by dependency(Hardware::class) { claw!! }
    internal val rotater by dependency(Hardware::class) { rotater!! }
    internal val arms by dependency(Hardware::class) {
        val linkageServos = linkageServos!!
        object : SetValue {
            override var value: Double = 0.0
                set(value) {
                    linkageServos.forEach {
                        it.position = value
                    }
                    field = value
                }
        }
    }

    internal lateinit var _pos: SettableLoopValue<Vec>

    override val pos: LoopValue<Vec> get() = _pos

    override fun start() {
        _pos = controlLoop.newSettableValue()
        botSystem.scope.launch {
            doOutput()
        }
    }

    private suspend fun doOutput() {
        var state = OutputState.Ready
        while (coroutineContext.isActive) {
            state = with(state) { run() }
        }
    }
}

enum class OutputState {
    Ready {
        override suspend fun Output.run(): OutputState {
            val zero = zeroVec(2)
            _pos.setValue(zero)
            controlLoop.loopAndWait {
                _pos.setValue(zero)
                val buttons = buttons.await()
                if (buttons.left_bumper.isDownClicked) {
                    claw.close()
                    LoopManager.stopSelf()
                }
            }
            return In
        }
    },
    In {
        override suspend fun Output.run(): OutputState {
            TODO("not implemented")
        }
    }


    ;

    fun exit() {
        LoopManager.stopSelf()
    }

    abstract suspend fun Output.run(): OutputState
}
