package com.dsd.s2.buffer

import com.dsd.s1.model.SensorSample
import com.dsd.s2.model.*

class AsyncBuffer(val sessionContext: SessionContext) {

    private val lock = Any()
    private val sensorData = mutableListOf<SensorSample>()
    private val targetAngles = mutableListOf<TargetAngle>()
    private val errors = mutableListOf<ErrorEvent>()
    private val cursors = mutableMapOf<String, Triple<Int, Int, Int>>()

    fun pushSamples(samples: List<SensorSample>) {
        synchronized(lock) { sensorData.addAll(samples) }
    }

    fun pushAngles(angles: List<TargetAngle>) {
        synchronized(lock) { targetAngles.addAll(angles) }
    }

    fun pushError(error: ErrorEvent) {
        synchronized(lock) { errors.add(error) }
    }

    fun drain(consumerId: String = "default"): FormatData {
        synchronized(lock) {
            val (sdStart, taStart, errStart) = cursors.getOrDefault(consumerId, Triple(0, 0, 0))

            val data = FormatData(
                sessionContext = sessionContext,
                sensorData = sensorData.subList(sdStart, sensorData.size).toList(),
                targetAngles = targetAngles.subList(taStart, targetAngles.size).toList(),
                errors = errors.subList(errStart, errors.size).toList()
            )

            cursors[consumerId] = Triple(sensorData.size, targetAngles.size, errors.size)
            maybeCompact()
            return data
        }
    }

    private fun maybeCompact() {
        if (cursors.isEmpty()) return

        val minSd = cursors.values.minOf { it.first }
        val minTa = cursors.values.minOf { it.second }
        val minErr = cursors.values.minOf { it.third }

        if (minSd > 0) {
            sensorData.subList(0, minSd).clear()
            for ((cid, c) in cursors) {
                cursors[cid] = Triple(c.first - minSd, c.second, c.third)
            }
        }
        if (minTa > 0) {
            targetAngles.subList(0, minTa).clear()
            for ((cid, c) in cursors) {
                cursors[cid] = Triple(c.first, c.second - minTa, c.third)
            }
        }
        if (minErr > 0) {
            errors.subList(0, minErr).clear()
            for ((cid, c) in cursors) {
                cursors[cid] = Triple(c.first, c.second, c.third - minErr)
            }
        }
    }
}
