package org.firstinspires.ftc.teamcode.motorboat;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import detectors.OpenCvDetector;

/*

    If you're using this library, THANKS! I spent a lot of time on it.

    However, stuff isn't as well-documented as I like...still working on that

    So if you have questions, email me at xchenbox@gmail.com and I will get back to you in about a day (usually)

    Enjoy!

    Below is the code to display to the RC; Thanks, EasyOpenCV!
	If it crashes after about a minute, it's probably because OpenCV is using too much native memory.
	That should not happen much because the MatAllocator recycles all Mats (but not MatofInt)
 */

@TeleOp(name = "CV test", group = "Auto")
class OpenCVDetection extends OpMode {

	private OpenCvDetector fieldElementDetector;

	@Override
	public void init() {
		fieldElementDetector = new OpenCvDetector(this.hardwareMap.appContext);
	}

	/*
	 * Code to run REPEATEDLY when the driver hits INIT
	 */
	@Override
	public void init_loop() {
	}

	/*
	 * Code to run ONCE when the driver hits PLAY
	 */
	@Override
	public void start() {
		fieldElementDetector.start();
	}

	/*
	 * Code to run REPEATEDLY when the driver hits PLAY
	 */
	@Override
	public void loop() {

	}

	/*
	 * Code to run ONCE after the driver hits STOP
	 */
	@Override
	public void stop() {
		fieldElementDetector.stop();
	}
}
