package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.hardware.getMotorVelocities
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.jargon.control.PIDCoefficients
import org.futurerobotics.jargon.control.PIDControllerArray
import org.futurerobotics.jargon.linalg.*
import org.futurerobotics.jargon.math.MotionState
import org.futurerobotics.jargon.math.Pose2d
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.model.FixedWheelDriveModel
import org.futurerobotics.jargon.model.MotorModel
import org.futurerobotics.jargon.model.TransmissionModel
import org.futurerobotics.jargon.statespace.DriveStateSpaceModels
import kotlin.math.pow

private const val NUM_MOTORS = 4

object DriveModel {

    @Suppress("UNREACHABLE_CODE")
    val model = run {
        val length: Double = TODO()
        val mass: Double = TODO()
        val moi = 1 / 2 * mass * length.pow(2)

        val motor: MotorModel = TODO()
        val transmission =
            TransmissionModel.fromTorqueMultiplier(
                motor,
                2.0,
                100 * oz * `in`,
                torqueMultiplier = 0.9
            )
        FixedWheelDriveModel.mecanum(
            mass, moi, transmission, 2 * `in`, TODO(), TODO()
        )
    }
    val voltPerAccel: Vec
    val voltPerVel: Vec

    val stateSpaceModels = run {
        val stateSpaceModel = DriveStateSpaceModels.motorVelocityController(model)
        voltPerAccel = stateSpaceModel.B.let { mat ->
            genVec(mat.rows) { i -> 1 / mat[i, i] }
        }
        val deccelPerVel = stateSpaceModel.A.let { mat ->
            genVec(mat.rows) { i -> mat[i, i] }
        }
        voltPerVel = genVec(deccelPerVel.size) { i ->
            -voltPerAccel[i] * deccelPerVel[i]
        }
    }
/*

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
*/
}

interface DriveVelocity {
    val targetMotion: MotionState<Pose2d>
}

//This simply uses a PID contorller for each wheel.
class DriveVelocityController : LoopElement() {

    private val controlLoop by loopOn<ControlLoop>()
    private val bulkData: TheBulkData by dependency()
    private val hardware: Hardware by dependency()
    private val driveVelocity: DriveVelocity by dependency()

    private val controller = PIDControllerArray(4, PIDCoefficients(1.0, 0.1, 0.1))
    override fun init() {
        controller.reset()
    }

    override fun loop() {
        val driveModel = DriveModel.model
        val bulkData = bulkData.value
        val wheels = hardware.wheelMotors!!
        val targetMotion = driveVelocity.targetMotion

        val botVel = targetMotion.value.toVec()
        val botAccel = targetMotion.velocity.toVec()
        val wheelVel = driveModel.motorVelFromBotVel * botVel
        val wheelAccel = driveModel.motorAccelFromBotAccel * botAccel

        val measuredWheelVel = bulkData.getMotorVelocities(wheels)
        val rawSignal = controller.update(wheelVel, measuredWheelVel, controlLoop.elapsedNanos)

        val feedForward = DriveModel.voltPerAccel emul wheelAccel + DriveModel.voltPerVel emul wheelVel
        val finalSignal = rawSignal + feedForward

        wheels.forEachIndexed { i, motor ->
            motor.voltage = finalSignal[i]
        }
    }
}
