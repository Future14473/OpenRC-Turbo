package org.firstinspires.ftc.teamcode.original.BotSys;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.original.MainTeleOp;

public class robot {
	//TODO
	drivetrain drivetrain;
	public spinMech lift;
	spinMech intake;

	map hardwareMap;

	public robot (map hardwareMap) {
		this.hardwareMap = hardwareMap;
		drivetrain = new drivetrain(hardwareMap.frontLeft,
									hardwareMap.frontRight,
									hardwareMap.backLeft,
									hardwareMap.backRight);
		lift = new spinMechBig(0,500, hardwareMap.liftLeft, hardwareMap.liftRight);
		intake = new spinMechBig(hardwareMap.intakeLeft, hardwareMap.intakeRight);
		lift.setPower(0.1f);
		//TODO
	}

	public void liftPower(float target){
		hardwareMap.liftRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		hardwareMap.liftLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

		float leftDiff =Math.abs(-hardwareMap.liftLeft.getCurrentPosition()-target);
		if(leftDiff > 20) {

			if (hardwareMap.liftLeft.getCurrentPosition() > -target) {
				hardwareMap.liftLeft.setPower(leftDiff/500f+0.1);
				MainTeleOp.telemetry.addData("left PWR","UP");

			}
			if (hardwareMap.liftLeft.getCurrentPosition() < -target) {
				hardwareMap.liftLeft.setPower(-(leftDiff/700f));
				MainTeleOp.telemetry.addData("left PWR","DOWN");
			}

		}

		float rightDiff = Math.abs(hardwareMap.liftRight.getCurrentPosition()-target);
		if(rightDiff > 20){
			if (hardwareMap.liftRight.getCurrentPosition()<target){
				hardwareMap.liftRight.setPower(rightDiff/500f+0.1);

			}else if (hardwareMap.liftRight.getCurrentPosition()>target){
				hardwareMap.liftRight.setPower(-(rightDiff/700f));

			}else {
				hardwareMap.liftRight.setPower(0);

			}
		}else{
			hardwareMap.liftLeft.setPower(0);
			hardwareMap.liftRight.setPower(0);
		}

		MainTeleOp.telemetry.addData("liftleft diff",leftDiff);
		MainTeleOp.telemetry.addData("liftright diff",rightDiff);
	}

	public void liftStop(){
		MainTeleOp.telemetry.addData("lift","stop");
		lift.stop();
	}

	public void intakeIn(){

		intake.spinClose();
	}

	public void intakeOut(){
		intake.spinOpen();
	}

	public void intakeStop(){
		intake.stop();
	}

	public void move(double x, double y, double rot){
		if(Math.abs(x)<0.15) x=0;
		if(Math.abs(y)<0.15) y=0;
		if(Math.abs(rot)<0.15) rot=0;

		drivetrain.move(x, y, rot);
	}
}
