package org.firstinspires.ftc.teamcode.original.BotSys;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class drivetrain {
	DcMotorEx frontLeft;
	DcMotorEx backLeft;
	DcMotorEx frontRight;
	DcMotorEx backRight;

	public drivetrain (DcMotorEx fL, DcMotorEx fR, DcMotorEx rL, DcMotorEx rR){
		frontLeft = fL;
		backLeft = rL;
		frontRight =fR;
		backRight = rR;

//		frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
//		backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
//		frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
//		backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

		frontRight.setDirection(DcMotorSimple.Direction.REVERSE);
		backRight.setDirection(DcMotorSimple.Direction.REVERSE);
	}

	public void move (double x, double y, double rot) {
		//set powers
		double frontLeft = (y + rot - x);
		double backLeft  = (y + rot + x);
		double frontRight= (y - rot + x);
		double backRight = (y - rot - x);

		//normalize
		double max = Math.max(Math.max(frontLeft, frontRight), Math.max(backLeft, backRight));
		double regulationFactor = (max > 1) ? 1/max : 1;

		//apply normalization
		frontLeft *= regulationFactor;
		frontRight *= regulationFactor;
		backLeft *= regulationFactor;
		backRight *= regulationFactor;

		this.frontLeft.setPower(frontLeft);
		this.frontRight.setPower(frontRight);
		this.backLeft.setPower(backLeft);
		this.backRight.setPower(backRight);
	}

	public double movementUnits(){
		return  frontLeft.getCurrentPosition() +
				frontRight.getCurrentPosition()+
				backLeft.getCurrentPosition()  +
				backRight.getCurrentPosition();
	}

	public void waitDeltaMovementUnits(int num){
		double begin = movementUnits();

		while(Math.abs(movementUnits()-begin) < num){
			//wait
		}
	}
}
