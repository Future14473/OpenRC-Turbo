package org.firstinspires.ftc.teamcode.original;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.original.BotSys.imu;
import org.firstinspires.ftc.teamcode.original.BotSys.map;
import org.firstinspires.ftc.teamcode.original.BotSys.robot;

import detectors.OpenCvDetector;

@TeleOp(name = "TELEOP", group = "TeleOcp")
public class MainTeleOp extends OpMode {

	public static Telemetry telemetry;

	map hardwareMap;
	robot robot;
	imu imu;

	boolean upWasPressed = false;
	boolean downWasPressed = false;

	float target = 0;
	/*
	 * Once when init
	 */
	@Override
	public void init() {
		telemetry = super.telemetry;
		hardwareMap = new map(super.hardwareMap);
		robot = new robot(hardwareMap);
		imu = new imu(hardwareMap);
		telemetry.addLine("Initialization done :)");
	}


	public void init_loop() {}
	public void start() {}



	/*
	 * repeat when play
	 */
	@Override
	public void loop() {
		robot.move(gamepad1.right_stick_x,gamepad1.right_stick_y,gamepad1.left_stick_x);

		if(gamepad2.a) robot.intakeIn();
		if(gamepad2.y) robot.intakeOut();
		if(gamepad2.x) robot.intakeStop();

		if(gamepad2.dpad_up && !upWasPressed) {
			target += 200;
			upWasPressed = true;
		}

		if(!gamepad2.dpad_up){
			upWasPressed = false;
		}

		if(gamepad2.dpad_down && !downWasPressed) {
			target -=200;
			downWasPressed = true;
		}
		if(!gamepad2.dpad_down){
			downWasPressed = false;
		}

		telemetry.addData("target",target);
		if(target < 0) target = 0;
		if(target > 2000) target = 2000;

		robot.liftPower(target);

		telemetry.addData("liftRight", hardwareMap.liftRight.getCurrentPosition());
		telemetry.addData("liftLeft", hardwareMap.liftLeft.getCurrentPosition());
		telemetry.addData("imu heading",imu.heading());

		telemetry.update();
	}




	/*
	 * Code to run ONCE after the driver hits STOP
	 */
	@Override
	public void stop() {

	}
}
