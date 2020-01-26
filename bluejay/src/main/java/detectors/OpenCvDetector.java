package detectors;

import android.content.Context;
import detectors.FoundationPipeline.Foundation;
import detectors.FoundationPipeline.Pipeline;
import detectors.FoundationPipeline.Skystone;
import detectors.FoundationPipeline.Stone;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.openftc.easyopencv.*;
import org.openftc.opencvrepackaged.DynamicOpenCvNativeLibLoader;

public class OpenCvDetector extends StartStoppable {
	
	//Originally in RobotControllerActivity, but caused the camera shutter to make weird noises, so now it lives here
	static {
//		DynamicOpenCvNativeLibLoader.loadNativeLibOnStartRobot();
//		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	//This is a reference to the camera
	private OpenCvCamera phoneCam;
	
	public OpenCvDetector() {
		
		//init EOCV
//		int cameraMonitorViewId = appContext.getResources().getIdentifier("cameraMonitorViewId", "id",
//				appContext.getPackageName());
//
		phoneCam = OpenCvCameraFactory.getInstance()
				           .createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK);
		
		Pipeline.doFoundations = false;
		Pipeline.doStones = false;
		Pipeline.doSkyStones = true;
		
		phoneCam.setPipeline(new OpenCvPipeline() {
			@Override
			public Mat processFrame(Mat input) {
				return Pipeline.process(input);
			}
		});
		
		phoneCam.openCameraDevice();
	}
	
	@Override
	public void loop() {
	
	}
	
	@Override
	public void begin() {
		phoneCam.startStreaming(640, 480, OpenCvCameraRotation.UPRIGHT);
	}
	
	@Override
	public void end() {
		phoneCam.stopStreaming();
		phoneCam.closeCameraDevice();
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
	
	public Skystone[] getSkyStones() {
		
		return Pipeline.skystones.toArray(new Skystone[0]);
	}
}
