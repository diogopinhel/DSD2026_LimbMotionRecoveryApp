package com.dsd.s1.ble;

import android.content.Context;
import com.dsd.s1.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SensorService {
    private final Context appContext;
    private final Map<String, SensorState> states = new ConcurrentHashMap<>();
    private final List<SensorWorker> workers = new ArrayList<>();
    private volatile boolean running = false;

    public SensorService(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void initialize(List<SensorConfig> sensors, ServiceConfig serviceConfig) {
        for (SensorConfig config : sensors) {
            SensorState state = new SensorState();
            states.put(config.deviceAddress, state);
            workers.add(new SensorWorker(appContext, config, state, serviceConfig));
        }
    }

    public void startSensors() {
        if (running) return;
        running = true;
        for (SensorWorker worker : workers) {
            worker.start();
        }
    }

    public void stopSensors() {
        if (!running) return;
        running = false;
        for (SensorWorker worker : workers) {
            worker.stop();
        }
    }

    public List<SensorSample> readSamples() {
        List<SensorSample> allSamples = new ArrayList<>();
        for (Map.Entry<String, SensorState> entry : states.entrySet()) {
            allSamples.addAll(entry.getValue().drainSamples());
        }
        allSamples.sort(Comparator.comparingLong(a -> a.timestamp));
        return allSamples;
    }

    public SensorStatus getStatus() {
        boolean allConnected = true;
        boolean allProducing = true;
        List<String> errors = new ArrayList<>();
        int total = states.size();
        int connectedCount = 0;
        int producingCount = 0;

        for (SensorState state : states.values()) {
            SensorState.Snapshot snap = state.snapshot();
            allConnected = allConnected && snap.connected;
            allProducing = allProducing && snap.producing;
            if (snap.connected) connectedCount++;
            if (snap.producing) producingCount++;
            if (snap.errorMessage != null) errors.add(snap.errorMessage);
        }

        boolean connected = allConnected && allProducing;
        String error = null;
        if (!errors.isEmpty()) {
            Map<String, Integer> priority = new HashMap<>();
            priority.put("sensor_disconnected", 0);
            priority.put("timeout", 1);
            priority.put("data_corruption", 2);
            error = errors.stream().min((a, b) ->
                    priority.getOrDefault(a, 99) - priority.getOrDefault(b, 99)).orElse(null);
        }

        return new SensorStatus(connected, error, total, connectedCount, producingCount);
    }
}
