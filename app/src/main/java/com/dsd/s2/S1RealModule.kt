package com.dsd.s2

import android.util.Log
import com.dsd.s1.ble.SensorService
import com.dsd.s1.model.SensorSample
import com.dsd.s1.model.SensorStatus
import com.dsd.s2.core.S1DataSource
import com.dsd.s2.core.S1Module
import com.dsd.s2.core.S1SessionControl

private const val TAG = "S1RealModule"

class S1RealModule(private val sensorService: SensorService) : S1Module {

    override val sensor = object : S1DataSource {
        override fun read(): List<SensorSample> = sensorService.readSamples()
        override fun status(): SensorStatus = sensorService.getStatus()
    }

    override val session = object : S1SessionControl {
        override fun start(sessionMetaData: Map<String, Any>): Map<String, Any?> {
            return try {
                sensorService.startSensors()
                Log.i(TAG, "S1 session started with metadata: $sessionMetaData")
                mapOf("success" to true, "error" to null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start S1 session", e)
                mapOf("success" to false, "error" to e.message)
            }
        }

        override fun stop(): Map<String, Any?> {
            return try {
                sensorService.stopSensors()
                Log.i(TAG, "S1 session stopped")
                mapOf("success" to true, "error" to null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop S1 session", e)
                mapOf("success" to false, "error" to e.message)
            }
        }
    }
}
