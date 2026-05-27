package com.dsd.s2.sim

import com.dsd.s1.model.SensorSample
import com.dsd.s1.model.SensorStatus
import com.dsd.s2.core.S1DataSource
import com.dsd.s2.core.S1Module
import com.dsd.s2.core.S1SessionControl
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.*
import kotlin.random.Random

class SimulatedSensorService(
    private val sensorIds: List<Map<String, String>> = listOf(
        mapOf("deviceId" to "SIM_SENSOR_A", "deviceName" to "SimUpperLeg"),
        mapOf("deviceId" to "SIM_SENSOR_B", "deviceName" to "SimLowerLeg")
    ),
    private val sampleRateHz: Double = 50.0
) : S1DataSource {

    private val intervalMs = (1000.0 / sampleRateHz).toLong()
    private val buffer = CopyOnWriteArrayList<SensorSample>()
    @Volatile private var running = false
    private var thread: Thread? = null
    private var startTime = 0L

    fun startGeneration() {
        if (running) return
        running = true
        startTime = System.currentTimeMillis()
        thread = Thread({
            while (running) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                for (sensor in sensorIds) {
                    buffer.add(generateSample(elapsed, sensor["deviceId"]!!, sensor["deviceName"]!!))
                }
                try { Thread.sleep(intervalMs) } catch (_: InterruptedException) { break }
            }
        }, "sim-data-gen").also { it.isDaemon = true; it.start() }
    }

    fun stopGeneration() {
        running = false
        thread?.interrupt()
        thread?.join(5000)
        thread = null
    }

    override fun read(): List<SensorSample> {
        val samples = ArrayList(buffer)
        buffer.clear()
        return samples
    }

    override fun status(): SensorStatus {
        return SensorStatus(
            running = running,
            error = if (running) null else "sensor_disconnected",
            totalCount = sensorIds.size,
            activeCount = if (running) sensorIds.size else 0,
            connectedCount = if (running) sensorIds.size else 0
        )
    }

    private fun generateSample(elapsed: Double, deviceId: String, deviceName: String): SensorSample {
        val freq = 0.5
        val phase = (deviceId.hashCode() % 100) * 0.01 * PI
        val noise = Random.nextGaussian() * 0.5
        val angleBase = 45.0 * sin(2 * PI * freq * elapsed + phase)
        return SensorSample(
            System.currentTimeMillis(), deviceId, deviceName,
            (sin(elapsed) * 0.2 + noise * 0.01).toFloat(),
            (cos(elapsed) * 0.3 + noise * 0.01).toFloat(),
            (0.98 + noise * 0.01).toFloat(),
            (noise * 2).toFloat(),
            (noise * 3).toFloat(),
            (noise * 1.5).toFloat(),
            (angleBase + noise).toFloat(),
            (angleBase * 0.8 + noise).toFloat(),
            (angleBase * 0.3 + noise).toFloat()
        )
    }
}

class SimulatedS1Module(
    sensorIds: List<Map<String, String>>? = null,
    sampleRateHz: Double = 50.0
) : S1Module {

    private val simService = SimulatedSensorService(
        sensorIds = sensorIds ?: listOf(
            mapOf("deviceId" to "SIM_SENSOR_A", "deviceName" to "SimUpperLeg"),
            mapOf("deviceId" to "SIM_SENSOR_B", "deviceName" to "SimLowerLeg")
        ),
        sampleRateHz = sampleRateHz
    )

    override val sensor: S1DataSource = simService

    override val session = object : S1SessionControl {
        override fun start(sessionMetaData: Map<String, Any>): Map<String, Any?> {
            simService.startGeneration()
            return mapOf("success" to true, "error" to null)
        }
        override fun stop(): Map<String, Any?> {
            simService.stopGeneration()
            return mapOf("success" to true, "error" to null)
        }
    }
}

private fun Random.nextGaussian(): Double {
    val u1 = nextDouble()
    val u2 = nextDouble()
    return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
}
