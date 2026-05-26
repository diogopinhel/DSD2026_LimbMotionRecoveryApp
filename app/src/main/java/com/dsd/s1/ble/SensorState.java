package com.dsd.s1.ble;

import com.dsd.s1.model.SensorSample;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SensorState {
    private volatile boolean connected = false;
    private volatile boolean producing = false;
    private volatile String errorMessage = null;
    private final Queue<SensorSample> sampleQueue = new ConcurrentLinkedQueue<>();

    public synchronized void setConnected(boolean connected) {
        this.connected = connected;
        if (!connected) {
            this.producing = false;
            this.errorMessage = "sensor_disconnected";
        } else {
            if ("sensor_disconnected".equals(this.errorMessage)) {
                this.errorMessage = null;
            }
        }
    }

    public synchronized void markProducing() {
        this.producing = true;
        if ("timeout".equals(errorMessage) || "sensor_disconnected".equals(errorMessage)) {
            errorMessage = null;
        }
    }

    public synchronized void markTimeout() {
        this.producing = false;
        if (connected) this.errorMessage = "timeout";
    }

    public synchronized void markDataCorruption() {
        this.errorMessage = "data_corruption";
    }

    public synchronized void clearErrorIfAny() {
        if ("data_corruption".equals(errorMessage)) errorMessage = null;
    }

    public synchronized void setErrorMessage(String msg) {
        this.errorMessage = msg;
    }

    public synchronized boolean isConnected() { return connected; }

    public synchronized Snapshot snapshot() {
        return new Snapshot(connected, producing, errorMessage);
    }

    public void addSample(SensorSample sample) {
        sampleQueue.offer(sample);
    }

    public List<SensorSample> drainSamples() {
        List<SensorSample> samples = new ArrayList<>();
        SensorSample sample;
        while ((sample = sampleQueue.poll()) != null) {
            samples.add(sample);
        }
        samples.sort(Comparator.comparingLong(a -> a.timestamp));
        return samples;
    }

    static class Snapshot {
        public final boolean connected;
        public final boolean producing;
        public final String errorMessage;
        Snapshot(boolean connected, boolean producing, String errorMessage) {
            this.connected = connected;
            this.producing = producing;
            this.errorMessage = errorMessage;
        }
    }
}
