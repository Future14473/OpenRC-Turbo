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

	public DcMotorEx intakeRight;
	public DcMotorEx intakeLeft;

	public Servo extendLeft;
	public Servo extendRight;

	public Servo spin;
	public Servo grab;

	public Servo gate;

	public Servo foundationLeft;
	public Servo foundationRight;

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

		spin = hardwaremap.get(Servo.class, "Rotater");
		grab = hardwaremap.get(Servo.class, "Claw");
		gate = hardwaremap.get(Servo.class, "Dropper");

		foundationLeft = hardwaremap.get(Servo.class, "GrabberLeft");
		foundationRight = hardwaremap.get(Servo.class, "GrabberRight");

		extendLeft = hardwaremap.tryGet(Servo.class, "LinkageLeft");
		extendRight = hardwaremap.tryGet(Servo.class, "LinkageRight");

		imu = hardwaremap.get(BNO055IMU.class, "imu");
	}
}
