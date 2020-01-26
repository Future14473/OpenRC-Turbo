package org.firstinspires.ftc.teamcode.system

import org.firstinspires.ftc.teamcode.hardware.Hardware
import org.firstinspires.ftc.teamcode.hardware.getMotorAngle
import org.firstinspires.ftc.teamcode.hardware.getMotorVelocity
import org.firstinspires.ftc.teamcode.hardware.liftTpr
import org.firstinspires.ftc.teamcode.system.LiftModel.additionalVoltsForLimiter
import org.firstinspires.ftc.teamcode.system.LiftModel.ff
import org.firstinspires.ftc.teamcode.system.LiftModel.spoolRadius
import org.firstinspires.ftc.teamcode.system.LiftModel.ugff
import org.firstinspires.ftc.teamcode.system.LiftModel.voltsPerVel
import org.futurerobotics.botsystem.LoopElement
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.jargon.linalg.*
import org.futurerobotics.jargon.math.TAU
import org.futurerobotics.jargon.math.convert.*
import org.futurerobotics.jargon.model.MotorModel
import org.futurerobotics.jargon.statespace.*

@UseExperimental(ExperimentalStateSpace::class)
object LiftModel {

    //    private val kAug: Mat
    val controller: GainController
    val ff: ReferenceTrackingFeedForward
    val matrices: DiscreteStateSpaceMatrices

    const val spoolRadius = 16 * mm // 0.05mm higher because string. Well, accuracy of measurement.
    private const val mechanismMass = 4 * kg
    private const val g = 9.8 * m / s / s
    val ugff: Double
    val voltsPerVel: Double
    const val additionalVoltsForLimiter = -3.0

    init {
        // HEY FUTURE BEN
        // FIXME
        //   MAKE A WAY FOR THINGS TO BE EASIER FOR THINGS LIKE SPOOLS

        val motor = MotorModel.fromMotorData(
            12.0,
            175 * ozf * `in`,
            11.5,
            340 * rev / mins,
            0.5
        )
        voltsPerVel = motor.voltsPerAngVel / spoolRadius

        val voltsPerAccel = motor.voltsPerTorque * spoolRadius * mechanismMass / 2

        val accelPerVolt = 1 / voltsPerAccel
        val deccelFromVel = -accelPerVolt * voltsPerVel

        //state = [pos, vel]
        //signal = [volts]

        //this
//@formatter:off
        val a = Mat(
            //      pos  vel
            /*pos*/  0,   1 to
            /*vel*/  0,   deccelFromVel
        )
        val b = Mat(
            0 to
            accelPerVolt
        )
//@formatter:on
        val c = idenMat(2) / spoolRadius

        val k = Mat(125, 5) //discrete does not work right now

        ugff = voltsPerAccel * g

        controller = GainController(k)
        matrices = ContinuousStateSpaceMatrices(a, b, c).discretize(ControlLoop.PERIOD)
        ff = ReferenceTrackingFeedForward(plantInversion(b), false)
    }

    private val initialCovariance = diagMat(0.0, 0.0) //


    private val noiseCovariance = NoiseCovariance(
        diagMat(
            5 * `in` * ControlLoop.PERIOD,
            5 * `in` * ControlLoop.PERIOD
        ),
        diagMat(TAU / liftTpr / 2, TAU / liftTpr / 2)
        //only a resolution of "ticks". so i just put a random estimate
    )

    fun getObserver() = LinearKalmanFilter(
        ConstantNoiseCovariance(noiseCovariance),
        initialCovariance
    )
}

//todo: standardizish
class SingleLiftController {

    private val matrices: DiscreteStateSpaceMatrices = LiftModel.matrices
    private val observer = LiftModel.getObserver()
    private val controller = LiftModel.controller

    var signal: Double = 0.0
        private set
    var state = zeroVec(2)
        private set
    private var currentTimeNanos = 0L

    fun reset(initialState: Vec) {
        observer.reset(initialState)
        currentTimeNanos = 0L
    }

    fun update(r: Vec, y: Vec, elapsedTimeInNanos: Long): Double {
        currentTimeNanos += elapsedTimeInNanos
        val x = observer.update(matrices, createVec(signal), y, currentTimeNanos)
        val u = controller.getSignal(matrices, x, r, currentTimeNanos)
        val r1 = r.copy().also { it[0] += it[1] * elapsedTimeInNanos / 1e9 }
        val uff = ff.getFeedForward(matrices, r, r1)

        val vel = x[1]
        val minVolts = additionalVoltsForLimiter + voltsPerVel * vel
        signal = (u[0] + uff[0] + ugff).coerceAtLeast(minVolts)
        state = x
        return signal
    }
}


interface LiftTarget {
    /** meters */
    val liftHeight: Double
    /** m/s */
    val liftVelocity: Double
}

class LiftController
@JvmOverloads constructor(
    private val debug: Boolean = false
) : LoopElement() {

    private val controlLoop by loopOn<ControlLoop>()

    private val theBulkData: TheBulkData by dependency()
    private val hardware: Hardware by dependency()
    private val target: LiftTarget by dependency()
    private val controllers = List(2) { SingleLiftController() }
    private val telemetry by onInit { botSystem.tryGet<OpModeElement>()?.opMode?.telemetry }

    var currentHeight: Double = 0.0
        private set

    override fun init() {
        val liftMotors = hardware.liftsMotors ?: error("Lift motors not found! (check config)")
        liftMotors.forEach {
            it.resetPosition()
        }
        controllers.forEach {
            it.reset(zeroVec(2))
        }
    }


    @UseExperimental(ExperimentalStateSpace::class)
    override fun loop() {
        val motors = hardware.liftsMotors!!
        val bulkData = theBulkData.value
        val targetPos = target.liftHeight
        val targetVel = target.liftVelocity
        val ys = motors.map {
            createVec(
                bulkData.getMotorAngle(it),
                bulkData.getMotorVelocity(it)
            )
        }
        val r = createVec(targetPos, targetVel)

        controllers.forEachIndexed { i, controller ->
            val y = ys[i]
            motors[i].voltage = controller.update(
                r, y, controlLoop.elapsedNanos
            )
        }
        currentHeight = controllers.sumByDouble { it.state[0] } / 2

        if (debug) telemetry?.apply {
            addLine("Target : p%.2f, v%.2f".format(targetPos, targetVel))
            repeat(2) { i ->
                val measurement = ys[i] * spoolRadius
                val state = controllers[i].state
                val signal = controllers[i].signal
                addLine("$i")
                addLine("Measure: p%4.3f, v%4.3f".format(measurement[0], measurement[1]))
                addLine("State  : p%4.3f, v%4.3f".format(state[0], state[1]))
                addLine("Signal : v%4.3f".format(signal))
            }
        }
    }
}
