package com.dsd.s1.model

data class SensorStatus(
    val running: Boolean = false,
    val error: String? = null,
    val errorMessage: String? = null,
    val connected: Boolean = false,
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val connectedCount: Int = 0,
    val connectedSensors: Int = 0
)
