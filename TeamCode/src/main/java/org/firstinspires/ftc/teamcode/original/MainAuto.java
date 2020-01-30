package org.firstinspires.ftc.teamcode.original;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.original.BotSys.map;
import org.firstinspires.ftc.teamcode.original.BotSys.robot;

import detectors.OpenCvDetector;

@TeleOp(name = "CV test", group = "Auto")
public class MainAuto extends OpMode {

	map hardwareMap;
	robot robot;

	/*
	 * Once when init
	 */
	@Override
	public void init() {
		hardwareMap = new map(super.hardwareMap);
		robot = new robot(hardwareMap);
	}


	public void init_loop() {}
	public void start() {}



	/*
	 * repeat when play
	 */
	@Override
	public void loop() {
		robot.move(gamepad1.right_stick_x,gamepad1.right_stick_y,gamepad1.left_stick_x);

	}




	/*
	 * Code to run ONCE after the driver hits STOP
	 */
	@Override
	public void stop() {

	}
}
