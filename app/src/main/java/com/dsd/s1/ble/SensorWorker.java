package com.dsd.s1.ble;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import com.dsd.s1.model.SensorConfig;
import com.dsd.s1.model.ServiceConfig;
import java.util.List;
import java.util.UUID;

public class SensorWorker {
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final SensorConfig config;
    private final SensorState state;
    private final ServiceConfig serviceCfg;
    private final WitMotionParser parser;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private BluetoothGatt gatt;
    private volatile boolean shouldStop = false;
    private volatile long lastPacketTime = 0;
    private Runnable timeoutChecker;
    private final Runnable reconnectRunnable = this::connect;

    public SensorWorker(Context context, SensorConfig config, SensorState state, ServiceConfig serviceCfg) {
        this.context = context;
        this.config = config;
        this.state = state;
        this.serviceCfg = serviceCfg;
        this.parser = new WitMotionParser();
    }

    public void stop() {
        shouldStop = true;
        handler.removeCallbacks(reconnectRunnable);
        if (timeoutChecker != null) handler.removeCallbacks(timeoutChecker);
        state.setConnected(false);
        closeGattSafely();
    }

    private void closeGattSafely() {
        if (gatt != null) {
            try {
                if (hasBluetoothConnectPermission()) {
                    gatt.disconnect();
                    gatt.close();
                } else {
                    try { gatt.close(); } catch (Exception ignored) {}
                }
            } catch (SecurityException e) {
                try { gatt.close(); } catch (Exception ignored) {}
            } catch (Exception e) {
                // ignore
            }
            gatt = null;
        }
    }

    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean hasBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void start() {
        shouldStop = false;
        handler.removeCallbacks(reconnectRunnable);
        if (timeoutChecker != null) handler.removeCallbacks(timeoutChecker);
        state.setConnected(false);
        connect();
    }

    private void connect() {
        if (shouldStop) return;
        closeGattSafely();

        if (!hasBluetoothConnectPermission() || !hasBluetoothScanPermission()) {
            state.setConnected(false);
            state.setErrorMessage("no_ble_permission");
            scheduleReconnect();
            return;
        }

        try {
            BluetoothDevice device = BluetoothUtil.getDeviceByAddress(context, config.deviceAddress);
            if (device == null) {
                state.setConnected(false);
                state.setErrorMessage("invalid_device_address");
                scheduleReconnect();
                return;
            }
            gatt = device.connectGatt(context, false, new GattCallback());
        } catch (SecurityException e) {
            state.setConnected(false);
            state.setErrorMessage("no_ble_permission");
            scheduleReconnect();
        } catch (Exception e) {
            state.setConnected(false);
            state.setErrorMessage("connect_failed");
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!shouldStop) {
            handler.removeCallbacks(reconnectRunnable);
            handler.postDelayed(reconnectRunnable, (long)(serviceCfg.reconnectDelaySec * 1000));
        }
    }

    private BluetoothGattCharacteristic findCharacteristic(BluetoothGatt gatt, UUID characteristicUuid) {
        if (gatt == null) return null;
        if (!hasBluetoothConnectPermission()) return null;
        List<BluetoothGattService> services;
        try {
            services = gatt.getServices();
        } catch (SecurityException e) {
            return null;
        }
        if (services == null) return null;
        for (BluetoothGattService service : services) {
            if (service == null) continue;
            BluetoothGattCharacteristic c = service.getCharacteristic(characteristicUuid);
            if (c != null) return c;
        }
        return null;
    }

    private void startTimeoutChecker() {
        if (timeoutChecker != null) handler.removeCallbacks(timeoutChecker);
        timeoutChecker = () -> {
            if (!shouldStop && state.isConnected()) {
                long delta = System.currentTimeMillis() - lastPacketTime;
                if (delta >= serviceCfg.noDataTimeoutSec * 1000) {
                    state.markTimeout();
                }
            }
            if (!shouldStop) handler.postDelayed(timeoutChecker, 500);
        };
        handler.post(timeoutChecker);
    }

    private class GattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                try {
                    if (hasBluetoothConnectPermission()) {
                        gatt.discoverServices();
                    } else {
                        state.setConnected(false);
                    }
                } catch (SecurityException e) {
                    state.setConnected(false);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                state.setConnected(false);
                closeGattSafely();
                scheduleReconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    if (!hasBluetoothConnectPermission()) {
                        state.setConnected(false);
                        return;
                    }
                    UUID charUuid = UUID.fromString(config.notifyCharUuid);
                    BluetoothGattCharacteristic characteristic = findCharacteristic(gatt, charUuid);
                    if (characteristic == null) {
                        state.setConnected(false);
                        state.setErrorMessage("notify_char_not_found");
                        closeGattSafely();
                        scheduleReconnect();
                        return;
                    }
                    boolean notifySet = gatt.setCharacteristicNotification(characteristic, true);
                    if (!notifySet) {
                        state.setConnected(false);
                        state.setErrorMessage("enable_notify_failed");
                        closeGattSafely();
                        scheduleReconnect();
                        return;
                    }
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        try {
                            gatt.writeDescriptor(descriptor);
                        } catch (SecurityException e) {
                            state.setConnected(false);
                            state.setErrorMessage("no_ble_permission");
                            closeGattSafely();
                            scheduleReconnect();
                            return;
                        }
                    }
                    state.setConnected(true);
                    lastPacketTime = System.currentTimeMillis();
                    startTimeoutChecker();
                } catch (SecurityException e) {
                    state.setConnected(false);
                    state.setErrorMessage("no_ble_permission");
                } catch (Exception e) {
                    state.setConnected(false);
                    state.setErrorMessage("service_discovery_failed");
                }
            } else {
                state.setConnected(false);
                state.setErrorMessage("service_discovery_failed");
                closeGattSafely();
                scheduleReconnect();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            lastPacketTime = System.currentTimeMillis();
            byte[] data = characteristic.getValue();
            WitMotionParser.ParseResult result = parser.feed(data, config.deviceAddress, config.deviceName);
            if (result.corruptionCount > 0) state.markDataCorruption();
            if (!result.samples.isEmpty()) {
                state.markProducing();
                state.clearErrorIfAny();
                for (com.dsd.s1.model.SensorSample sample : result.samples) {
                    if (isSampleFinite(sample)) state.addSample(sample);
                    else state.markDataCorruption();
                }
            }
        }

        private boolean isSampleFinite(com.dsd.s1.model.SensorSample s) {
            return !Float.isNaN(s.accX) && !Float.isInfinite(s.accX) &&
                    !Float.isNaN(s.accY) && !Float.isInfinite(s.accY) &&
                    !Float.isNaN(s.accZ) && !Float.isInfinite(s.accZ);
        }
    }
}
