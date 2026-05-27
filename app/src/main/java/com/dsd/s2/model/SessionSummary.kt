package com.dsd.s2.model

data class SessionSummary(
    val sessionId: Int,
    val sampleCount: Int,
    val errorCount: Int,
    val startTime: String,
    val endTime: String
)
