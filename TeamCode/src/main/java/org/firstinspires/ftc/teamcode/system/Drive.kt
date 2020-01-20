package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.ROBOT_LOOP_PERIOD
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.futurerobotics.botsystem.Element
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.LoopValue
import org.futurerobotics.jargon.linalg.*
import org.futurerobotics.jargon.math.MotionState
import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.model.FixedWheelDriveModel
import org.futurerobotics.jargon.model.MotorModel
import org.futurerobotics.jargon.model.TransmissionModel
import org.futurerobotics.jargon.statespace.*
import kotlin.math.pow

//TODO: AUGMENTATIONS
private object DriveModel {

    @Suppress("UNREACHABLE_CODE")
    val model = run {
        val length: Double = TODO()
        val mass: Double = TODO()
        val moi = 1 / 2 * mass * length.pow(2)

        val motor: MotorModel = TODO()
        val transmission = TransmissionModel.fromTorqueMultiplier(
            motor,
            2.0,
            100 * oz * `in`,
            torqueMultiplier = 0.9
        )
        FixedWheelDriveModel.mecanum(
            mass, moi, transmission, 2 * `in`, TODO(), TODO()
        )
    }

    val discreteMatrices: DiscreteStateSpaceMatrices
    val K: Mat

    init {

        val contModel = DriveStateSpaceModels.poseVelocityController(model, model).apply {
            val newC = concatCol(C, Vec(0, 0, 1).toRowMatrix())
            ContinuousStateSpaceMatrices(A, B, newC)
        }
        discreteMatrices = contModel.discretize(ROBOT_LOOP_PERIOD)
        val Q = idenMat(4) / (1.5 * radians).pow(2)
        val R = zeroMat(3, 3).apply {
            this[0, 0] = idenMat(2) / (3 * inches).pow(2)
            this[3, 3] = 1 / (0.3 * radians).pow(2)
        }
        val qrCost = QRCost(Q, R)

        K = continuousLQR(contModel, qrCost)
    }

    val Kff = plantInversion(discreteMatrices)

    @UseExperimental(ExperimentalStateSpace::class)
    fun getSSRunner(): Pair<ManualObserver, StateSpaceRunner> {
        val observer = ManualObserver()
        val runner = StateSpaceRunnerBuilder().apply {
            setMatrices(discreteMatrices)
            addReferenceTracking(Kff)
            addGainController(K)
            setObserver(observer)
        }.build()

        return observer to runner
    }
}


// HEY BEN IN THE FUTURE:
// FIXME
//  So i found a problem with the "experimental" state space runner thing.
//  Very hard to mix and match parts.
//  Maybe use that declarative programming thing you were thinking about. BUT
//  LIKE ACTUALLY THINK ABOUT IT so you don't revise it 8 times
class ManualObserver : StateSpaceObserver {

    override var currentState: Vec = zeroVec(3)

    override fun reset(initialState: Vec) {
        //so, I guess, this ain't gonna work
    }

    override fun update(matrices: DiscreteStateSpaceMatrices, u: Vec, y: Vec, timeInNanos: Long): Vec {
        return currentState
    }
}


interface DriveVelocity : Element {
    val velocity: LoopValue<MotionState<Pose2d>>
}

@UseExperimental(ExperimentalStateSpace::class)
class DriveController : LoopElement<Unit>() {

    init {
        loopOn<ControlLoop>()
    }

    private val hardware by dependency<Hardware>()
    private val targetVelocity by dependency<DriveVelocity>()
    private val actualVelocity by dependency<VelocityObserver>()
    private val observer: ManualObserver
    private val runner: StateSpaceRunner

    init {
        val (observer, runner) = DriveModel.getSSRunner()
        this.runner = runner
        this.observer = observer
    }


    override suspend fun loop() {
        observer.currentState = actualVelocity.value.await()
        val state = targetVelocity.velocity.await()
        val period = ROBOT_LOOP_PERIOD
        val r = state.value
        val r1 = state.value + state.velocity * period + state.acceleration * (0.5 * period.pow(2))
        runner.update(
            dummy,
            r.toVec(),
            r1.toVec(),
            0L
        )
        repeat(4) { i ->
            hardware.wheelMotors!![i].voltage = runner.signal[i]
        }
    }

    companion object {
        private val dummy = zeroVec(1)
    }
}
