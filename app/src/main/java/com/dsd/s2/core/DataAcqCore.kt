package com.dsd.s2.core

import android.content.Context
import android.util.Log
import com.dsd.s1.model.SensorSample
import com.dsd.s1.model.SensorStatus
import com.dsd.s2.model.ErrorEvent
import com.dsd.s2.model.TargetAngle
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "DataAcqCore"
private const val POLL_INTERVAL_MS = 20L

class DataAcqCore(
    private val dataSource: S1DataSource,
    sensorJointMapping: Map<String, String>,
    bindMode: String = BINDMODE_BACK,
    private val onSamples: ((List<SensorSample>) -> Unit)? = null,
    private val onAngles: ((List<TargetAngle>) -> Unit)? = null,
    private val onError: ((ErrorEvent) -> Unit)? = null,
    private val context: Context? = null
) {
    private val jointPairs = parseJointPairs(sensorJointMapping)
    private val angleComputer = JointAngleComputer(jointPairs, bindMode)

    private var scope: CoroutineScope? = null
    private var job: Job? = null

    var sampleCount: Int = 0
        private set
    var errorCount: Int = 0
        private set

    private var logWriter: FileWriter? = null
    private var logPath: String? = null

    init {
        initAngleLog()
    }

    private fun initAngleLog() {
        try {
            val dir = if (context != null) {
                File(context.getExternalFilesDir(null), "log").also { it.mkdirs() }
            } else {
                File("log").also { it.mkdirs() }
            }
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "angles_$ts.csv")
            logWriter = FileWriter(file, true)
            logWriter?.write("timestamp_ms,timestamp_iso,angleID,angle_deg\n")
            logWriter?.flush()
            logPath = file.absolutePath
            Log.i(TAG, "Angle log file: $logPath")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to init angle log: ${e.message}")
        }
    }

    private fun logAngles(angles: List<TargetAngle>) {
        val writer = logWriter ?: return
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            for (a in angles) {
                val iso = sdf.format(Date(a.timestamp))
                writer.write("${a.timestamp},$iso,${a.angleID},${a.angle}\n")
            }
            writer.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write angle log: ${e.message}")
        }
    }

    private fun closeAngleLog() {
        try {
            logWriter?.close()
            logWriter = null
            Log.i(TAG, "Angle log closed: $logPath")
        } catch (_: Exception) {}
    }

    fun start() {
        if (job?.isActive == true) return
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        job = scope!!.launch {
            while (isActive) {
                try {
                    pollOnce()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error in poll loop", e)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        scope?.cancel()
        job = null
        scope = null
        closeAngleLog()
    }

    private fun pollOnce() {
        val status: SensorStatus = dataSource.status()
        if (!status.connected && status.errorMessage != null) {
            onError?.invoke(ErrorEvent(
                timestamp = System.currentTimeMillis(),
                sensorId = null,
                errorType = status.errorMessage,
                message = "Sensor status: ${status.errorMessage}"
            ))
        }

        val rawSamples: List<SensorSample> = dataSource.read()
        if (rawSamples.isEmpty()) return

        val validSamples = mutableListOf<SensorSample>()
        for (sample in rawSamples) {
            val err = validateSample(sample)
            if (err == null) {
                validSamples.add(sample)
                sampleCount++
            } else {
                errorCount++
                onError?.invoke(ErrorEvent(
                    timestamp = sample.timestamp,
                    sensorId = sample.deviceId,
                    errorType = "validation_failure",
                    message = err
                ))
            }
        }

        if (validSamples.isNotEmpty()) {
            onSamples?.invoke(validSamples)
        }

        val angles = angleComputer.feedSamples(validSamples)
        if (angles.isNotEmpty()) {
            logAngles(angles)
            onAngles?.invoke(angles)
        }
    }

    companion object {
        fun validateSample(sample: SensorSample): String? {
            val fields = listOf(
                "accX" to sample.accX, "accY" to sample.accY, "accZ" to sample.accZ,
                "gyroX" to sample.gyroX, "gyroY" to sample.gyroY, "gyroZ" to sample.gyroZ,
                "roll" to sample.roll, "pitch" to sample.pitch, "yaw" to sample.yaw
            )
            for ((name, value) in fields) {
                if (!value.isFinite()) return "Non-finite value in $name: $value"
            }
            if (sample.timestamp <= 0) return "Invalid timestamp: ${sample.timestamp}"
            return null
        }
    }
}
