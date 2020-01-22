package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.robotcore.hardware.Servo

/**
 * Hopefully self explanatory
 */
class ServoDoor(
    private val servo: Servo,
    private val closeOpenRange: ClosedFloatingPointRange<Double>,
    initialIsOpen: Boolean
) {

    var isOpen = false
        set(value) {
            if (value != field) {
                servo.position = if (value) closeOpenRange.endInclusive else closeOpenRange.start
            }
            field = value
        }

    init {
        isOpen = initialIsOpen
    }

    fun close() {
        isOpen = false
    }

    fun open() {
        isOpen = true
    }

}
