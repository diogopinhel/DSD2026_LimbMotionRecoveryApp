package com.dsd.s1.ble

import android.content.Context

class SensorService(private val context: Context) {
    fun initialize(sensorConfigs: List<com.dsd.s1.model.SensorConfig>, serviceConfig: com.dsd.s1.model.ServiceConfig) {}
    fun startSensors() {}
    fun stopSensors() {}
    fun readSamples(): List<com.dsd.s1.model.SensorSample> = emptyList()
    fun getStatus(): com.dsd.s1.model.SensorStatus =
        com.dsd.s1.model.SensorStatus(false, "", "", false, 0, 0, 0)
}
