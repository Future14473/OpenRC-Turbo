package org.firstinspires.ftc.teamcode.motorboat.botsys;

public interface SpinMech {
	void open();

	void close();

	void spinOpen();

	void spinClose();

	void stop();

	void setPosition(float pos);

	float getPos();
}
