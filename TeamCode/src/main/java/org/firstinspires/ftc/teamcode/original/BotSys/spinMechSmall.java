package org.firstinspires.ftc.teamcode.original.BotSys;

import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.original.utils.Calc;

import java.util.Arrays;

public class spinMechSmall  implements spinMech{
	Servo[] allMotors;
	public float currentPos = 0;
	/**
	 * Close is LESS; open is MORE
	 * @param openPos
	 * @param closePos
	 * @param motors
	 */
	public spinMechSmall(float openPos, float closePos, Servo... motors){
		allMotors = motors;
		allMotors = Calc.removeNulls(allMotors);
		Arrays.stream(allMotors).forEach(motor -> motor.scaleRange(
				closePos<openPos?closePos:openPos,
				openPos>closePos?openPos:closePos));
	}
	/**
	 * Note: open is more value
	 */
	@Override
	public void open() {
		setPosition(1);
	}

	/**
	 * Note: close is less value
	 */
	@Override
	public void close() {
		setPosition(0);
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

	@Override
	public void setPosition(float pos) {
		Arrays.stream(allMotors).forEach(motor -> {
			motor.setPosition(pos);
			currentPos = pos;
		});
	}

	public float getPos(){
		return currentPos;
	}
}
