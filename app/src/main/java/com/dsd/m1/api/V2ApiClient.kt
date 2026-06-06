package com.dsd.m1.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log
import java.io.File

class V2ApiClient(
    private val baseUrl: String = "http://113.44.220.94:3000"
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private fun url(path: String) = "$baseUrl$path"

    private fun jsonBody(map: Map<String, Any?>): RequestBody {
        return gson.toJson(map).toRequestBody(JSON_MEDIA)
    }

    private fun authBuilder(token: String): Request.Builder {
        return Request.Builder().header("Authorization", "Bearer $token")
    }

    private fun parseError(body: String, code: Int): String {
        return try {
            gson.fromJson(body, JsonObject::class.java).get("error")?.asString
        } catch (_: Exception) {
            null
        } ?: "HTTP $code"
    }

    private fun parseResponse(response: Response): Map<String, Any?> {
        val body = response.body?.string() ?: "{}"
        if (!response.isSuccessful) {
            val err = parseError(body, response.code)
            Log.e("V2ApiClient", "Request failed: $err, responseBody: $body")
            throw RuntimeException(err)
        }
        return gson.fromJson(body, object : TypeToken<Map<String, Any?>>() {}.type)
    }

    private fun parseListResponse(response: Response): List<Map<String, Any?>> {
        val body = response.body?.string() ?: "[]"
        if (!response.isSuccessful) {
            val err = parseError(body, response.code)
            Log.e("V2ApiClient", "Request failed: $err, responseBody: $body")
            throw RuntimeException(err)
        }
        return gson.fromJson(body, object : TypeToken<List<Map<String, Any?>>>() {}.type)
    }

    private fun parseBytes(response: Response): ByteArray {
        if (!response.isSuccessful) {
            val body = response.body?.string() ?: ""
            val err = parseError(body, response.code)
            Log.e("V2ApiClient", "Request failed: $err, responseBody: $body")
            throw RuntimeException(err)
        }
        return response.body?.bytes() ?: byteArrayOf()
    }

    private fun emptyJsonBody(): RequestBody = "{}".toRequestBody(JSON_MEDIA)

    private fun ensureSuccess(response: Response) {
        if (!response.isSuccessful) {
            val body = response.body?.string() ?: ""
            val err = parseError(body, response.code)
            Log.e("V2ApiClient", "Request failed: $err, responseBody: $body")
            throw RuntimeException(err)
        }
    }

    // ==================== Health ====================

    fun healthCheck(): Map<String, Any?> {
        val req = Request.Builder().url(url("/health")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    // ==================== Auth ====================

    fun getUserPublic(id: Int): Map<String, Any?> {
        val req = Request.Builder().url(url("/users/$id")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    fun register(name: String, email: String, password: String, role: String = "patient"): Map<String, Any?> {
        val req = Request.Builder().url(url("/auth/register"))
            .post(jsonBody(PayloadConverter.registerPayload(name, email, password, role)))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun login(email: String, password: String): Map<String, Any?> {
        val req = Request.Builder().url(url("/auth/login"))
            .post(jsonBody(PayloadConverter.loginPayload(email, password)))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun getMe(token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/auth/me")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    fun getAuthStatus(token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/auth/status")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    fun approveUser(userId: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/auth/approve/$userId"))
            .patch(emptyJsonBody())
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun rejectUser(userId: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/auth/reject/$userId"))
            .patch(emptyJsonBody())
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    // ==================== Users ====================

    fun getUsers(role: String? = null, token: String): List<Map<String, Any?>> {
        val urlBuilder = url("/users").toHttpUrlOrNull()!!.newBuilder()
        role?.let { urlBuilder.addQueryParameter("role", it) }
        val req = authBuilder(token).url(urlBuilder.build()).get().build()
        return parseListResponse(client.newCall(req).execute())
    }

    fun getUser(id: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/users/$id")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    fun createUser(name: String, email: String, role: String, age: Int? = null, doctorId: Int? = null, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/users"))
            .post(jsonBody(PayloadConverter.createUserPayload(name, email, role, age, doctorId)))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun updateUser(
        id: Int,
        name: String? = null,
        email: String? = null,
        password: String? = null,
        age: Int? = null,
        role: String? = null,
        status: String? = null,
        conditionLabel: String? = null,
        conditionDate: String? = null,
        doctorId: Int? = null,
        token: String
    ): Map<String, Any?> {
        val req = authBuilder(token).url(url("/users/$id"))
            .patch(jsonBody(PayloadConverter.updateUserPayload(name, email, password, age, role, status, conditionLabel, conditionDate, doctorId)))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun getUserLicense(id: Int, token: String): ByteArray {
        val req = authBuilder(token).url(url("/users/$id/license")).get().build()
        return parseBytes(client.newCall(req).execute())
    }

    fun updateUserLicense(id: Int, licenseFile: File, token: String): Map<String, Any?> {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "license",
                licenseFile.name,
                licenseFile.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()
        val req = authBuilder(token).url(url("/users/$id/license")).patch(body).build()
        return parseResponse(client.newCall(req).execute())
    }

    // ==================== Patients ====================

    fun getPatients(token: String): List<Map<String, Any?>> {
        val req = authBuilder(token).url(url("/patients")).get().build()
        return parseListResponse(client.newCall(req).execute())
    }

    fun getPatient(id: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/patients/$id")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    // ==================== Sessions ====================

    fun createSession(userId: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/sessions"))
            .post(jsonBody(PayloadConverter.createSessionPayload(userId)))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun getSessions(userId: Int? = null, token: String): List<Map<String, Any?>> {
        val urlBuilder = url("/sessions").toHttpUrlOrNull()!!.newBuilder()
        userId?.let { urlBuilder.addQueryParameter("userId", it.toString()) }
        val req = authBuilder(token).url(urlBuilder.build()).get().build()
        return parseListResponse(client.newCall(req).execute())
    }

    fun getSession(sessionId: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/sessions/$sessionId")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    fun endSession(sessionId: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/sessions/$sessionId/end"))
            .patch(emptyJsonBody())
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun deleteSession(sessionId: Int, token: String) {
        val req = authBuilder(token).url(url("/sessions/$sessionId")).delete().build()
        ensureSuccess(client.newCall(req).execute())
    }

    // ==================== Measurements ====================

    fun uploadMeasurement(payload: Map<String, Any>, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/measurements"))
            .post(gson.toJson(payload).toRequestBody(JSON_MEDIA))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun uploadMeasurementsBatch(sessionId: Int, measurements: List<Map<String, Any>>, token: String): Map<String, Any?> {
        val payload = PayloadConverter.batchPayload(sessionId, measurements)
        val req = authBuilder(token).url(url("/measurements/batch"))
            .post(gson.toJson(payload).toRequestBody(JSON_MEDIA))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun uploadRawMeasurement(payload: Map<String, Any>, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/measurements/raw"))
            .post(gson.toJson(payload).toRequestBody(JSON_MEDIA))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun getMeasurements(sessionId: Int, startDate: String? = null, endDate: String? = null, token: String): List<Map<String, Any?>> {
        val urlBuilder = url("/measurements/$sessionId").toHttpUrlOrNull()!!.newBuilder()
        startDate?.let { urlBuilder.addQueryParameter("startDate", it) }
        endDate?.let { urlBuilder.addQueryParameter("endDate", it) }
        val req = authBuilder(token).url(urlBuilder.build()).get().build()
        return parseListResponse(client.newCall(req).execute())
    }

    // ==================== Recommendations ====================

    fun getEngineRecommendations(userId: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/recommendations/engine/$userId")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    fun getSessionRecommendations(sessionId: Int, token: String): List<Map<String, Any?>> {
        val req = authBuilder(token).url(url("/recommendations/session/$sessionId")).get().build()
        return parseListResponse(client.newCall(req).execute())
    }

    fun createRecommendation(sessionId: Int, movement: String, confidence: Double, notes: String? = null, token: String): Map<String, Any?> {
        val payload = PayloadConverter.createRecommendationPayload(sessionId, movement, confidence, notes)
        val req = authBuilder(token).url(url("/recommendations"))
            .post(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun updateRecommendation(id: Int, status: String, token: String): Map<String, Any?> {
        val payload = PayloadConverter.updateRecommendationPayload(status)
        val req = authBuilder(token).url(url("/recommendations/$id"))
            .patch(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    // ==================== Schedule ====================

    fun getSchedule(userId: Int, token: String): List<Map<String, Any?>> {
        val req = authBuilder(token).url(url("/schedule/$userId")).get().build()
        return parseListResponse(client.newCall(req).execute())
    }

    fun createSchedule(
        userId: Int,
        exercise: String,
        date: String,
        duration: Int? = null,
        notes: String? = null,
        videoUrl: String? = null,
        status: String? = null,
        token: String
    ): Map<String, Any?> {
        val payload = PayloadConverter.createSchedulePayload(userId, exercise, date, duration, notes, videoUrl, status)
        val req = authBuilder(token).url(url("/schedule"))
            .post(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun updateSchedule(
        scheduleId: Int,
        exercise: String? = null,
        date: String? = null,
        duration: Int? = null,
        notes: String? = null,
        videoUrl: String? = null,
        status: String? = null,
        token: String
    ): Map<String, Any?> {
        val payload = PayloadConverter.updateSchedulePayload(exercise, date, duration, notes, videoUrl, status)
        val req = authBuilder(token).url(url("/schedule/$scheduleId"))
            .patch(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun deleteSchedule(scheduleId: Int, token: String) {
        val req = authBuilder(token).url(url("/schedule/$scheduleId")).delete().build()
        ensureSuccess(client.newCall(req).execute())
    }

    fun getScheduleExercises(scheduleId: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/schedule/$scheduleId/exercises")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    fun addScheduleExercise(
        scheduleId: Int,
        name: String,
        phase: String? = null,
        sets: Int? = null,
        reps: Int? = null,
        holdSeconds: Int? = null,
        token: String
    ): Map<String, Any?> {
        val payload = PayloadConverter.addExercisePayload(name, phase, sets, reps, holdSeconds)
        val req = authBuilder(token).url(url("/schedule/$scheduleId/exercises"))
            .post(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun completeScheduleExercise(
        scheduleId: Int,
        exerciseId: Int,
        painLevel: Int? = null,
        token: String
    ): Map<String, Any?> {
        val payload = PayloadConverter.completeExercisePayload(painLevel)
        val req = authBuilder(token).url(url("/schedule/$scheduleId/exercises/$exerciseId/complete"))
            .patch(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    // ==================== Progress ====================

    fun getProgress(userId: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/progress/$userId")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    // ==================== Push ====================

    fun registerPushToken(userId: Int, deviceToken: String, platform: String, token: String): Map<String, Any?> {
        val payload = PayloadConverter.pushRegisterPayload(userId, deviceToken, platform)
        val req = authBuilder(token).url(url("/push/register"))
            .post(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun getPushTokens(userId: Int, token: String): List<Map<String, Any?>> {
        val req = authBuilder(token).url(url("/push/tokens/$userId")).get().build()
        return parseListResponse(client.newCall(req).execute())
    }

    // ==================== Feedback ====================

    fun getFeedback(status: String? = null, token: String): List<Map<String, Any?>> {
        val urlBuilder = url("/feedback").toHttpUrlOrNull()!!.newBuilder()
        status?.let { urlBuilder.addQueryParameter("status", it) }
        val req = authBuilder(token).url(urlBuilder.build()).get().build()
        return parseListResponse(client.newCall(req).execute())
    }

    fun getFeedbackById(id: Int, token: String): Map<String, Any?> {
        val req = authBuilder(token).url(url("/feedback/$id")).get().build()
        return parseResponse(client.newCall(req).execute())
    }

    fun createFeedback(userId: Int, content: String, token: String): Map<String, Any?> {
        val payload = PayloadConverter.createFeedbackPayload(userId, content)
        val req = authBuilder(token).url(url("/feedback"))
            .post(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun updateFeedback(id: Int, status: String? = null, response: String? = null, token: String): Map<String, Any?> {
        val payload = PayloadConverter.updateFeedbackPayload(status, response)
        val req = authBuilder(token).url(url("/feedback/$id"))
            .patch(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    // ==================== Announcements ====================

    fun getAnnouncements(token: String): List<Map<String, Any?>> {
        val req = authBuilder(token).url(url("/announcements")).get().build()
        return parseListResponse(client.newCall(req).execute())
    }

    fun createAnnouncement(title: String, content: String, createdBy: String, status: String? = null, token: String): Map<String, Any?> {
        val payload = PayloadConverter.createAnnouncementPayload(title, content, createdBy, status)
        val req = authBuilder(token).url(url("/announcements"))
            .post(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun updateAnnouncement(
        id: Int,
        title: String? = null,
        content: String? = null,
        createdBy: String? = null,
        status: String? = null,
        token: String
    ): Map<String, Any?> {
        val payload = PayloadConverter.updateAnnouncementPayload(title, content, createdBy, status)
        val req = authBuilder(token).url(url("/announcements/$id"))
            .patch(jsonBody(payload))
            .build()
        return parseResponse(client.newCall(req).execute())
    }

    fun deleteAnnouncement(id: Int, token: String) {
        val req = authBuilder(token).url(url("/announcements/$id")).delete().build()
        ensureSuccess(client.newCall(req).execute())
    }

    // ==================== Audit ====================

    fun getAuditLogs(userId: Int? = null, action: String? = null, targetType: String? = null, token: String): List<Map<String, Any?>> {
        val urlBuilder = url("/audit-logs").toHttpUrlOrNull()!!.newBuilder()
        userId?.let { urlBuilder.addQueryParameter("userId", it.toString()) }
        action?.let { urlBuilder.addQueryParameter("action", it) }
        targetType?.let { urlBuilder.addQueryParameter("targetType", it) }
        val req = authBuilder(token).url(urlBuilder.build()).get().build()
        return parseListResponse(client.newCall(req).execute())
    }

    // ==================== WebSocket ====================

    fun connectWebSocket(sessionId: Int, token: String, listener: WebSocketListener): WebSocket {
        val request = Request.Builder()
            .url("ws://113.44.220.94:3000/ws?sessionId=$sessionId")
            .header("Authorization", "Bearer $token")
            .build()
        return client.newWebSocket(request, listener)
    }
}