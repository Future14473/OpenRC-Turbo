package org.firstinspires.ftc.teamcode.original.utils;

import com.qualcomm.robotcore.hardware.Servo;

import java.util.Arrays;
import java.util.List;

public class Calc {
	public static Servo[] removeNulls(Servo[] stuff) {
		List<Servo> inp;
		inp = Arrays.asList(stuff);

		int i = 0;
		for (Object o : inp) {
			if (o == null) {
				inp.remove(i);
			} else {
				i++;
			}
		}

		stuff = inp.toArray(new Servo[0]);
		return stuff;
	}

	public static boolean delay(Long time) {
		long start = System.currentTimeMillis();
		while ((System.currentTimeMillis() - start) < time) {
			//wait
		}
		return true;
	}
}
