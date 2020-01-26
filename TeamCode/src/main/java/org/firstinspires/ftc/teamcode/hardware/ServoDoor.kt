package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.robotcore.hardware.Servo

/**
 * Hopefully self explanatory
 */
class ServoDoor(
    private val servo: Servo,
    private val closeOpenRange: ClosedFloatingPointRange<Double>,
    initialIsOpen: Boolean? = null
) {

    var isOpen = false
        set(value) {
            servo.position = if (value) closeOpenRange.endInclusive else closeOpenRange.start
            field = value
        }

    var isClosed
        get() = !isOpen
        set(value) {
            isOpen = !value
        }

    init {
        if (initialIsOpen != null) {
            isOpen = initialIsOpen
        }
    }

    fun close() {
        isOpen = false
    }

    fun open() {
        isOpen = true
    }
}
