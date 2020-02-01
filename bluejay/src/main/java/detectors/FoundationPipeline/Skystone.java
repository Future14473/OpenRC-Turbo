package detectors.FoundationPipeline;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.Collections;

public class SkyStone extends Stone implements Comparable<SkyStone> {

	public SkyStone(MatOfPoint shape) {
		super(shape);
		isBastard = length < 0.4 || Properlength > 25;

		if (size < 600) isBastard = true;

		RotatedRect rr = Imgproc.minAreaRect(compute.toDouble(shape));
		Size s = rr.size;
		double ratio = Imgproc.contourArea(shape) / (s.width * s.height);
		//System.out.println(ratio);
		if (ratio < 0.4) isBastard = true;
	}

	public void draw(Mat canvas) {
		Scalar color = new Scalar(0, 255, 0);
		Scalar black = new Scalar(0, 0, 0);

		Imgproc.drawContours(canvas, Collections.singletonList(shape), 0, new Scalar(255, 0, 255), 4);
		Imgproc.putText(canvas, "SKYSTONE", new Point(x, y - 30), 0, 0.6, black, 7);
		Imgproc.putText(canvas, "SKYSTONE", new Point(x, y - 30), 0, 0.6, color, 2);
		Imgproc.circle(canvas, new Point(x, y), 4, new Scalar(255, 255, 255), -1);
	}

	@Override
	public int compareTo(SkyStone o) {
		return Double.compare(o.size, size);
	}
}
