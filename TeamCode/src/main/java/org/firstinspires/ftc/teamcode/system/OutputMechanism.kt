package org.firstinspires.ftc.teamcode.system

import kotlinx.coroutines.isActive
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.blockHeight
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.delayGrabMillis
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.delayReleaseMillis
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.dropperReleaseMillis
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendMaxAngle
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendMaxSpeed
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendMinCanEmptyRotate
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendMinCanRotate
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendReady
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftBeforeExtendTolerance
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftMaxHeight
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftMaxSpeed
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftMinHeight
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftReadyTolerance
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftUpBeforeRetract
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.retractBeforeLower
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.rotationMax
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.rotationMaxSpeed
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.rotationMin
import org.firstinspires.ftc.teamcode.tests.IntakeControl
import org.futurerobotics.botsystem.SyncScope
import org.futurerobotics.botsystem.SyncedElement
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.math.distTo
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

/**
 * Output controller. Controlled by gamepad 2
 */
class OutputMechanism
@JvmOverloads constructor(
    internal val debug: Boolean = false
) : SyncedElement(), LiftTarget {

    companion object {
        const val blockHeight = 4 * `in`

        const val liftMaxHeight = 0.70 * meters //not decreasing by 1 cm
        const val liftMaxSpeed = 0.35 * m / s
        const val liftMinHeight = 1 * `in`
        const val liftReadyTolerance = 2 * `in` //as in all the way down
        const val liftBeforeExtendTolerance = 3 * `in`

        const val extendMaxAngle = 1.0
        const val extendMaxSpeed = 0.8
        const val extendReady = .4

        const val extendMinCanRotate = .76 //with block
        const val extendMinCanEmptyRotate = .6 //without block
        //rotation
        const val rotationMaxSpeed = 60 * deg / s
        const val rotationMin = 60 * deg
        const val rotationMax = 260 * deg

        //release sequence
        const val liftUpBeforeRetract = 4 * `in`
        const val retractBeforeLower = .25
        //waiting for claw
        const val delayGrabMillis = 250
        const val delayReleaseMillis = 100
        const val dropperReleaseMillis = 250
    }

    // --- Signals ---
    internal val grabSignal get() = buttons.right_bumper.isClicked
    internal val releaseSignal get() = buttons.left_bumper.isClicked
    internal val manualReleaseSignal get() = buttons.back.isClicked

    internal val extensionSignal get() = gamepad.right_stick_x
    internal val liftSignal get() = -gamepad.left_stick_y
    internal val rotationSignal get() = -gamepad.left_stick_x

    internal val blockUpSignal get() = buttons.dpad_up.isClicked
    internal val blockDownSignal get() = buttons.dpad_down.isClicked

    internal val to90Signal get() = buttons.a.isClicked
    internal val to180Signal get() = buttons.b.isClicked
    internal val retractSignal get() = buttons.x.isClicked

    internal val weAreAboutToWinTheCompetitionSignal get() = buttons.y.isClicked
    // --- Elements ---
    internal val controlLoop by loopOn<ControlLoop>()

    private val opMode by dependency(OpModeElement::class) { opMode }
    private val gamepad get() = opMode.gamepad2
    private val buttons by dependency(ButtonsElement::class) { buttons2 }
    internal val telemetry get() = opMode.telemetry

    private val hardware: Hardware by dependency()
    internal val lift by onInit { get<LiftController>() } //cannot be dependency else circular dependency.
    internal val extension: Extension by dependency()
    internal val rotater: Rotater by dependency()
    internal val claw get() = hardware.claw!!
    internal val dropper get() = hardware.dropper!!

    init {
        dependsOn<IntakeControl>()
    }

    //values
    @Volatile
    override var liftHeight: Double = 0.0
    @Volatile
    override var liftVelocity: Double = 0.0
    internal var liftHeightDeferred = 0.0
    internal var rotaterTargetDeferred = 0.0
    //running
    internal lateinit var sync: SyncScope
        private set

    internal var state = OutputStateMachine.Ready

    override suspend fun SyncScope.run() {
        sync = this
        while (coroutineContext.isActive) {
            state = with(state) { runState() }
        }
    }
}

/**
 * State machine for the output mechanism
 */
internal enum class OutputStateMachine {

    Ready { //Rest and grab.
        override suspend fun OutputMechanism.runState(): OutputStateMachine {
            //extra precautions
            claw.open()
            rotater.targetAngle = 0.0
            extension.targetAngle = 0.0
            liftHeight = 0.0
            liftVelocity = 0.0
            liftHeightDeferred = liftMinHeight
            rotaterTargetDeferred = 0.0
            waitUntil {
                ((lift.currentHeight <= liftReadyTolerance)
                        and rotater.isAtTarget
                        and extension.isAtTarget)
            }
            var previousClosed = claw.isClosed
            loop {
                if (!previousClosed && claw.isClosed) {
                    delay(delayGrabMillis)
                    return In
                }
                previousClosed = claw.isClosed
            }
        }
    },
    In { //Extension is in, only lift.
        override suspend fun OutputMechanism.runState(): OutputStateMachine {
            rotaterTargetDeferred = 0.0
            rotater.targetAngle = 0.0
            if (!rotater.isAtTarget)
                extension.targetAngle = extendMinCanRotate
            loop {
                if (releaseSignal && extension.targetAngle < extendReady) claw.open()
                controlLift(liftSignal)
                controlRotation()
                if (rotaterTargetDeferred != 0.0
                    && lift.currentHeight distTo liftHeight < liftBeforeExtendTolerance
                ) return Out
                if (rotater.isAtTarget) {
                    extension.targetAngle = min(extension.targetAngle, extendReady)
                    if (extension.currentAngle <= extendReady && weAreAboutToWinTheCompetitionSignal) {
                        return to T posing
                    }
                }
            }
        }

        private val posing = 0
    },
    Out { //Extension is out -- doing the stacking
        override suspend fun OutputMechanism.runState(): OutputStateMachine {
            extension.targetAngle = extendMaxAngle
            loop {
                controlLift(liftSignal)
                controlExtension(extensionSignal, extendMinCanRotate..extendMaxAngle)
                controlRotation()
                if (extension.currentAngle >= extendMinCanRotate && rotaterTargetDeferred != 0.0) {
                    rotater.targetAngle = rotaterTargetDeferred
                }
                if (retractSignal) return In
                if (releaseSignal) return Release
            }
        }
    },
    StackCapstone {
        override suspend fun OutputMechanism.runState(): OutputStateMachine {
            dropper.open()
            dropper.open()
            delay(dropperReleaseMillis)
            loop {
                controlLift(liftSignal)
                if (to90Signal || to180Signal) {
                    extension.targetAngle = extendMaxAngle
                } else if (retractSignal) {
                    extension.targetAngle = extendReady
                }
                if (claw.isOpen && releaseSignal) return Release
            }
        }
    },
    Release { //release sequence
        override suspend fun OutputMechanism.runState(): OutputStateMachine {
            liftVelocity = 0.0
            claw.open()
            delay(delayReleaseMillis)
            //raise lift
            liftHeight += liftUpBeforeRetract
            waitUntil {
                lift.currentHeight distTo liftHeight < 1 * `in`
            }
            val maxExtensionBeforeLower = extension.targetAngle - retractBeforeLower
            extension.targetAngle = extendMinCanEmptyRotate
            rotater.targetAngle = 0.0
            loop {
                val lowering = extension.currentAngle <= maxExtensionBeforeLower
                if (lowering) {
                    liftHeight = 0.0
                }
                if (rotater.isAtTarget) {
                    extension.targetAngle = 0.0
                    if (lowering) return Ready
                }
            }
        }
    };

    @Suppress("ClassName", "NOTHING_TO_INLINE", "FunctionName", "UNUSED_PARAMETER")
    private object to {

        inline infix fun T(posing: Int) = StackCapstone
    }

    private fun OutputMechanism.alwaysEachLoop() {
        if (grabSignal) claw.close()
        if (manualReleaseSignal) claw.open()
    }

    protected fun OutputMechanism.controlExtension(
        power: Float,
        allowedRange: ClosedFloatingPointRange<Double>
    ) {
        val delta = power * extendMaxSpeed * controlLoop.elapsedSeconds
        extension.targetAngle = (extension.targetAngle + delta).coerceIn(allowedRange)
    }

    protected fun OutputMechanism.controlRotation() {
        if (to90Signal) rotaterTargetDeferred = 90 * deg
        if (to180Signal) rotaterTargetDeferred = 180 * deg
        if (rotationSignal != 0f) {
            val delta = rotationSignal * rotationMaxSpeed * controlLoop.elapsedSeconds
            rotaterTargetDeferred = (rotaterTargetDeferred + delta).coerceIn(rotationMin..rotationMax)
        }
    }

    /** Lift control logic */
    fun OutputMechanism.controlLift(power: Float) {
        val velocity = power * liftMaxSpeed
        val delta = velocity * controlLoop.elapsedSeconds

        var height = liftHeightDeferred + delta
        if (blockUpSignal) height += blockHeight
        if (blockDownSignal) height -= blockHeight
        liftHeightDeferred = height.coerceIn(liftMinHeight, liftMaxHeight)
        //only attempt to go forward enough when we need to
        if (liftHeightDeferred > liftMinHeight || extension.targetAngle > 0) {
            extension.targetAngle = max(extension.targetAngle, extendReady)
            if (extension.currentAngle >= extendReady) { //only lift up if forward enough
                liftHeight = liftHeightDeferred
                liftVelocity = if (liftHeight >= liftMaxHeight - blockHeight) 0.0
                else velocity
            }
        }
    }

    /** Loops, synced. */
    protected suspend inline fun OutputMechanism.loop(block: () -> Unit): Nothing {
        while (true) {
            sync.endLoop()
            sync.awaitAllDependencies()
            alwaysEachLoop()
            block()
            if (debug) printDebug()
        }
    }

    private fun OutputMechanism.printDebug() {
        telemetry.apply {
            addLine("Current state: $state")
            addLine("Target lift  : $liftHeight")
            addLine("Target extend: ${extension.targetAngle}")
            addLine("Actual extend: ${extension.currentAngle}")
            addLine("Target rotate: ${rotater.targetAngle}")
            addLine("Actual rotate: ${rotater.currentAngle}")
        }
    }

    /** Waits until the condition is true, loop synced. */
    protected suspend inline fun OutputMechanism.waitUntil(condition: () -> Boolean) {
        loop {
            if (condition()) return
        }
    }

    /** Delays current time, loop synced. */
    protected suspend inline fun OutputMechanism.delay(millis: Int) {
        val start = System.currentTimeMillis()
        val end = start + millis
        waitUntil { System.currentTimeMillis() >= end }
    }


    abstract suspend fun OutputMechanism.runState(): OutputStateMachine
}
