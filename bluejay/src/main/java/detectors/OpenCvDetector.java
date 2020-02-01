package detectors;

import android.content.Context;
import android.util.Log;
import detectors.FoundationPipeline.Foundation;
import detectors.FoundationPipeline.Pipeline;
import detectors.FoundationPipeline.SkyStone;
import detectors.FoundationPipeline.Stone;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.openftc.easyopencv.*;

import java.util.List;

public class OpenCvDetector extends StartStoppable implements AutoCloseable {

	//Originally in RobotControllerActivity, but caused the camera shutter to make weird noises, so now it lives here
	static {
		//DynamicOpenCvNativeLibLoader.loadNativeLibOnStartRobot();
		//System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	//This is a reference to the camera
	private OpenCvCamera phoneCam;

	public OpenCvDetector(Context appContext) {
		//OpMode

		//init EOCV
		int cameraMonitorViewId = appContext.getResources().getIdentifier("cameraMonitorViewId",
				"id", appContext.getPackageName());
		phoneCam = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK,
				cameraMonitorViewId);

		Pipeline.doFoundations = false;
		Pipeline.doStones = false;
		Pipeline.doSkyStones = true;

		phoneCam.setPipeline(new OpenCvPipeline() {
			@Override
			public Mat processFrame(Mat input) {
				Log.d("CV", "RUN_________________");
				return Pipeline.process(input);
			}
		});

		phoneCam.openCameraDevice();
	}

	@Override
	public void close() {
		end();
	}

	//will be called when detector is activated
	@Override
	public void begin() {
		Log.d("CV", "BEGIN_________________");
		phoneCam.startStreaming(640, 480, OpenCvCameraRotation.UPRIGHT);
	}

	//will be called when detector is ended
	@Override
	public void end() {
		phoneCam.stopStreaming();
		phoneCam.closeCameraDevice();
	}

	@Override
	public void loop() {
		//will be called repeatedly when detector is active

	}

	/*
	 * hold the phone sideways w/ camera on right
	 * x: 0 at the top, increases as you go down
	 * y: 0 at the right, increases as you go left
	 */

	public Foundation[] getFoundations() {

		return Pipeline.foundations.toArray(new Foundation[0]);
	}

	public Stone[] getStones() {

		return Pipeline.stones.toArray(new Stone[0]);
	}

	public SkyStone[] getSkyStones() {

		return Pipeline.skyStones.toArray(new SkyStone[0]);
	}

	public Point[] getSkyStonesPoint() {

		List<SkyStone> stones = Pipeline.skyStones;
		Point[] points = new Point[stones.size()];

		int i = 0;
		for (SkyStone s : stones) {
			points[i] = new Point(s.x, s.y);
			i++;
		}
		return points;
	}
}
