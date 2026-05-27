package com.dsd.s2.model

data class SessionContext(
    val sessionId: Int,
    val userId: Int,
    val sensorJointMapping: Map<String, String>,
    val payloadStatus: String
)
