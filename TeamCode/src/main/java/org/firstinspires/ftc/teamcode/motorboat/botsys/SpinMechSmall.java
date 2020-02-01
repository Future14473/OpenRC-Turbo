package org.firstinspires.ftc.teamcode.motorboat.botsys;

import com.qualcomm.robotcore.hardware.Servo;

import java.util.Arrays;
import java.util.Objects;

public class SpinMechSmall implements SpinMech {
	private Servo[] allMotors;
	private float currentPos = 0;

	/**
	 * Close is LESS; open is MORE
	 */
	public SpinMechSmall(float openPos, float closePos, Servo... motors) {
		allMotors = motors;
		allMotors = Arrays.stream(allMotors)
				            .filter(Objects::nonNull)
				            .toArray(Servo[]::new);
		Arrays.stream(allMotors).forEach(motor -> motor.scaleRange(
				Math.min(closePos, openPos),
				Math.max(openPos, closePos)));
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

	public void stop() {

	}

	@Override
	public void setPosition(float pos) {
		Arrays.stream(allMotors).forEach(motor -> {
			motor.setPosition(pos);
			currentPos = pos;
		});
	}

	public float getPos() {
		return currentPos;
	}
}
