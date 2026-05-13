package com.dsd.s1.util;

import android.content.Context;
import com.dsd.s1.model.SensorSample;
import java.io.*;
import java.util.List;

public class CsvWriter {
    private final File file;
    private boolean headerWritten = false;
    private static final String[] HEADERS = {
            "timestamp", "deviceId", "deviceName",
            "accX", "accY", "accZ", "gyroX", "gyroY", "gyroZ",
            "roll", "pitch", "yaw"
    };

    private static String joinCsv(String[] cols) {
        if (cols == null || cols.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(cols[i] == null ? "" : cols[i]);
        }
        return sb.toString();
    }

    public CsvWriter(Context context, String filename) throws IOException {
        file = new File(context.getExternalFilesDir(null), filename);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        headerWritten = file.exists() && file.length() > 0;
    }

    public void writeSamples(List<SensorSample> samples) throws IOException {
        if (samples.isEmpty()) return;
        try (FileWriter fw = new FileWriter(file, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter writer = new PrintWriter(bw)) {
            if (!headerWritten) {
                writer.println(joinCsv(HEADERS));
                headerWritten = true;
            }
            for (SensorSample sample : samples) {
                writer.println(joinCsv(sample.toCsvRow()));
            }
        }
    }

    public String getPath() {
        return file.getAbsolutePath();
    }
}
