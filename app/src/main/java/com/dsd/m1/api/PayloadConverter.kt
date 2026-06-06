package com.dsd.m1.api

import com.dsd.s2.model.FormatData
import java.text.SimpleDateFormat
import java.util.*

object PayloadConverter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun msToIso(tsMs: Long): String = isoFormat.format(Date(tsMs))

    private fun build(vararg pairs: Pair<String, Any?>): Map<String, Any?> {
        return mapOf(*pairs).filterValues { it != null }
    }

    // ==================== S2 Data -> V2 Measurement ====================

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

    // ==================== Auth ====================

    fun registerPayload(name: String, email: String, password: String, role: String = "patient"): Map<String, Any?> {
        return build("name" to name, "email" to email, "password" to password, "role" to role)
    }

    fun loginPayload(email: String, password: String): Map<String, Any?> {
        return build("email" to email, "password" to password)
    }

    // ==================== Users ====================

    fun createUserPayload(name: String, email: String, role: String, age: Int? = null, doctorId: Int? = null): Map<String, Any?> {
        return build("name" to name, "email" to email, "role" to role, "age" to age, "doctorId" to doctorId)
    }

    fun updateUserPayload(
        name: String? = null,
        email: String? = null,
        password: String? = null,
        age: Int? = null,
        role: String? = null,
        status: String? = null,
        conditionLabel: String? = null,
        conditionDate: String? = null,
        doctorId: Int? = null
    ): Map<String, Any?> {
        return build(
            "name" to name,
            "email" to email,
            "password" to password,
            "age" to age,
            "role" to role,
            "status" to status,
            "conditionLabel" to conditionLabel,
            "conditionDate" to conditionDate,
            "doctorId" to doctorId
        )
    }

    // ==================== Sessions ====================

    fun createSessionPayload(userId: Int): Map<String, Any?> {
        return build("userId" to userId)
    }

    // ==================== Measurements ====================

    fun measurementPayload(
        sessionId: Int,
        targetAngles: List<Map<String, Any>> = emptyList(),
        sensorData: List<Map<String, Any>> = emptyList(),
        errors: List<Map<String, Any>> = emptyList()
    ): Map<String, Any?> {
        return build(
            "sessionId" to sessionId,
            "targetAngles" to targetAngles,
            "sensorData" to sensorData,
            "errors" to errors
        )
    }

    fun batchPayload(sessionId: Int, measurements: List<Map<String, Any>>): Map<String, Any?> {
        return build("sessionId" to sessionId, "measurements" to measurements)
    }

    fun rawPayload(
        sessionId: Int,
        targetAngles: List<Map<String, Any>> = emptyList(),
        sensorData: List<Map<String, Any>> = emptyList(),
        errors: List<Map<String, Any>> = emptyList()
    ): Map<String, Any?> {
        return build(
            "sessionId" to sessionId,
            "targetAngles" to targetAngles,
            "sensorData" to sensorData,
            "errors" to errors
        )
    }

    // ==================== Recommendations ====================

    fun createRecommendationPayload(sessionId: Int, movement: String, confidence: Double, notes: String? = null): Map<String, Any?> {
        return build("sessionId" to sessionId, "movement" to movement, "confidence" to confidence, "notes" to notes)
    }

    fun updateRecommendationPayload(status: String): Map<String, Any?> {
        return build("status" to status)
    }

    // ==================== Schedule ====================

    fun createSchedulePayload(
        userId: Int,
        exercise: String,
        date: String,
        duration: Int? = null,
        notes: String? = null,
        videoUrl: String? = null,
        status: String? = null
    ): Map<String, Any?> {
        return build(
            "userId" to userId,
            "exercise" to exercise,
            "date" to date,
            "duration" to duration,
            "notes" to notes,
            "videoUrl" to videoUrl,
            "status" to status
        )
    }

    fun updateSchedulePayload(
        exercise: String? = null,
        date: String? = null,
        duration: Int? = null,
        notes: String? = null,
        videoUrl: String? = null,
        status: String? = null
    ): Map<String, Any?> {
        return build(
            "exercise" to exercise,
            "date" to date,
            "duration" to duration,
            "notes" to notes,
            "videoUrl" to videoUrl,
            "status" to status
        )
    }

    fun addExercisePayload(
        name: String,
        phase: String? = null,
        sets: Int? = null,
        reps: Int? = null,
        holdSeconds: Int? = null
    ): Map<String, Any?> {
        return build(
            "name" to name,
            "phase" to phase,
            "sets" to sets,
            "reps" to reps,
            "holdSeconds" to holdSeconds
        )
    }

    fun completeExercisePayload(painLevel: Int? = null): Map<String, Any?> {
        return build("painLevel" to painLevel)
    }

    // ==================== Push ====================

    fun pushRegisterPayload(userId: Int, token: String, platform: String): Map<String, Any?> {
        return build("userId" to userId, "token" to token, "platform" to platform)
    }

    // ==================== Feedback ====================

    fun createFeedbackPayload(userId: Int, content: String): Map<String, Any?> {
        return build("userId" to userId, "content" to content)
    }

    fun updateFeedbackPayload(status: String? = null, response: String? = null): Map<String, Any?> {
        return build("status" to status, "response" to response)
    }

    // ==================== Announcements ====================

    fun createAnnouncementPayload(title: String, content: String, createdBy: String, status: String? = null): Map<String, Any?> {
        return build("title" to title, "content" to content, "createdBy" to createdBy, "status" to status)
    }

    fun updateAnnouncementPayload(
        title: String? = null,
        content: String? = null,
        createdBy: String? = null,
        status: String? = null
    ): Map<String, Any?> {
        return build("title" to title, "content" to content, "createdBy" to createdBy, "status" to status)
    }
}