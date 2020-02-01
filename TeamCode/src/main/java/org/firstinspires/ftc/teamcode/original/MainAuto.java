package org.firstinspires.ftc.teamcode.original;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.original.BotSys.imu;
import org.firstinspires.ftc.teamcode.original.BotSys.map;
import org.firstinspires.ftc.teamcode.original.BotSys.robot;

import detectors.FoundationPipeline.SkyStone;
import detectors.OpenCvDetector;

/*

    If you're using this library, THANKS! I spent a lot of time on it.

    However, stuff isn't as well-documented as I like...still working on that

    So if you have questions, email me at xchenbox@gmail.com and I will get back to you in about a day (usually)

    Enjoy!
*/

@TeleOp(name = "AUTONOMOUS+", group = "Auto")
public class MainAuto extends LinearOpMode {

	OpenCvDetector  fieldElementDetector;
	map             hardwareMap;
	robot           robot;
	imu             imu;

	@Override
	public void runOpMode() throws InterruptedException {
		telemetry.setAutoClear(true);
		fieldElementDetector = new OpenCvDetector(this);
		hardwareMap = new map(super.hardwareMap);
		robot = new robot(hardwareMap);
		imu = new imu(hardwareMap);

		waitForStart();

		fieldElementDetector.start();

		skyStoneAlign();

		robot.drivetrain.move(1,-0.1,0);
		robot.drivetrain.waitDeltaMovementUnits(200);

		robot.drivetrain.rotateDegrees(-90, imu);

		robot.drivetrain.move(0,1,0);
		robot.drivetrain.waitDeltaMovementUnits(50);

		robot.drivetrain.rotateDegrees(90, imu);

		fieldElementDetector.stop();
	}

	void skyStoneAlign() {
		//get X position of first SkyStone
		boolean aligned = false;
		int noneInd = 0;

		while (!aligned && noneInd < 50 && opModeIsActive()) {
			//Skystone order in Array is left to right
			SkyStone[] elements = fieldElementDetector.getSkyStones();

			//empty
			find:if (elements.length == 0) {
				telemetry.addData("Nothing found", "For " + noneInd + "frames");

				noneInd++;
			} else {//detected
				SkyStone that = elements[0];

				if(that == null){
					telemetry.addData("NULL","NULL");
					break find;
				}

				int xpos = (int) that.x;
				telemetry.addData("Position", xpos);

				aligned = moveRobot(xpos);

				noneInd = 0;
			}
			telemetry.update();
		}
	}


	private boolean moveRobot(int xpos) {
		//The center of the screen has x value of 320
		int difference = xpos-320;

		//75 pixels ought to be close enough
		if(Math.abs(difference) < 75) {
			telemetry.addLine("Aligned to SkyStone!");
			return true;
		}

		if (difference > 0) {

			telemetry.addData("Moving","Right");
		} else {
			//Strafe Left
			telemetry.addData("Moving","Left");
		}

		return false;
	}
}

