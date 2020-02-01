package org.firstinspires.ftc.teamcode.motorboat;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import detectors.FoundationPipeline.SkyStone;
import detectors.OpenCvDetector;
import org.firstinspires.ftc.teamcode.motorboat.botsys.BotMap;
import org.firstinspires.ftc.teamcode.motorboat.botsys.Imu;
import org.firstinspires.ftc.teamcode.motorboat.botsys.Robot;

@TeleOp(name = "AUTO TEST", group = "Auto")
@Disabled
class AlignTest extends LinearOpMode {

	private OpenCvDetector fieldElementDetector;
	private BotMap hardwareMap;
	private Robot robot;
	private Imu imu;

	@Override
	public void runOpMode() throws InterruptedException {
		telemetry.setAutoClear(true);
		fieldElementDetector = new OpenCvDetector(super.hardwareMap.appContext);
		hardwareMap = new BotMap(super.hardwareMap);
		robot = new Robot(hardwareMap);
		imu = new Imu(hardwareMap);

		waitForStart();

		fieldElementDetector.start();

		while (opModeIsActive())
			skyStoneAlign();

		fieldElementDetector.stop();
	}

	private void skyStoneAlign() {
		//get X position of first SkyStone
		boolean aligned = false;
		int noneInd = 0;

		//while (!aligned && noneInd < 50 && opModeIsActive()) {
		//Skystone order in Array is left to right
		SkyStone[] elements = fieldElementDetector.getSkyStones();

		//empty
		if (elements.length == 0) {
			telemetry.addData("Nothing found", "For " + noneInd + "frames");

			noneInd++;
		} else {//detected
			int xpos = (int) elements[0].x;
			telemetry.addData("Position", xpos);

			aligned = moveRobot(xpos);

			noneInd = 0;
		}
		telemetry.update();
		//}
	}

	private boolean moveRobot(int xpos) {
		//The center of the screen has x value of 320
		int difference = xpos - 380;

		//75 pixels ought to be close enough
		if (Math.abs(difference) < 75) {
			telemetry.addLine("Aligned to SkyStone!");
			robot.drivetrain.move(-0, 0, 0);
			return true;
		}

		if (difference > 0) {

			telemetry.addData("Moving", "Right");
			robot.drivetrain.move(-0.5, 0, 0);
		} else {
			//Strafe Left
			telemetry.addData("Moving", "Left");
			robot.drivetrain.move(0.5, 0, 0);
		}

		return false;
	}
}

