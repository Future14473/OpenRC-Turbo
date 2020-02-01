package org.firstinspires.ftc.teamcode.original;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.original.botsys.Imu;
import org.firstinspires.ftc.teamcode.original.botsys.Map;
import org.firstinspires.ftc.teamcode.original.botsys.Robot;
import org.firstinspires.ftc.teamcode.original.utils.Calc;

@TeleOp(name = "TELEOP+", group = "TeleOp")
public class MainTeleOp extends OpMode {
	//this is an abuse of static
	public static Telemetry telemetry;

	private Map   hardwareMap;
	private Robot robot;
	private Imu   imu;

	private boolean upWasPressed   = false;
	private boolean downWasPressed = false;
	private boolean backWasPressed = false;
	private boolean isManual       = false;
	private float   target         = 0;

	/*
	 * Once when init
	 */
	@Override
	public void init() {
		telemetry = super.telemetry;
		hardwareMap = new Map(super.hardwareMap);
		robot = new Robot(hardwareMap);
		imu = new Imu(hardwareMap);
		telemetry.addLine("Initialization done");

		robot.deExtend();
		robot.spinRest();
		robot.grabOpen();
		robot.gateClose();
	}


	public void init_loop() {
	}

	public void start() {
	}


	/*
	 * repeat when play
	 */
	@Override
	public void loop() {
		//slow/fast mode
		if (gamepad1.left_trigger > 0.5) robot.drivetrain.power = 100000;
		else robot.drivetrain.power = 1f;

		robot.move(gamepad1.right_stick_x, gamepad1.right_stick_y, gamepad1.left_stick_x);

		//manual toggle
		if (gamepad2.back && !backWasPressed) {
			backWasPressed = true;
			isManual = !isManual;
		}
		if (!gamepad2.back) backWasPressed = false;

		//INTAKE
		if (gamepad2.right_trigger > 0.5) robot.intakeIn();
		else if (gamepad2.left_trigger > 0.5) robot.intakeOut();
		else robot.intakeStop();

		boolean hasGrabbed = robot.grab.getPos() > 0.5;

		//capstone
		if (gamepad2.y && hasGrabbed) robot.gateOpen();

		//SPIN
		if (gamepad2.a && hasGrabbed) {
			robot.extend();
			Calc.delay(500L);
			robot.spinMiddle();
		}
		if (gamepad2.b && hasGrabbed) {
			robot.extend();
			Calc.delay(500L);
			robot.spinOut();
		}
		if (gamepad2.left_bumper) {
			robot.grabOpen();

			if (isManual) return;

			Calc.delay(500L);
			robot.spinRest();
			Calc.delay(500L);
			robot.deExtend();
			target += 512 + 300;
			while (!robot.liftArrived()) {
				robot.moveToTarget(target);
				telemetry.addData("AWAIT ARRIVAL;", target);
				telemetry.update();
			}
			Calc.delay(500L);
			target = 0;
		}

		//GRAB
		if (gamepad2.right_bumper) {
			robot.grabClose();
		}
		//GATE

		//FOUNDATIONS
		if (gamepad1.a) robot.foundationRest();
		if (gamepad1.b) robot.foundationEngage();

		//is extendo extended
		boolean isExtended = robot.extendoMagicko.getPos() < 0.5;

		if (gamepad2.dpad_up && !upWasPressed && isExtended) {
			target += 4100 / 8;

			upWasPressed = true;
		}

		if (!gamepad2.dpad_up) {
			upWasPressed = false;
		}

		if (gamepad2.dpad_down && !downWasPressed) {
			target -= 4100 / 8;
			downWasPressed = true;
		}
		if (!gamepad2.dpad_down) {
			downWasPressed = false;
		}

		//variable lift
		if (Math.abs(gamepad2.left_stick_y) > 0.15 && isExtended)
			target += gamepad2.left_stick_y * 5;

		//variable extend
		if (Math.abs(gamepad2.right_stick_x) > 0.15) robot.extendoMagicko.setPosition(
				robot.extendoMagicko.getPos() + gamepad2.right_stick_x * 5);

		telemetry.addData("target", target);
		if (target < 80) target = 80;
		if (target > 4000) target = 4000;

		robot.moveToTarget(target);

		telemetry.addData("heading", imu.heading());

		telemetry.addData("IS EXTENDED??", robot.extendoMagicko.getPos());
		telemetry.addData("foundationsLEFT", hardwareMap.foundationLeft.getPosition());
		telemetry.addData("foundationsRIGHT", hardwareMap.foundationRight.getPosition());

		telemetry.update();
	}


	/*
	 * Code to run ONCE after the driver hits STOP
	 */
	@Override
	public void stop() {

	}
}
