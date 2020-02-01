package org.firstinspires.ftc.teamcode.system

import kotlinx.coroutines.isActive
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.blockHeight
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.delayGrabMillis
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.delayReleaseMillis
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendControlSpeed
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendDefaultOut
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendMaxAngle
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendMaxBeforeLower
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendMinCanEmptyRotate
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendMinCanRotate
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.extendReadyToLift
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftBeforeExtendTolerance
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftCapstoneBeforeRetract
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftControlSpeed
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftMaxHeight
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftMinHeight
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftReadyTolerance
import org.firstinspires.ftc.teamcode.system.OutputMechanism.Companion.liftUpBeforeRetract
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
        //lift
        const val blockHeight = 4.1 * `in`

        const val liftMaxHeight = 0.70 * meters //not decreasing by 1 cm
        const val liftMinHeight = 0.5 * `in`

        const val liftReadyTolerance = 1 * `in` //as in all the way down
        const val liftBeforeExtendTolerance = 2 * `in`

        const val liftControlSpeed = 0.35 * m / s
        //extend
        const val extendMaxAngle = 1.0

        const val extendReadyToLift = 0.4
        const val extendDefaultOut = 0.75

        const val extendMinCanRotate = .75 //with block
        const val extendMinCanEmptyRotate = .55 //without block

        const val extendMaxBeforeLower = 0.2

        const val extendControlSpeed = 0.6
        //release sequence
        const val liftUpBeforeRetract = 5 * `in`
        const val liftCapstoneBeforeRetract = 2 * `in`
        //waiting for claw
        const val delayGrabMillis = 250
        const val delayReleaseMillis = 50
    }

    // --- Signals ---
    internal val grabSignal get() = buttons.right_bumper.isClicked
    internal val releaseSignal get() = buttons.left_bumper.isClicked
    internal val manualReleaseSignal get() = buttons.back.isClicked

    internal val extensionSignal get() = gamepad.right_stick_x
    internal val liftSignal get() = -gamepad.left_stick_y

    internal val blockUpSignal get() = buttons.dpad_up.isClicked
    internal val blockDownSignal get() = buttons.dpad_down.isClicked

    internal val toExtendSignal get() = buttons.b.isClicked
    internal val to9001Signal get() = buttons.a.isClicked
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
    //running
    internal lateinit var sync: SyncScope
        private set

    internal var armStateDeferred = ArmStateDeferred.In

    internal var state = OutputStateMachine.Ready

    override suspend fun SyncScope.run() {
        sync = this
        while (coroutineContext.isActive) {
            state = with(state) { runState() }
        }
    }
}

internal enum class ArmStateDeferred {
    In, Extended, Over9000
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
            armStateDeferred = ArmStateDeferred.In
            waitUntil {
                lift.currentHeight <= liftReadyTolerance &&
                        rotater.isAtTarget &&
                        extension.isAtTarget
            }
            var previousClosed = claw.isClosed
            loop {
                if (!previousClosed && claw.isClosed) {
                    delay(delayGrabMillis)
                    return Grab
                }
                previousClosed = claw.isClosed
            }
        }
    },
    Grab { //Grab, but no lift.
        override suspend fun OutputMechanism.runState(): OutputStateMachine {
            loop {
                if (releaseSignal && extension.targetAngle < extendReadyToLift) return Ready
                controlLiftDeferred(liftSignal)
                controlArmDeferred()
                if (liftHeightDeferred > liftMinHeight || armStateDeferred != ArmStateDeferred.In)
                    return In
                if (weAreAboutToWinTheCompetitionSignal) dropper.open()
            }
        }
    },
    In {
        override suspend fun OutputMechanism.runState(): OutputStateMachine {
            rotater.targetAngle = 0.0
            extension.targetAngle = min(extension.targetAngle, extendMinCanRotate)
            loop {
                if (rotater.isAtTarget)
                    extension.targetAngle = extendReadyToLift
                controlLift(liftSignal)
                controlArmDeferred()
                if (lift.currentHeight distTo liftHeight <= liftBeforeExtendTolerance)
                    when {
                        armStateDeferred == ArmStateDeferred.Over9000 && dropper.isClosed -> return Over9000
                        armStateDeferred == ArmStateDeferred.Extended -> return Extended
                    }
                if (weAreAboutToWinTheCompetitionSignal) dropper.open()
            }
        }
    },
    Extended { //Extension is out -- doing the stacking
        override suspend fun OutputMechanism.runState(): OutputStateMachine {
            extension.targetAngle = extendDefaultOut
            rotater.targetAngle = 0.0
            loop {
                controlLift(liftSignal)
                controlExtension(extensionSignal, extendReadyToLift..extendMaxAngle)
                controlArmDeferred()
                when {
                    armStateDeferred == ArmStateDeferred.Over9000 && dropper.isClosed -> return Over9000
                    armStateDeferred == ArmStateDeferred.In -> return In
                }
                if (extension.targetAngle >= extendReadyToLift && releaseSignal) return Release

            }
        }
    },
    Over9000 { //Extension is out -- doing the stacking
        override suspend fun OutputMechanism.runState(): OutputStateMachine {
            extension.targetAngle = extendMaxAngle
            loop {
                controlLift(liftSignal)
                controlExtension(extensionSignal, extendMinCanRotate..extendMaxAngle)
                controlArmDeferred()
                if (extension.currentAngle >= extendMinCanRotate) rotater.targetAngle = 90 * deg
                when (armStateDeferred) {
                    ArmStateDeferred.In -> return In
                    ArmStateDeferred.Extended -> return Extended
                    ArmStateDeferred.Over9000 -> {
                    }
                }
                if (extension.targetAngle >= extendMinCanRotate && releaseSignal) return Release
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
            if (dropper.isOpen)
                liftHeight += liftCapstoneBeforeRetract
            waitUntil {
                lift.currentHeight distTo liftHeight < liftBeforeExtendTolerance
            }
            extension.targetAngle = min(extension.targetAngle, extendMinCanEmptyRotate)
            rotater.targetAngle = 0.0
            loop {
                val lowering = extension.currentAngle <= extendMaxBeforeLower
                if (lowering)
                    liftHeight = 0.0
                if (rotater.isAtTarget) {
                    extension.targetAngle = 0.0
                    if (lowering) return Ready
                }
            }
        }
    };

    private fun OutputMechanism.alwaysEachLoop() {
        if (grabSignal) claw.close()
        if (manualReleaseSignal) claw.open()
    }

    protected fun OutputMechanism.controlExtension(
        power: Float,
        allowedRange: ClosedFloatingPointRange<Double>
    ) {
        val delta = power * extendControlSpeed * controlLoop.elapsedSeconds
        extension.targetAngle = (extension.targetAngle + delta).coerceIn(allowedRange)
    }

    /** Lift control logic */
    fun OutputMechanism.controlLift(power: Float) {
        val velocity = controlLiftDeferred(power)
        //only attempt to go forward enough when we can
        if (liftHeightDeferred > liftMinHeight) {
            extension.targetAngle = max(extension.targetAngle, extendReadyToLift)
            if (extension.currentAngle >= extendReadyToLift) {
                liftHeight = liftHeightDeferred
                liftVelocity =
                    if (liftHeight >= liftMaxHeight - blockHeight) 0.0
                    else velocity
            }
        }
    }

    protected fun OutputMechanism.controlLiftDeferred(power: Float): Double {
        val velocity = power * liftControlSpeed
        val delta = velocity * controlLoop.elapsedSeconds

        var height = liftHeightDeferred + delta
        if (blockUpSignal) height += blockHeight
        if (blockDownSignal) height -= blockHeight
        liftHeightDeferred = height.coerceIn(liftMinHeight, liftMaxHeight)
        return velocity
    }


    protected fun OutputMechanism.controlArmDeferred() {
        armStateDeferred = when {
            retractSignal -> ArmStateDeferred.In
            toExtendSignal -> ArmStateDeferred.Extended
            to9001Signal -> ArmStateDeferred.Over9000
            else -> return
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
