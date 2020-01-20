package org.firstinspires.ftc.teamcode.system

import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.teamcode.ROBOT_LOOP_PERIOD
import org.firstinspires.ftc.teamcode.hardware.*
import org.futurerobotics.botsystem.*
import org.futurerobotics.botsystem.ftc.OpModeElement
import org.futurerobotics.jargon.linalg.Vec
import org.futurerobotics.jargon.linalg.mapToVec

class ControlLoop : LoopManager(ROBOT_LOOP_PERIOD)
class ButtonsElement : BaseElement() {
    private val opMode by dependency(OpModeElement::class) { opMode }
    private val controlLoop by dependency<ControlLoop>()

    val gamepad1Buttons by buttonsLoop(opMode.gamepad1)

    val gamepad2Buttons by buttonsLoop(opMode.gamepad2)

    private fun buttonsLoop(gamepad: Gamepad): Property<LoopValue<Buttons>> {
        val buttons = Buttons(gamepad)
        return onInit {
            controlLoop.addElement {
                buttons.updateNow()
                buttons
            }
        }
    }
}

@Suppress("DuplicatedCode")
class Measurements : BaseElement() {

    init {
        dependsOn<Hardware>()
    }

    private val controlLoop by dependency<ControlLoop>()

    lateinit var bulkData: LoopValue<BulkData>
        private set

    private var _angle: LoopValue<Double>? = null
    val heading: LoopValue<Double> get() = _angle ?: error("IMU NOT FOUND")

    private var _angularVelocity: LoopValue<Double>? = null
    val angularVelocity: LoopValue<Double> get() = _angularVelocity ?: error("IMU NOT FOUND")


    private var _wheelPositions: LoopValue<Vec>? = null
    val wheelPositions: LoopValue<Vec> get() = _wheelPositions ?: error("WHEELS NOT FOUND")

    private var _wheelVelocities: LoopValue<Vec>? = null
    val wheelVelocities: LoopValue<Vec> get() = _wheelPositions ?: error("WHEELS NOT FOUND")

    private var _liftPositions: LoopValue<Vec>? = null
    val listPositions: LoopValue<Vec> get() = _liftPositions ?: error("LIFTS NOT FOUND")

    private var _liftVelocities: LoopValue<Vec>? = null
    val liftVelocities: LoopValue<Vec> get() = _liftPositions ?: error("LIFTS NOT FOUND")


    override fun init() {
        val hardware: Hardware = botSystem.get()
        val hubs = hardware.hubs ?: throw IllegalStateException(
            """
                |Expansion Hubs not found! Oh no!
                |
                |Lots of things rely on this!
                |
                |I don't know try restarting the app!
                |
                |I'm going to regret this error message later!
                """.trimMargin()
        )
        controlLoop.apply {
            bulkData = controlLoop.addElement {
                MultipleBulkData(hubs.map { it.bulkInputData })
            }
            val imu = hardware.gyro
            if (imu != null) {
                _angle = addElement {
                    imu.angle
                }
                _angularVelocity = addElement {
                    imu.angularVelocity
                }
            }
            val wheels = hardware.wheelMotors
            if (wheels != null) {
                _wheelPositions = addElement {
                    val bulk = bulkData.currentValue.await()
                    wheels.mapToVec { bulk.getMotorCurrentAngle(it) }
                }
                _wheelVelocities = addElement {
                    val bulk = bulkData.currentValue.await()
                    wheels.mapToVec { bulk.getMotorAngularVelocity(it) }
                }
            }

            val lifts = hardware.liftsMotors
            if (lifts != null) {
                _liftPositions = addElement {
                    val bulk = bulkData.currentValue.await()
                    lifts.mapToVec { bulk.getMotorCurrentAngle(it) }
                }
                _liftVelocities = addElement {
                    val bulk = bulkData.currentValue.await()
                    lifts.mapToVec { bulk.getMotorAngularVelocity(it) }
                }
            }
        }
    }
}


