package org.firstinspires.ftc.teamcode.original.botsys;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.teamcode.original.utils.MutableDouble;

import java.util.Arrays;

public class SpinMechBig implements SpinMech {
	private float       power = 1;
	private DcMotorEx[] allMotors;
	private int         lowerBound;
	private int         higherBound;

	public SpinMechBig(DcMotorEx... motors) {
		this(Integer.MIN_VALUE, Integer.MAX_VALUE, motors);
	}

	private SpinMechBig(int lowerBound, int higherBound, DcMotorEx... motors) {
		allMotors = motors;
		this.lowerBound = lowerBound;
		this.higherBound = higherBound;
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
		MutableDouble sum = new MutableDouble(0);
		Arrays.stream(allMotors).forEach(motor -> sum.increment(motor.getCurrentPosition()));
		return (float) (sum.get() / allMotors.length);
	}
}
