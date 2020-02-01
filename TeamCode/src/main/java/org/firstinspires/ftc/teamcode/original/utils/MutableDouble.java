package org.firstinspires.ftc.teamcode.original.utils;

public class MutableDouble {
	private double me = 0;

	public MutableDouble(double d) {
		me = d;
	}

	public void increment(double d) {
		me += d;
	}

	public double get() {
		return me;
	}

	public void set(double d) {
		me = d;
	}

	public int getInt() {
		return (int) me;
	}
}
