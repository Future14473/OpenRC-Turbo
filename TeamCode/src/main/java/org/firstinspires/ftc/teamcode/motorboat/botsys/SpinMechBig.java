package org.firstinspires.ftc.teamcode.motorboat.botsys;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import java.util.Arrays;

public class SpinMechBig implements SpinMech {
	private float power = 1;
	private DcMotorEx[] allMotors;

	public SpinMechBig(DcMotorEx... motors) {
		allMotors = motors;
		Arrays.stream(allMotors).forEach(motor -> {
			motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
			motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
		});
	}

	@Override
	public void open() {
	}

	@Override
	public void close() {
	}

	@Override
	public void spinOpen() {
		Arrays.stream(allMotors).forEach(motor -> {
			motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			motor.setPower(power);
		});
		//(motor.getCurrentPosition()<higherBound)?1:0));
	}

	@Override
	public void spinClose() {
		Arrays.stream(allMotors).forEach(motor -> {
			motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			motor.setPower(-power);
		});
		//(motor.getCurrentPosition()>lowerBound)?-1:0));
	}

	public void stop() {
		Arrays.stream(allMotors).forEach(motor -> motor.setPower(0));
	}

	@Override
	public void setPosition(float pos) {

	}

	public float getPos() {
		//noinspection OptionalGetWithoutIsPresent
		return (float) Arrays.stream(allMotors).mapToInt(DcMotor::getCurrentPosition).average().getAsDouble();
	}
}
