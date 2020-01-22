package org.firstinspires.ftc.teamcode.system

import kotlinx.coroutines.isActive
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.hardware.SetValue
import org.futurerobotics.botsystem.SyncScope
import org.futurerobotics.botsystem.SyncedElement
import org.futurerobotics.jargon.math.convert.*
import kotlin.coroutines.coroutineContext

//TODO: figure out true values
const val BLOCK_HEIGHT = 4 * `in`
const val MAX_LIFT_HEIGHT = 1.5 * meters

/**
 * Output controller. Controlled by player 2
 */
class Output : SyncedElement(), LiftTarget {

    init {
        loopOn<ControlLoop>()
    }

    internal val buttons by dependency(ButtonsElement::class) { buttons2 }
    internal val claw by dependency(Hardware::class) { claw!! }
    internal val rotater by dependency(Hardware::class) { rotater!! }
    internal val linkage by dependency(Hardware::class) {
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

    @Volatile
    override var liftHeight: Double = 0.0

    @Volatile
    override var liftVelocity: Double = 0.0

    internal lateinit var sync: SyncScope
        private set

    override suspend fun SyncScope.run() {
        sync = this
        var state = OutputState.Ready
        while (coroutineContext.isActive) {
            state = with(state) { runState() }
        }
    }
}

/**
 * State machine for hte output
 */
private enum class OutputState {

    Ready { //all it does is explode.
        override suspend fun Output.runState(): OutputState {
            liftHeight = 0.0
            liftVelocity = 0.0
            claw.open()
            rotater.position = 0.0
            loop {
                if (buttons.left_bumper.isClicked) {
                    claw.close()
                    delay(200)
                    return In
                }
            }
        }
    },
    In { //onlu input. Cannot rotate, can only rotate when extended.
        override suspend fun Output.runState(): OutputState {
            rotater.position = 0.0
            loop {
                if (buttons.dpad_up.isClicked) {
                    liftHeight += BLOCK_HEIGHT
                }
                if(buttons.dpad_down.isClicked){
                    liftHeight -= BLOCK_HEIGHT
                }
                liftHeight = liftHeight.coerceIn(0.0,MAX_LIFT_HEIGHT)

            }
        }
    }
    ;

    /**
     * Runs the [block], looping, synchronizing.
     */
    protected suspend inline fun Output.loop(block: () -> Unit): Nothing {
        while (true) {
            block()
            sync.endLoop()
        }
    }

    /**
     * Delays current time, while still allowing other elements that may be waiting for this to run.
     */
    protected suspend inline fun Output.delay(millis: Int) {
        val start = System.currentTimeMillis()
        val end = start + millis
        loop {
            if (System.currentTimeMillis() > end) return
        }
    }

    abstract suspend fun Output.runState(): OutputState
}
