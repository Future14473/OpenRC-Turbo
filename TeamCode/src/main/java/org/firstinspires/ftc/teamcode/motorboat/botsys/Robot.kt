package org.firstinspires.ftc.teamcode.motorboat.botsys

class Robot(hardwareMap: BotMap) {
    @JvmField
    var drivetrain = Drivetrain(
        hardwareMap.frontLeft,
        hardwareMap.frontRight,
        hardwareMap.backLeft,
        hardwareMap.backRight
    )
}
