package com.dsd.s1.model;

public class SensorStatus {
    public boolean connected;
    public String errorMessage;
    public int totalSensors;
    public int connectedSensors;
    public int producingSensors;

    public SensorStatus(boolean connected, String errorMessage) {
        this.connected = connected;
        this.errorMessage = errorMessage;
        this.totalSensors = 0;
        this.connectedSensors = 0;
        this.producingSensors = 0;
    }

    public SensorStatus(boolean connected, String errorMessage, int totalSensors, int connectedSensors, int producingSensors) {
        this.connected = connected;
        this.errorMessage = errorMessage;
        this.totalSensors = totalSensors;
        this.connectedSensors = connectedSensors;
        this.producingSensors = producingSensors;
    }
}
