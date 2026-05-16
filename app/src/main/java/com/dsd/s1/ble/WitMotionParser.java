package com.dsd.s1.ble;

import com.dsd.s1.model.SensorSample;
import java.util.*;

public class claWitMotionParser {
    private static final int SERIAL_FRAME_LEN = 11;
    private static final int BLE_PACKET_LEN = 20;
    private static final int MAX_RX_BUFFER = 4096;

    private final List<Byte> rxBuffer = new ArrayList<>();
    private float[] acc = null;
    private float[] gyro = null;
    private float[] angle = null;

    public ParseResult feed(byte[] payload, String deviceId, String deviceName) {
        List<SensorSample> samples = new ArrayList<>();
        int corruptionCount = 0;

        for (byte b : payload) rxBuffer.add(b);
        trimRxIfNeeded();

        while (true) {
            if (rxBuffer.size() < 2) break;

            int start = findHeader();
            if (start < 0) {
                corruptionCount += rxBuffer.size();
                rxBuffer.clear();
                break;
            }
            if (start > 0) {
                corruptionCount += start;
                for (int i = 0; i < start; i++) rxBuffer.remove(0);
            }

            int frameType = rxBuffer.get(1) & 0xFF;
            if (frameType == 0x61) {
                if (rxBuffer.size() < BLE_PACKET_LEN) break;
                byte[] packet = new byte[BLE_PACKET_LEN];
                for (int i = 0; i < BLE_PACKET_LEN; i++) packet[i] = rxBuffer.get(i);
                SensorSample sample = decodeBleImuPacket(packet, deviceId, deviceName);
                if (sample == null) {
                    corruptionCount++;
                    rxBuffer.remove(0);
                    continue;
                }
                samples.add(sample);
                for (int i = 0; i < BLE_PACKET_LEN; i++) rxBuffer.remove(0);
                continue;
            }

            if (rxBuffer.size() < SERIAL_FRAME_LEN) break;
            if (!isStandardFrameType(frameType)) {
                corruptionCount++;
                rxBuffer.remove(0);
                continue;
            }

            byte[] frame = new byte[SERIAL_FRAME_LEN];
            for (int i = 0; i < SERIAL_FRAME_LEN; i++) frame[i] = rxBuffer.get(i);

            int expected = 0;
            for (int i = 0; i < 10; i++) expected += (frame[i] & 0xFF);
            expected &= 0xFF;

            if ((frame[10] & 0xFF) != expected) {
                corruptionCount++;
                int nextHeader = findHeaderFrom(1, SERIAL_FRAME_LEN);
                if (nextHeader > 0) {
                    for (int i = 0; i < nextHeader; i++) rxBuffer.remove(0);
                } else {
                    rxBuffer.remove(0);
                }
                continue;
            }

            decodeFrame(frame);
            SensorSample sample = buildSampleIfReady(deviceId, deviceName);
            if (sample != null) samples.add(sample);
            for (int i = 0; i < SERIAL_FRAME_LEN; i++) rxBuffer.remove(0);
        }

        return new ParseResult(samples, corruptionCount);
    }

    private int findHeader() {
        for (int i = 0; i < rxBuffer.size(); i++) {
            if ((rxBuffer.get(i) & 0xFF) == 0x55) return i;
        }
        return -1;
    }

    private int findHeaderFrom(int start, int limit) {
        int end = Math.min(start + limit, rxBuffer.size());
        for (int i = start; i < end; i++) {
            if ((rxBuffer.get(i) & 0xFF) == 0x55) return i;
        }
        return -1;
    }

    private void trimRxIfNeeded() {
        if (rxBuffer.size() <= MAX_RX_BUFFER) return;
        int start = -1;
        for (int i = rxBuffer.size() - 1; i >= 0; i--) {
            if ((rxBuffer.get(i) & 0xFF) == 0x55) { start = i; break; }
        }
        if (start < 0) rxBuffer.clear();
        else {
            for (int i = 0; i < start; i++) rxBuffer.remove(0);
            while (rxBuffer.size() > MAX_RX_BUFFER) rxBuffer.remove(rxBuffer.size() - 1);
        }
    }

    private boolean isStandardFrameType(int frameType) {
        return frameType >= 0x50 && frameType <= 0x5F;
    }

    private SensorSample decodeBleImuPacket(byte[] packet, String deviceId, String deviceName) {
        if (packet.length != BLE_PACKET_LEN || (packet[0] & 0xFF) != 0x55 || (packet[1] & 0xFF) != 0x61)
            return null;

        int[] values = new int[9];
        for (int i = 0; i < 9; i++) {
            values[i] = toInt16(packet[2 + i * 2], packet[3 + i * 2]);
        }

        return new SensorSample(
                System.currentTimeMillis(), deviceId, deviceName,
                values[0] / 32768.0f * 16.0f,
                values[1] / 32768.0f * 16.0f,
                values[2] / 32768.0f * 16.0f,
                values[3] / 32768.0f * 2000.0f,
                values[4] / 32768.0f * 2000.0f,
                values[5] / 32768.0f * 2000.0f,
                values[6] / 32768.0f * 180.0f,
                values[7] / 32768.0f * 180.0f,
                values[8] / 32768.0f * 180.0f
        );
    }

    private void decodeFrame(byte[] frame) {
        int type = frame[1] & 0xFF;
        int v0 = toInt16(frame[2], frame[3]);
        int v1 = toInt16(frame[4], frame[5]);
        int v2 = toInt16(frame[6], frame[7]);

        if (type == 0x51) {
            acc = new float[]{
                    v0 / 32768.0f * 16.0f,
                    v1 / 32768.0f * 16.0f,
                    v2 / 32768.0f * 16.0f
            };
        } else if (type == 0x52) {
            gyro = new float[]{
                    v0 / 32768.0f * 2000.0f,
                    v1 / 32768.0f * 2000.0f,
                    v2 / 32768.0f * 2000.0f
            };
        } else if (type == 0x53) {
            angle = new float[]{
                    v0 / 32768.0f * 180.0f,
                    v1 / 32768.0f * 180.0f,
                    v2 / 32768.0f * 180.0f
            };
        }
    }

    private SensorSample buildSampleIfReady(String deviceId, String deviceName) {
        if (acc == null || gyro == null || angle == null) return null;
        return new SensorSample(System.currentTimeMillis(), deviceId, deviceName,
                acc[0], acc[1], acc[2], gyro[0], gyro[1], gyro[2],
                angle[0], angle[1], angle[2]);
    }

    private int toInt16(byte low, byte high) {
        return (short)((low & 0xFF) | ((high & 0xFF) << 8));
    }

    static class ParseResult {
        public final List<SensorSample> samples;
        public final int corruptionCount;
        ParseResult(List<SensorSample> samples, int corruptionCount) {
            this.samples = samples;
            this.corruptionCount = corruptionCount;
        }
    }
}
