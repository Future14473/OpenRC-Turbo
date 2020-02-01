package org.firstinspires.ftc.teamcode.original;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.original.botsys.Drivetrain;
import org.firstinspires.ftc.teamcode.original.botsys.Imu;
import org.firstinspires.ftc.teamcode.original.botsys.Map;
import org.firstinspires.ftc.teamcode.original.botsys.Robot;

import java.util.Arrays;

import detectors.FoundationPipeline.SkyStone;
import detectors.OpenCvDetector;

/*

    If you're using this library, THANKS! I spent a lot of time on it.

    However, stuff isn't as well-documented as I like...still working on that

    So if you have questions, email me at xchenbox@gmail.com and I will get back to you in about a day (usually)

    Enjoy!
*/

@TeleOp(name = "AUTONOMOUS+", group = "Auto")
class MainAuto extends LinearOpMode {

	private OpenCvDetector fieldElementDetector;
	private Map            hardwareMap;
	private Robot          robot;
	private Imu            imu;

	@Override
	public void runOpMode() throws InterruptedException {
		telemetry.setAutoClear(true);
		fieldElementDetector = new OpenCvDetector(this);
		hardwareMap = new Map(super.hardwareMap);
		robot = new Robot(hardwareMap);
		imu = new Imu(hardwareMap);

		waitForStart();

		fieldElementDetector.start();

		skyStoneAlign();

		Drivetrain drivetrain = robot.drivetrain;
		drivetrain.move(0, 0.6, 0);
		drivetrain.waitDeltaMovementUnits(200);

		drivetrain.rotateDegrees(-90, imu);

		drivetrain.move(0, 1, 0);
		drivetrain.waitDeltaMovementUnits(50);

		drivetrain.rotateDegrees(90, imu);

		fieldElementDetector.stop();
	}

	private void skyStoneAlign() {
		//get X position of first SkyStone
		boolean aligned = false;
		int noneInd = 0;

		while (!aligned && noneInd < 50 && opModeIsActive()) {
			//Skystone order in Array is left to right
			SkyStone[] elements = fieldElementDetector.getSkyStones();

			Arrays.sort(elements);

			//empty
			find:
			if (elements.length == 0) {
				telemetry.addData("Nothing found", "For " + noneInd + "frames");

				noneInd++;
			} else {//detected
				SkyStone that = elements[0];

				if (that == null) {
					telemetry.addData("NULL", "NULL");
					break find;
				}

				int xpos = (int) that.x;
				telemetry.addData("Position", xpos + " " + that.size);

				aligned = moveRobot(xpos);

				noneInd = 0;
			}
			telemetry.update();
		}
	}


	private boolean moveRobot(int xpos) {
		//The center of the screen has x value of 320
		int difference = xpos - 380;

		//75 pixels ought to be close enough
		if (Math.abs(difference) < 75) {
			telemetry.addLine("Aligned to SkyStone!");
			return true;
		}

		if (difference > 0) {

			telemetry.addData("Moving", "Right");
		} else {
			//Strafe Left
			telemetry.addData("Moving", "Left");
		}

		return false;
	}
}

