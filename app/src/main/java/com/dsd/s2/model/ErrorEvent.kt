package com.dsd.s2.model

data class ErrorEvent(
    val timestamp: Long,
    val sensorId: String?,
    val errorType: String,
    val message: String
)
