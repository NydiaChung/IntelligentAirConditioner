package com.newland.airconditionercontroller.bean;

public class DeviceInfo {
    public static String deviceId = "12734";
    public static String apiTagUpperLimit = "upperLimit";
    public static String apiTagLowerLimit = "lowerLimit";
    public static String apiTagCurrentTemp = "currentTemp";
    public static String apiTagAlarm = "alarm";
    public static String apiTagPower = "power";
    public static String apiTagUpperLimitCtrl = "upperLimitCtrl";
    public static String apiTagLowerLimitCtrl = "lowerLimitCtrl";
    public static String apiTagPowerCtrl = "powerCtrl";
    public static int openPowerValue = 1;
    public static int closePowerValue = 0;
    public static int alarmNomalValue = 0;
    public static int alarmUnNomalValue = 1;

    private String value;
    private String recordTime;

    public DeviceInfo(String value, String recordTime) {
        this.value = value;
        this.recordTime = recordTime;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(String recordTime) {
        this.recordTime = recordTime;
    }
}
