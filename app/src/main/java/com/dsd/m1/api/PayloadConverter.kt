package com.dsd.m1.api

import com.dsd.s2.model.FormatData
import java.text.SimpleDateFormat
import java.util.*

object PayloadConverter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun msToIso(tsMs: Long): String = isoFormat.format(Date(tsMs))

    fun formatDataToPayload(data: FormatData): Map<String, Any> {
        val targetAngles = data.targetAngles.map { ta ->
            mapOf(
                "timestamp" to msToIso(ta.timestamp),
                "angleID" to ta.angleID,
                "angle" to ta.angle
            )
        }

        val errors = data.errors.map { err ->
            mapOf(
                "timestamp" to msToIso(err.timestamp),
                "sensorId" to (err.sensorId ?: ""),
                "errorType" to err.errorType,
                "message" to err.message
            )
        }

        val sensorData = data.sensorData.map { sd ->
            mapOf(
                "timestamp" to msToIso(sd.timestamp),
                "sensorId" to sd.deviceId,
                "accX" to sd.accX, "accY" to sd.accY, "accZ" to sd.accZ,
                "gyroX" to sd.gyroX, "gyroY" to sd.gyroY, "gyroZ" to sd.gyroZ,
                "roll" to sd.roll, "pitch" to sd.pitch, "yaw" to sd.yaw
            )
        }

        return mapOf(
            "sessionId" to data.sessionContext.sessionId,
            "targetAngles" to targetAngles,
            "errors" to errors,
            "sensorData" to sensorData
        )
    }
}
