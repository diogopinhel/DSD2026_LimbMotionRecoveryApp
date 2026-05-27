package com.dsd.s1.model;

public final class SensorSample {
    public final long timestamp;
    public final String deviceId;
    public final String deviceName;
    public final float accX, accY, accZ;
    public final float gyroX, gyroY, gyroZ;
    public final float roll, pitch, yaw;

    public SensorSample(long timestamp, String deviceId, String deviceName,
                        float accX, float accY, float accZ,
                        float gyroX, float gyroY, float gyroZ,
                        float roll, float pitch, float yaw) {
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
    }
}