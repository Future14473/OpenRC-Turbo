package org.firstinspires.ftc.teamcode.original.BotSys;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

public class imu {
	private final BNO055IMU imu;
	float startHeading = 0;

	public imu(map hardwareMap){
		// Set up the parameters with which we will use our IMU. Note that integration
		// algorithm here just reports accelerations to the logcat log; it doesn't actually
		// provide positional information.
		BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
		parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
		parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
		//parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
		parameters.loggingEnabled      = true;
		parameters.loggingTag          = "IMU";
		parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

		imu = hardwareMap.imu;
		imu.initialize(parameters);

		startHeading = heading();
	}

	public float heading (){
		Orientation angles = imu.getAngularOrientation(AxesReference.EXTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
		return angles.secondAngle - startHeading;
	}

	public void resetHeading(int heading){
		startHeading -= (heading() - heading);
	}
}
