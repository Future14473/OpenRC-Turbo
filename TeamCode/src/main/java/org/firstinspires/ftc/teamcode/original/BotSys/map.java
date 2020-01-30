package org.firstinspires.ftc.teamcode.original.BotSys;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

public class map {
	DcMotorEx frontLeft;
	DcMotorEx backLeft;
	DcMotorEx frontRight;
	DcMotorEx backRight;

	public DcMotorEx liftRight;
	public DcMotorEx liftLeft;

	DcMotorEx intakeRight;
	DcMotorEx intakeLeft;

	Servo extendLeft;
	Servo extendRight;

	Servo spin;
	Servo grab;

	Servo foundationLeft;
	Servo foundationRight;

	public
	BNO055IMU imu;

	public map(HardwareMap hardwaremap){
		//TODO
		frontLeft = hardwaremap.get(DcMotorEx.class, "FrontLeft");
		frontRight = hardwaremap.get(DcMotorEx.class, "FrontRight");
		backLeft = hardwaremap.get(DcMotorEx.class, "BackLeft");
		backRight = hardwaremap.get(DcMotorEx.class, "BackRight");

		intakeLeft = hardwaremap.get(DcMotorEx.class, "IntakeLeft");
		intakeRight = hardwaremap.get(DcMotorEx.class, "IntakeRight");
		intakeRight.setDirection(DcMotorSimple.Direction.REVERSE);

		liftLeft = hardwaremap.get(DcMotorEx.class, "LiftLeft");
		liftRight = hardwaremap.get(DcMotorEx.class, "LiftRight");

		imu = hardwaremap.get(BNO055IMU.class, "imu");
	}
}
