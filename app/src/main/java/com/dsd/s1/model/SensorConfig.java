package com.dsd.s1.model;

public class SensorConfig {
    public final String macAddress;
    public final String characteristicUuid;

    public SensorConfig(String macAddress, String characteristicUuid) {
        this.macAddress = macAddress;
        this.characteristicUuid = characteristicUuid;
    }
}