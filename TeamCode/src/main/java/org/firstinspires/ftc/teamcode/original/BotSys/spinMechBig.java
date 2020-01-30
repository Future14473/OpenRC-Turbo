package org.firstinspires.ftc.teamcode.original.BotSys;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.original.MainTeleOp;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class spinMechBig implements spinMech{
	float power = 1;
	DcMotorEx[] allMotors;
	int lowerBound;
	int higherBound;

	public spinMechBig(DcMotorEx... motors){
		this(Integer.MIN_VALUE, Integer.MAX_VALUE, motors);
	}

	public spinMechBig(int lowerBound, int higherBound,DcMotorEx... motors){
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
			motor.setVelocity(power*1120);
		});
				//(motor.getCurrentPosition()<higherBound)?1:0));
	}

	@Override
	public void spinClose() {
		Arrays.stream(allMotors).forEach(motor -> {
			motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			motor.setVelocity(-power*1120);
		});
				//(motor.getCurrentPosition()>lowerBound)?-1:0));
	}

	public void stop(){
		Arrays.stream(allMotors).forEach(motor -> motor.setPower(0));
	}

	public void setPower(float pwr){
		power = pwr;
	}
}
