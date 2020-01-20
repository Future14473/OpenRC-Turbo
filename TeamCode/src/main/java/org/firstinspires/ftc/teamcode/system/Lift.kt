package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.ROBOT_LOOP_PERIOD
import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.futurerobotics.botsystem.SyncedElement
import org.futurerobotics.botsystem.LoopValue
import org.futurerobotics.jargon.linalg.*
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.model.MotorModel
import org.futurerobotics.jargon.statespace.*
import kotlin.math.pow

object of {
    inline infix fun <T> the(t: T): T = t
}
//TODO
@Suppress("UNREACHABLE_CODE")
object LiftModel {

    val kAug: Mat
    val matrices: DiscreteStateSpaceMatrices

    init {
        // HEY FUTURE BEN
        // FIXME
        //   MAKE A WAY FOR THINGS TO BE EASIER FOR THINGS LIKE SPOOLS

        val motor: MotorModel = MotorModel.fromMotorData(12.0, 10.0, 12.0, 10.0, 0.1)
        val mass: Double = 3 * kg
        val radius: Double = 1.5 * `in`
        val voltsFromVel = motor.voltsPerAngVel * radius
        val voltsFromAccel = motor.voltsPerTorque * radius * mass / 2
        val accelFromVolts = 1 / voltsFromAccel
        val deccelFromVel = -accelFromVolts * voltsFromVel

        //state = [pos, vel]
        //signal = [volts]
        //signal
        val a = Mat(
            of the
                    0, 1 to
                    0, deccelFromVel
        )
        val b = Mat(
            0 to accelFromVolts
        )
        val c = idenMat(2) * radius

        val q = zeroMat(2, 2).apply {
            this[0, 0] = 1 / (3 * inches).pow(2)
            this[1, 1] = 1 / (4 * inches / seconds).pow(2)
        }
        val r = Mat(1 / (6 * volts).pow(2))

        val k = continuousLQR(a, b, q, r)

        val aAug = concat2x2dynamic(a, b, 0, 0)
        val bAug = concatCol(b, Mat(0))
        //FIXME fix this error message
        val cAug = concatRow(c, zeroMat(2, 1))
        kAug = concatRow(k, Mat(1))

        matrices = ContinuousStateSpaceMatrices(aAug, bAug, cAug).discretize(ROBOT_LOOP_PERIOD)
    }

    val initialCovariance = zeroMat(3, 3).apply {
        this[2, 2] = 4.0
    }
    val noiseCovariance = NoiseCovariance(
        zeroMat(3, 3).apply {
            this[0, 0] = 10 * `in` * ROBOT_LOOP_PERIOD
            this[1, 1] = 10 * `in` * ROBOT_LOOP_PERIOD
            this[2, 2] = 2 * volts * ROBOT_LOOP_PERIOD
        },
        zeroMat(2, 2).apply {
            this[0, 0] = 0.05 //not a lot of radians, measurements are nice.
            this[1, 1] = 0.05
        }
    )


    @UseExperimental(ExperimentalStateSpace::class)
    fun getRunner() = StateSpaceRunnerBuilder().apply {
        setMatrices(matrices)
        addGainController(kAug)
        addStateModifier(object : StateModifier {
            override fun augmentInitialState(x: Vec, prevXAug: Vec?): Vec {
                return x.append(prevXAug?.get(2) ?: 2.0)
            }

            override fun augmentReference(r: Vec): Vec {
                return r.append(0.0)
            }

            override fun deAugmentState(xAug: Vec): Vec {
                return xAug[0..1]
            }
        })

        addKalmanFilter {
            setInitialProcessCovariance(
                initialCovariance
            )

            setNoiseCovariance(
                noiseCovariance
            )
        }
    }.build()
}

interface LiftTarget {
    val pos: LoopValue<Vec>
}

class LiftController : SyncedElement<Unit>() {

    init {
        loopOn<ControlLoop>()
    }

    private val hardware by dependency<Hardware>()
    private val measurements by dependency<Measurements>()
    private val target by dependency<LiftTarget>()
    private val controllers = List(2) { LiftModel.getRunner() }
    override fun init() {
        controllers.forEach {
            it.reset(zeroVec(2))
        }
    }

    @UseExperimental(ExperimentalStateSpace::class)
    override suspend fun loop() {
        val pos = measurements.listPositions.await()
        val vel = measurements.liftVelocities.await()
        val target = target.pos.await()
        controllers.forEachIndexed { i, runner ->
            runner.update(
                createVec(pos[i], vel[i]),
                target,
                null,
                0L
            )
        }
        hardware.liftsMotors!!.forEachIndexed { i, motor ->
            motor.voltage = controllers[i].signal[0]
        }
    }
}
