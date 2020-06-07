package org.btelman.spherosdk.android.enums.sensor

/**
 * Created by Brendon on 2/15/2020.
 */
object Commands {
    val ENABLE_GYRO_MAX_NOTIFY : Byte = 0X0F
    val GYRO_MAX_NOTIFY : Byte = 0X10
    val RESET_LOCATOR_X_AND_Y : Byte = 0X13
    val SET_LOCATOR_FLAGS : Byte = 0X17
    val GET_BOT_TO_BOT_INFRARED_READINGS : Byte = 0X22
    val GET_RGBC_SENSOR_VALUES : Byte = 0X23
    val START_ROBOT_TO_ROBOT_INFRARED_BROADCASTING : Byte = 0X27
    val START_ROBOT_TO_ROBOT_INFRARED_FOLLOWING : Byte = 0X28
    val STOP_ROBOT_TO_ROBOT_INFRARED_BROADCASTING : Byte = 0X29
    val ROBOT_TO_ROBOT_INFRARED_MESSAGE_RECEIVED_NOTIFY : Byte = 0X2C
    val GET_AMBIENT_LIGHT_SENSOR_VALUE : Byte = 0X30
    val STOP_ROBOT_TO_ROBOT_INFRARED_FOLLOWING : Byte = 0X32
    val START_ROBOT_TO_ROBOT_INFRARED_EVADING : Byte = 0X33
    val STOP_ROBOT_TO_ROBOT_INFRARED_EVADING : Byte = 0X34
    val ENABLE_COLOR_DETECTION_NOTIFY : Byte = 0X35
    val COLOR_DETECTION_NOTIFY : Byte = 0X36
    val GET_CURRENT_DETECTED_COLOR_READING : Byte = 0X37
    val ENABLE_COLOR_DETECTION : Byte = 0X38
    val CONFIGURE_STREAMING_SERVICE : Byte = 0X39
    val START_STREAMING_SERVICE : Byte = 0X3A
    val STOP_STREAMING_SERVICE : Byte = 0X3B
    val CLEAR_STREAMING_SERVICE : Byte = 0X3C
    val STREAMING_SERVICE_DATA_NOTIFY : Byte = 0X3D
    val ENABLE_ROBOT_INFRARED_MESSAGE_NOTIFY : Byte = 0X3E
    val SEND_INFRARED_MESSAGE : Byte = 0X3F
    val GET_MOTOR_TEMPERATURE : Byte = 0X42
    val GET_MOTOR_THERMAL_PROTECTION_STATUS : Byte = 0X4B
    val ENABLE_MOTOR_THERMAL_PROTECTION_STATUS_NOTIFY : Byte = 0X4C
    val MOTOR_THERMAL_PROTECTION_STATUS_NOTIFY : Byte = 0X4D
}