package com.dsd.s1.model;

public class SensorConfig {
    public String deviceAddress;
    public String notifyCharUuid;
    public String deviceName;

    public SensorConfig(String deviceAddress, String notifyCharUuid) {
        this.deviceAddress = deviceAddress;
        this.notifyCharUuid = notifyCharUuid;
        this.deviceName = "WitMotion";
    }
}
