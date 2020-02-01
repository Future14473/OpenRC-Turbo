package org.firstinspires.ftc.teamcode.motorboat

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import detectors.OpenCvDetector
import org.firstinspires.ftc.teamcode.motorboat.botsys.BotMap
import org.firstinspires.ftc.teamcode.motorboat.botsys.Imu
import org.firstinspires.ftc.teamcode.motorboat.botsys.Robot

/*

    If you're using this library, THANKS! I spent a lot of time on it.

    However, stuff isn't as well-documented as I like...still working on that

    So if you have questions, email me at xchenbox@gmail.com and I will get back to you in about a day (usually)

    Enjoy!
*/
@TeleOp(name = "AUTONOMOUS+", group = "Auto")
internal class MainAuto : LinearOpMode() {

    private lateinit var fieldElementDetector: OpenCvDetector
    private lateinit var botMap: BotMap
    private lateinit var robot: Robot
    private lateinit var imu: Imu
    override fun runOpMode() {
        telemetry.isAutoClear = true
        fieldElementDetector = OpenCvDetector(hardwareMap.appContext)
        botMap = BotMap(super.hardwareMap)
        robot = Robot(botMap)
        imu = Imu(botMap)
        waitForStart()


        fieldElementDetector.start()
        skyStoneAlign()
        val drivetrain = robot.drivetrain
        drivetrain.move(0.0, 0.6, 0.0)
        drivetrain.waitDeltaMovementUnits(200)
        drivetrain.rotateDegrees(-90f, imu)
        drivetrain.move(0.0, 1.0, 0.0)
        drivetrain.waitDeltaMovementUnits(50)
        drivetrain.rotateDegrees(90f, imu)
        fieldElementDetector.stop()
    }

    private fun skyStoneAlign() { //get X position of first SkyStone
        var aligned = false
        var noneInd = 0
        while (!aligned && noneInd < 50 && opModeIsActive()) { //Skystone order in Array is left to right
            val elements = fieldElementDetector.skyStones
            if (elements.isEmpty()) {
                telemetry.addData("Nothing found", "For " + noneInd + "frames")
                noneInd++
            } else { //detected
                val that = elements.min()
                if (that == null) {
                    telemetry.addData("NULL", "NULL")
                } else {
                    val xpos = that.x.toInt()
                    telemetry.addData("Position", xpos.toString() + " " + that.size)
                    aligned = moveRobot(xpos)
                    noneInd = 0
                }
            }
            telemetry.update()
        }
    }

    private fun moveRobot(xpos: Int): Boolean { //The center of the screen has x value of 320
        val difference = xpos - 380
        //75 pixels ought to be close enough
        if (Math.abs(difference) < 75) {
            telemetry.addLine("Aligned to SkyStone!")
            return true
        }
        if (difference > 0) {
            telemetry.addData("Moving", "Right")
        } else { //Strafe Left
            telemetry.addData("Moving", "Left")
        }
        return false
    }
}
