package org.firstinspires.ftc.teamcode.original.BotSys;

import com.qualcomm.robotcore.hardware.Servo;

import java.util.Arrays;

public class spinMechSmall  implements spinMech{
	Servo[] allMotors;

	/**
	 * Close is LESS; open is MORE
	 * @param openPos
	 * @param closePos
	 * @param motors
	 */
	public spinMechSmall(int openPos, int closePos, Servo... motors){
		allMotors = motors;
		Arrays.stream(allMotors).forEach(motor -> motor.scaleRange(closePos, openPos));
	}
	/**
	 * Note: open is more value
	 */
	@Override
	public void open() {
		Arrays.stream(allMotors).forEach(motor -> motor.setPosition(1));
	}

	/**
	 * Note: close is less value
	 */
	@Override
	public void close() {
		Arrays.stream(allMotors).forEach(motor -> motor.setPosition(0));
	}

	@Override
	public void spinOpen() {
		//no lol
	}

	@Override
	public void spinClose() {
		//no lol
	}

	public void stop(){

	}

	@Override
	public void setPower(float pwr) {
		//none lol
	}
}
