package com.dsd.s1.model;

public class SensorSample {
    public long timestamp;
    public String deviceId;
    public String deviceName;
    public float accX, accY, accZ;
    public float gyroX, gyroY, gyroZ;
    public float roll, pitch, yaw;

    public SensorSample(long timestamp, String deviceId, String deviceName,
                        float accX, float accY, float accZ,
                        float gyroX, float gyroY, float gyroZ,
                        float roll, float pitch, float yaw) {
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.accX = accX; this.accY = accY; this.accZ = accZ;
        this.gyroX = gyroX; this.gyroY = gyroY; this.gyroZ = gyroZ;
        this.roll = roll; this.pitch = pitch; this.yaw = yaw;
    }

    public String[] toCsvRow() {
        return new String[]{
                String.valueOf(timestamp), deviceId, deviceName,
                String.valueOf(accX), String.valueOf(accY), String.valueOf(accZ),
                String.valueOf(gyroX), String.valueOf(gyroY), String.valueOf(gyroZ),
                String.valueOf(roll), String.valueOf(pitch), String.valueOf(yaw)
        };
    }
}
