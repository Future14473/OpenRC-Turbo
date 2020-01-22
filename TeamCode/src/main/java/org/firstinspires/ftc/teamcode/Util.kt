package org.firstinspires.ftc.teamcode


/**
 * Only exists so that the formatter isn't annoying.
 *
 * `of the x` returns `x`.
 */
object of {

    inline infix fun <T> the(t: T): T = t
}
