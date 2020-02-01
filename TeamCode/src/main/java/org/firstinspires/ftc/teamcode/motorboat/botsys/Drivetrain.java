package org.firstinspires.ftc.teamcode.motorboat.botsys;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

public class Drivetrain {
	public float power = 1;
	private DcMotorEx frontLeft;
	private DcMotorEx backLeft;
	private DcMotorEx frontRight;
	private DcMotorEx backRight;

	public Drivetrain(DcMotorEx fL, DcMotorEx fR, DcMotorEx rL, DcMotorEx rR) {
		frontLeft = fL;
		backLeft = rL;
		frontRight = fR;
		backRight = rR;

//		frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
//		backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
//		frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
//		backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

		frontRight.setDirection(DcMotorSimple.Direction.REVERSE);
		backRight.setDirection(DcMotorSimple.Direction.REVERSE);
	}

	public void waitDeltaMovementUnits(int num) {
		double begin = movementUnits();

		while (Math.abs(movementUnits() - begin) < num) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private double movementUnits() {
		return frontLeft.getCurrentPosition() +
				       frontRight.getCurrentPosition() +
				       backLeft.getCurrentPosition() +
				       backRight.getCurrentPosition();
	}

	public void rotateDegrees(float degs, Imu imu) {
		float startDeg = imu.heading();

		while (Math.abs(degs - (imu.heading() - startDeg)) > 10) {
			move(0, 0, ((imu.heading() - startDeg) - degs) / 100f);
		}
	}

	public void move(double x, double y, double rot) {
		rot *= -1;
		//set powers
		double frontLeft = (y + rot - x);
		double backLeft = (y + rot + x);
		double frontRight = (y - rot + x);
		double backRight = (y - rot - x);

		//normalize
		double max = Math.max(Math.max(frontLeft, frontRight), Math.max(backLeft, backRight));
		double regulationFactor = (max > 1) ? 1 / max : 1;

		//apply normalization
		frontLeft *= regulationFactor;
		frontRight *= regulationFactor;
		backLeft *= regulationFactor;
		backRight *= regulationFactor;

		this.frontLeft.setVelocity(frontLeft * 1120 * power);
		this.frontRight.setVelocity(frontRight * 1120 * power);
		this.backLeft.setVelocity(backLeft * 1120 * power);
		this.backRight.setVelocity(backRight * 1120 * power);
	}
}
