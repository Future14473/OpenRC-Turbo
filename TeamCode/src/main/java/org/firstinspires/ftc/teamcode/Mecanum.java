package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.ArrayList;
import java.util.List;

public class Mecanum {
    double topLeftPower;
    double topRightPower;
    double bottomLeftPower;
    double bottomRightPower;
    double f;
    double s;
    DcMotorEx topRightDrive;
    DcMotorEx topLeftDrive;
    DcMotorEx bottomRightDrive;
    DcMotorEx bottomLeftDrive;


    public Mecanum(){

    }

    public Mecanum(HardwareMap hardwareMap ){
        topLeftDrive  = hardwareMap.get(DcMotorEx.class, "top_left_drive");
        topRightDrive = hardwareMap.get(DcMotorEx.class, "top_right_drive");
        bottomLeftDrive = hardwareMap.get(DcMotorEx.class, "bottom_left_drive");
        bottomRightDrive = hardwareMap.get(DcMotorEx.class, "bottom_right_drive");

        topLeftDrive.setDirection(DcMotor.Direction.FORWARD);
        bottomLeftDrive.setDirection(DcMotor.Direction.FORWARD);
        topRightDrive.setDirection(DcMotor.Direction.REVERSE);
        bottomRightDrive.setDirection(DcMotor.Direction.REVERSE);
    }


    public void set_motor_values(double F, double S, double turn){
        topRightPower = F - S + turn;
        bottomRightPower = F + S + turn;
        topLeftPower = F + S - turn;
        bottomLeftPower = F - S - turn;
    }

    public void set_forward_strafe(double angle){
        angle = Math.toRadians(angle);
        f = Math.sin(angle);
        s = Math.cos(angle);
    }

    public void move(double f, double s, double turn){
//        set_forward_strafe(angle);
        set_motor_values(f, s, turn);
        topRightDrive.setVelocity(topRightPower);
        topLeftDrive.setVelocity(topLeftPower);
        bottomLeftDrive.setVelocity(bottomLeftPower);
        bottomRightDrive.setVelocity(bottomRightPower);

    }
}
