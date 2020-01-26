package org.firstinspires.ftc.teamcode.system

import com.qualcomm.robotcore.hardware.Gamepad

/**
 * Utilites for when button is clicked/released
 */
class Buttons(private val gamepad: Gamepad) {

    val a = Button()
    val b = Button()
    val x = Button()
    val y = Button()
    val dpad_up = Button()
    val dpad_down = Button()
    val dpad_left = Button()
    val dpad_right = Button()
    val left_bumper = Button()
    val right_bumper = Button()
    val left_stick_button = Button()
    val right_stick_button = Button()
    val start = Button()
    val back = Button()

    @Suppress("DuplicatedCode")
    fun update() {
        a update gamepad.a
        b update gamepad.b
        x update gamepad.x
        y update gamepad.y
        dpad_up update gamepad.dpad_up
        dpad_down update gamepad.dpad_down
        dpad_left update gamepad.dpad_left
        dpad_right update gamepad.dpad_right
        left_bumper update gamepad.left_bumper
        right_bumper update gamepad.right_bumper
        left_stick_button update gamepad.left_stick_button
        right_stick_button update gamepad.right_stick_button
        start update gamepad.start
        back update gamepad.back
    }


    class Button {
        var pastState: Boolean = false
            private set

        var currentState: Boolean = false
            private set

        val isPressed get() = currentState
        val isReleased get() = !currentState
        val isClicked get() = currentState && !pastState
        val isUpClicked get() = !currentState && pastState

        internal infix fun update(newState: Boolean) {
            pastState = currentState
            currentState = newState
        }
    }
    /*
        a                       update          gamepad.a
        b                       update          gamepad.b
        x                       update          gamepad.x
        y                       update          gamepad.y
        dpad_up                 update          gamepad.dpad_up
        dpad_down               update          gamepad.dpad_down
        dpad_left               update          gamepad.dpad_left
        dpad_right              update          gamepad.dpad_right
        left_bumper             update          gamepad.left_bumper
        right_bumper            update          gamepad.right_bumper
        left_stick_button       update          gamepad.left_stick_button
        right_stick_button      update          gamepad.right_stick_button
        start                   update          gamepad.start
        back                    update          gamepad.back
     */
}
