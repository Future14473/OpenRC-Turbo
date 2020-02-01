package org.firstinspires.ftc.teamcode.original.BotSys;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.original.MainTeleOp;

public class robot {
	//TODO
	public drivetrain drivetrain;

	public spinMech lift;
	boolean liftArrived = false;

	public spinMech extendoMagicko;
	spinMech intake;
	spinMech spin;
	public spinMech grab;
	spinMech gate;
	spinMech foundationL;
	spinMech foundationR;

	map hardwareMap;

	public robot (map hardwareMap) {
		this.hardwareMap = hardwareMap;
		drivetrain = new drivetrain(hardwareMap.frontLeft,
									hardwareMap.frontRight,
									hardwareMap.backLeft,
									hardwareMap.backRight);
		lift = new spinMechBig(hardwareMap.liftLeft, hardwareMap.liftRight);
		intake = new spinMechBig(hardwareMap.intakeLeft, hardwareMap.intakeRight);
		spin = new spinMechSmall(0,1,hardwareMap.spin);
		grab = new spinMechSmall(0,1,hardwareMap.grab);
		gate = new spinMechSmall(0,1,hardwareMap.gate);
		foundationL = new spinMechSmall(0,1,hardwareMap.foundationLeft);
		foundationR = new spinMechSmall(0,1,hardwareMap.foundationRight);

		//0.3 is out, 0.9 is in
		extendoMagicko = new spinMechSmall(0.0f,0.9f,hardwareMap.extendLeft,hardwareMap.extendRight);
		//TODO
	}

	public void moveToTarget(float target){
		hardwareMap.liftRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		hardwareMap.liftLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

		float leftDiff =Math.abs(-hardwareMap.liftLeft.getCurrentPosition()-target);
		if(leftDiff > 20) {

			if (hardwareMap.liftLeft.getCurrentPosition() > -target) {
				hardwareMap.liftLeft.setPower(leftDiff/700f+0.3);

			}
			if (hardwareMap.liftLeft.getCurrentPosition() < -target) {
				float intendedPwr = -(leftDiff/700f);
				hardwareMap.liftLeft.setPower(intendedPwr<-0.5?-0.5:intendedPwr);
			}
			liftArrived = false;
		}

		float rightDiff = Math.abs(hardwareMap.liftRight.getCurrentPosition()-target);
		if(rightDiff > 20){
			if (hardwareMap.liftRight.getCurrentPosition()<target){
				hardwareMap.liftRight.setPower(rightDiff/700f+0.3);

			}
			if (hardwareMap.liftRight.getCurrentPosition()>target){
				float intendedPwr = -(rightDiff/700f);
				hardwareMap.liftRight.setPower(intendedPwr<-0.5?-0.5:intendedPwr);
			}
			liftArrived = false;
		}
		if(leftDiff < 20 && rightDiff < 20) {
			hardwareMap.liftLeft.setPower(0);
			hardwareMap.liftRight.setPower(0);
			liftArrived = true;
		}

		MainTeleOp.telemetry.addData("liftleft diff",leftDiff);
		MainTeleOp.telemetry.addData("liftright diff",rightDiff);
	}

	public void liftStop(){
		MainTeleOp.telemetry.addData("lift","stop");
		lift.stop();
	}

	public void intakeIn(){
		intake.spinOpen();
	}

	public void intakeOut(){
		intake.spinClose();
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

	public void spinRest(){
		spin.setPosition(0.837f);
	}
	public void spinMiddle(){
		spin.setPosition(0.473f);
	}

	public void spinOut(){
		spin.setPosition(0.108f);
	}

	public void grabClose () {
		grab.setPosition(0.75f);
	}

	public void grabOpen () {
		grab.setPosition(0.35f);
	}

	public void gateClose() {
		gate.setPosition(1);
	}

	public void gateOpen() {
		gate.setPosition(0);
	}

	public void foundationRest() {
		foundationL.setPosition(0);
		foundationR.setPosition(1);
	}

	public void foundationEngage() {
		foundationL.setPosition(1);
		foundationR.setPosition(0);
	}

	public boolean liftArrived(){
		return liftArrived;
	}

	public void extend(){
		extendoMagicko.close();
	}
	public void deExtend(){
		extendoMagicko.open();
	}
}
