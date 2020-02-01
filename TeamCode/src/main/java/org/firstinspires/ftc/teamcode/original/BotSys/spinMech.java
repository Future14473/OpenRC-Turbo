package org.firstinspires.ftc.teamcode.original.BotSys;

public interface spinMech {
	public void open();
	public void close();

	public void spinOpen();
	public void spinClose();

	public void stop();

	public void setPosition(float pos);

	public void setPower(float pwr);

	public float getPos( );
}
