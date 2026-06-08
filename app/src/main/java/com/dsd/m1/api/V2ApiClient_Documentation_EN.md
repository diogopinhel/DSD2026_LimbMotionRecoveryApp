# V2ApiClient API Documentation

> **Base URL**: `http://113.44.220.94:3000`  
> **Language**: Kotlin  
> **HTTP Client**: OkHttp + Gson  
> **Payload Conversion**: All request bodies are constructed via `PayloadConverter`; responses are uniformly parsed into `Map<String, Any?>` or `List<Map<String, Any?>>`.  
> **Exceptions**: All methods throw `RuntimeException` on failure. The error message contains either the `error` field returned by V2 or the HTTP status code.

---

## Quick Reference

The table below lists all available methods, a one-line usage example, and a brief description. Click a method name to jump to its detailed section.

| Method | Example Call | Description |
|---|---|---|
| `healthCheck` | `v2.healthCheck()` | Check if the backend service is alive |
| `register` | `v2.register(name, email, password)` | Register a new patient account and receive a token |
| `login` | `v2.login(email, password)` | Log in and obtain a JWT token |
| `getMe` | `v2.getMe(token)` | Retrieve the current user's full profile |
| `getAuthStatus` | `v2.getAuthStatus(token)` | Get the current user's role and approval status |
| `approveUser` | `v2.approveUser(userId, token)` | Admin: approve a pending clinician account |
| `rejectUser` | `v2.rejectUser(userId, token)` | Admin: reject a pending clinician account |
| `getUsers` | `v2.getUsers(role, token)` | List users, optionally filtered by role |
| `getUser` | `v2.getUser(id, token)` | Get a specific user's details (commonly used to verify a doctorId) |
| `createUser` | `v2.createUser(name, email, role, token)` | Admin: manually create a user without a password |
| `updateUser` | `v2.updateUser(id, doctorId = 5, token)` | Update user info, including binding or changing a doctor |
| `getUserLicense` | `v2.getUserLicense(id, token)` | Download a clinician's license file (returns ByteArray) |
| `updateUserLicense` | `v2.updateUserLicense(id, file, token)` | Upload or replace a clinician's license file |
| `getPatients` | `v2.getPatients(token)` | List all patients |
| `getPatient` | `v2.getPatient(id, token)` | Get a specific patient's details |
| `createSession` | `v2.createSession(userId, token)` | Create a new rehabilitation exercise session |
| `getSessions` | `v2.getSessions(userId, token)` | List all sessions for a given user |
| `getSession` | `v2.getSession(sessionId, token)` | Get a single session detail with nested measurements |
| `endSession` | `v2.endSession(sessionId, token)` | End an active session |
| `deleteSession` | `v2.deleteSession(sessionId, token)` | Delete a session and its cascading data |
| `uploadMeasurement` | `v2.uploadMeasurement(payload, token)` | Upload a single measurement (angles, sensor data, errors) |
| `uploadMeasurementsBatch` | `v2.uploadMeasurementsBatch(sessionId, measurements, token)` | Upload multiple measurements in one request |
| `uploadRawMeasurement` | `v2.uploadRawMeasurement(payload, token)` | Upload a measurement in S2 raw format |
| `getMeasurements` | `v2.getMeasurements(sessionId, token)` | Retrieve all measurements for a session |
| `getEngineRecommendations` | `v2.getEngineRecommendations(userId, token)` | Get AI engine recommendations based on the last 10 sessions |
| `getSessionRecommendations` | `v2.getSessionRecommendations(sessionId, token)` | Get recommendations attached to a specific session |
| `createRecommendation` | `v2.createRecommendation(sessionId, movement, confidence, token)` | Create a rehabilitation recommendation record |
| `updateRecommendation` | `v2.updateRecommendation(id, status, token)` | Update a recommendation's status |
| `getSchedule` | `v2.getSchedule(userId, token)` | Get a patient's rehabilitation schedule list |
| `createSchedule` | `v2.createSchedule(userId, exercise, date, token)` | Create a new schedule item |
| `updateSchedule` | `v2.updateSchedule(scheduleId, status, token)` | Update a schedule item (e.g., mark as completed) |
| `deleteSchedule` | `v2.deleteSchedule(scheduleId, token)` | Delete a schedule item |
| `getScheduleExercises` | `v2.getScheduleExercises(scheduleId, token)` | Get the list of exercises under a schedule |
| `addScheduleExercise` | `v2.addScheduleExercise(scheduleId, name, token)` | Add an exercise to a schedule |
| `completeScheduleExercise` | `v2.completeScheduleExercise(scheduleId, exerciseId, token)` | Mark an exercise in a schedule as completed |
| `getProgress` | `v2.getProgress(userId, token)` | Get combined patient progress (ROM, adherence, pain) |
| `registerPushToken` | `v2.registerPushToken(userId, deviceToken, platform, token)` | Register an FCM/APNS device token for push notifications |
| `getPushTokens` | `v2.getPushTokens(userId, token)` | List registered push tokens for a user |
| `getFeedback` | `v2.getFeedback(status, token)` | List user feedback entries |
| `getFeedbackById` | `v2.getFeedbackById(id, token)` | Get a single feedback entry |
| `createFeedback` | `v2.createFeedback(userId, content, token)` | Submit new user feedback |
| `updateFeedback` | `v2.updateFeedback(id, status, response, token)` | Update feedback status and admin response |
| `getAnnouncements` | `v2.getAnnouncements(token)` | List system announcements |
| `createAnnouncement` | `v2.createAnnouncement(title, content, createdBy, token)` | Create a new announcement |
| `updateAnnouncement` | `v2.updateAnnouncement(id, title, content, token)` | Update an announcement |
| `deleteAnnouncement` | `v2.deleteAnnouncement(id, token)` | Delete an announcement |
| `getAuditLogs` | `v2.getAuditLogs(userId, action, token)` | Query admin audit logs |
| `connectWebSocket` | `v2.connectWebSocket(sessionId, token, listener)` | Establish a WebSocket connection for real-time feedback |

---

## 1. Health Check

### 1.1 `healthCheck()`

**Description**: Checks whether the V2 backend is online. Commonly used for network connectivity probing at app startup.

**Parameters**: None.

**Returns**: `Map<String, Any?>` — Usually contains a status field, e.g., `{"status": "ok"}`.

**Usage Example**:

```kotlin
try {
    val response = v2.healthCheck()
    Log.i("Health", "Server status: $response")
} catch (e: RuntimeException) {
    Log.e("Health", "Server unreachable: ${e.message}")
}
```

---

## 2. Authentication (Auth)

### 2.1 `register(name, email, password, role)`

**Description**: Registers a new user. Patients (`"patient"`) are registered immediately and receive a token. Clinicians (`"clinician"`) enter a `pending` state and require admin approval.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `name` | `String` | Yes | User's full name |
| `email` | `String` | Yes | Email address, unique identifier |
| `password` | `String` | Yes | Login password |
| `role` | `String` | No | Role, defaults to `"patient"`; optional `"clinician"` |

**Returns**: `Map<String, Any?>` — For patients: `{"token": "jwt...", "user": {...}}`. For clinicians: `{"userId": 3, "status": "pending"}`.

**Usage Example**:

```kotlin
try {
    val result = v2.register("Ana Costa", "ana@utad.pt", "123456", "patient")
    val token = result["token"] as String
    val user = result["user"] as Map<String, Any?>
    Log.i("Auth", "Registered, token: $token, userId: ${user["id"]}")
} catch (e: RuntimeException) {
    Log.e("Auth", "Register failed: ${e.message}") // 400: missing fields / 409: email exists
}
```

### 2.2 `login(email, password)`

**Description**: Authenticates with email and password to obtain a JWT token.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `email` | `String` | Yes | Registered email |
| `password` | `String` | Yes | Password |

**Returns**: `Map<String, Any?>` — Same structure as `register`, containing `token` and `user`.

**Usage Example**:

```kotlin
try {
    val result = v2.login("ana@utad.pt", "123456")
    val token = result["token"] as String
    val user = result["user"] as Map<String, Any?>
    val doctorId = user["doctor_id"] as? Double ?: 0.0
    Log.i("Auth", "Login OK, doctorId: ${doctorId.toInt()}")
} catch (e: RuntimeException) {
    Log.e("Auth", "Login failed: ${e.message}") // 401: wrong password / 404: user not found
}
```

### 2.3 `getMe(token)`

**Description**: Retrieves the full profile of the user associated with the current token. Used after login to load home-screen data or check `doctor_id` binding status.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `token` | `String` | Yes | JWT token from `login` or `register` |

**Returns**: `Map<String, Any?>` — User object containing `id`, `name`, `email`, `role`, `doctor_id`, `age`, `condition_label`, `currentRomDegrees`, `adherencePercent`, `streakWeeks`, etc.

**Usage Example**:

```kotlin
try {
    val me = v2.getMe(token)
    val name = me["name"] as String
    val doctorId = (me["doctor_id"] as? Double)?.toInt() ?: 0
    if (doctorId == 0) {
        Log.w("Profile", "Doctor not bound yet")
    }
} catch (e: RuntimeException) {
    Log.e("Profile", "Failed to get profile: ${e.message}")
}
```

### 2.4 `getAuthStatus(token)`

**Description**: Retrieves the current user's role and approval status. Used to determine whether a clinician has been approved.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — e.g., `{"userId": 1, "role": "patient", "status": "active"}`.

**Usage Example**:

```kotlin
val status = v2.getAuthStatus(token)
val userStatus = status["status"] as String
if (userStatus == "pending") {
    showPendingApprovalDialog()
}
```

### 2.5 `approveUser(userId, token)` / `rejectUser(userId, token)`

**Description**: Admin-only. Approves or rejects a clinician account that is in `pending` status.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int` | Yes | ID of the user to approve/reject |
| `token` | `String` | Yes | Admin JWT token |

**Returns**: `Map<String, Any?>` — Updated user status, e.g., `{"userId": 3, "role": "clinician", "status": "active"}`.

**Usage Example**:

```kotlin
try {
    v2.approveUser(3, adminToken)
    Log.i("Admin", "Clinician approved")
} catch (e: RuntimeException) {
    Log.e("Admin", "Approval failed: ${e.message}") // 404: not found / 409: not pending
}
```

---

## 3. Users & Doctor Binding

### 3.1 `getUsers(role, token)`

**Description**: Lists users, optionally filtered by role. M1 commonly uses this to retrieve all clinicians for doctorId verification.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `role` | `String?` | No | Filter role, e.g., `"clinician"` for doctors; `null` for all |
| `token` | `String` | Yes | JWT token |

**Returns**: `List<Map<String, Any?>>` — Array of user objects.

**Usage Example**:

```kotlin
val clinicians = v2.getUsers(role = "clinician", token)
clinicians.forEach { doc ->
    Log.d("Doctors", "${doc["id"]}: ${doc["name"]}")
}
```

### 3.2 `getUser(id, token)`

**Description**: Retrieves details for a specific user. M1 calls this during registration or doctor rebinding to verify that the `doctorId` exists and its role is `clinician`.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Int` | Yes | User ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — User details including `id`, `name`, `email`, `role`, `doctor_id`, `session_count`.

**Usage Example**:

```kotlin
try {
    val doctor = v2.getUser(5, token)
    if (doctor["role"] == "clinician") {
        showDoctorConfirmDialog(doctor["name"] as String, doctor["id"] as Double)
    }
} catch (e: RuntimeException) {
    Log.e("Doctor", "Invalid doctorId: ${e.message}") // 404
}
```

### 3.3 `createUser(name, email, role, age, doctorId, token)`

**Description**: Admin-only. Creates a user without a password. Typically used by the M2 doctor portal or back-office management.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `name` | `String` | Yes | Full name |
| `email` | `String` | Yes | Email address |
| `role` | `String` | Yes | Role |
| `age` | `Int?` | No | Age |
| `doctorId` | `Int?` | No | Bound doctor ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — The created user object.

**Usage Example**:

```kotlin
val newUser = v2.createUser(
    name = "Test Patient",
    email = "test@example.com",
    role = "patient",
    age = 30,
    token = adminToken
)
```

### 3.4 `updateUser(id, name, age, role, status, conditionLabel, conditionDate, doctorId, token)`

**Description**: Updates user information. The most common M1 use case is **binding or changing a doctor** via the `doctorId` parameter. Only pass the fields you want to change; leave others as `null`.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Int` | Yes | User ID to update |
| `name` | `String?` | No | New name |
| `age` | `Int?` | No | New age |
| `role` | `String?` | No | New role |
| `status` | `String?` | No | Account status |
| `conditionLabel` | `String?` | No | Condition label |
| `conditionDate` | `String?` | No | Condition date (ISO 8601) |
| `doctorId` | `Int?` | No | **Bind or change doctor**; pass `0` to unbind |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — The updated full user object.

**Usage Example**:

```kotlin
// Bind doctor after registration
try {
    val updated = v2.updateUser(id = 1, doctorId = 5, token = token)
    Log.i("Bind", "Bound to doctor: ${updated["doctor_id"]}")
} catch (e: RuntimeException) {
    // 403: target is not a clinician / 404: not found / 409: already bound to same doctor
    Log.e("Bind", "Binding failed: ${e.message}")
}

// Change doctor in Settings
try {
    val updated = v2.updateUser(id = 1, doctorId = 8, token = token)
    showSnackbar("Doctor updated successfully")
} catch (e: RuntimeException) {
    showError(e.message ?: "Update failed")
}
```

### 3.5 `getUserLicense(id, token)`

**Description**: Downloads a clinician's license file. Returns raw bytes for local storage or display.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Int` | Yes | Clinician user ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `ByteArray` — Raw file content.

**Usage Example**:

```kotlin
try {
    val bytes = v2.getUserLicense(5, token)
    val file = File(context.cacheDir, "license_5.pdf")
    file.writeBytes(bytes)
    openPdfViewer(file)
} catch (e: RuntimeException) {
    Log.e("License", "Download failed: ${e.message}") // 404: no license on file
}
```

### 3.6 `updateUserLicense(id, licenseFile, token)`

**Description**: Uploads or replaces a clinician's license file using `multipart/form-data`.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Int` | Yes | Clinician user ID |
| `licenseFile` | `File` | Yes | License file (PDF or image) |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — Update result.

**Usage Example**:

```kotlin
val license = File(context.filesDir, "license.pdf")
try {
    v2.updateUserLicense(5, license, token)
    Log.i("License", "Upload success")
} catch (e: RuntimeException) {
    Log.e("License", "Upload failed: ${e.message}")
}
```

---

## 4. Patients

### 4.1 `getPatients(token)` / `getPatient(id, token)`

**Description**: Retrieves the list of all patients or a single patient's details. The returned object shape is identical to `/users`, but `role` is always `patient` and includes `session_count` and `doctor_id`.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `token` | `String` | Yes | JWT token |
| `id` | `Int` | Yes | Patient ID (only for `getPatient`) |

**Returns**: `List<Map<String, Any?>>` or `Map<String, Any?>`.

**Usage Example**:

```kotlin
val patients = v2.getPatients(token)
val target = patients.find { (it["id"] as Double).toInt() == 1 }
```

---

## 5. Sessions

### 5.1 `createSession(userId, token)`

**Description**: Creates a new rehabilitation exercise session. Must be called before starting a workout. The returned `sessionId` is passed to S2 and subsequent measurement uploads.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int` | Yes | Patient user ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — New session object containing `id`, `user_id`, `user_name`, `started_at`, `ended_at`.

**Usage Example**:

```kotlin
try {
    val session = v2.createSession(userId = 1, token = token)
    val sessionId = (session["id"] as Double).toInt()
    // Pass sessionId to S2 to start data acquisition
    s2.session.start("session-$sessionId", "patient-1", sensorMap, "bend_knee_10")
} catch (e: RuntimeException) {
    Log.e("Session", "Create failed: ${e.message}")
}
```

### 5.2 `getSessions(userId, token)`

**Description**: Retrieves all sessions for a given user, used to populate the history screen.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int?` | No | Filter by user ID; pass `null` to retrieve all (requires permission) |
| `token` | `String` | Yes | JWT token |

**Returns**: `List<Map<String, Any?>>` — Array of session summaries, each containing `id`, `user_id`, `started_at`, `ended_at`, `measurement_count`.

**Usage Example**:

```kotlin
val sessions = v2.getSessions(userId = 1, token = token)
sessions.forEach { s ->
    val id = (s["id"] as Double).toInt()
    val count = (s["measurement_count"] as Double).toInt()
    Log.d("History", "Session $id has $count measurements")
}
```

### 5.3 `getSession(sessionId, token)`

**Description**: Retrieves full details for a single session, including the nested `measurements` array.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | `Int` | Yes | Session ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — Full session object with `measurements` array. Each measurement contains `target_angles`, `joint_angles`, `errors`, `sensor_data`, `is_correct`.

**Usage Example**:

```kotlin
val detail = v2.getSession(sessionId = 1, token = token)
val measurements = detail["measurements"] as List<Map<String, Any?>>
measurements.forEach { m ->
    val angles = m["target_angles"] as List<Map<String, Any?>>
    angles.forEach { a ->
        Log.d("Angle", "${a["angleID"]}: ${a["angle"]}")
    }
}
```

### 5.4 `endSession(sessionId, token)`

**Description**: Ends the specified active session. After calling, `ended_at` is set to the current time and no further measurements can be uploaded to this session.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | `Int` | Yes | Session ID to end |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — Updated session object with `ended_at` as an ISO 8601 string.

**Usage Example**:

```kotlin
try {
    val ended = v2.endSession(sessionId = 1, token = token)
    Log.i("Session", "Ended at: ${ended["ended_at"]}")
} catch (e: RuntimeException) {
    // 404: session not found / 409: already closed
    Log.e("Session", "End failed: ${e.message}")
}
```

### 5.5 `deleteSession(sessionId, token)`

**Description**: Deletes a session and its cascading data (measurements, recommendations). On success, returns HTTP 204 with no body.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | `Int` | Yes | Session ID to delete |
| `token` | `String` | Yes | JWT token |

**Returns**: None (`Unit`). Throws on failure.

**Usage Example**:

```kotlin
try {
    v2.deleteSession(sessionId = 1, token = token)
    Log.i("History", "Session deleted")
    refreshHistoryList()
} catch (e: RuntimeException) {
    Log.e("History", "Delete failed: ${e.message}")
}
```

---

## 6. Measurements

### 6.1 `uploadMeasurement(payload, token)`

**Description**: Uploads a single measurement to the specified session. The request body should be constructed via `PayloadConverter`, or manually built to match the V2 format.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `payload` | `Map<String, Any>` | Yes | Measurement data; must include `sessionId`; optional `targetAngles`, `sensorData`, `errors` |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — The stored measurement object.

**Usage Example**:

```kotlin
// Option 1: Use PayloadConverter to transform S2 data
val formatData: FormatData = s2.data.read()
val payload = PayloadConverter.formatDataToPayload(formatData)
val stored = v2.uploadMeasurement(payload, token)

// Option 2: Manual construction
val manualPayload = mapOf(
    "sessionId" to 1,
    "targetAngles" to listOf(
        mapOf("timestamp" to "2026-05-02T13:43:38.549Z", "angleID" to "knee", "angle" to 45.2)
    ),
    "sensorData" to emptyList<Map<String, Any>>(),
    "errors" to emptyList<Map<String, Any>>()
)
val stored = v2.uploadMeasurement(manualPayload, token)
```

### 6.2 `uploadMeasurementsBatch(sessionId, measurements, token)`

**Description**: Uploads multiple measurements in a single request, reducing network overhead. Ideal for flushing cached data after a workout.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | `Int` | Yes | Session ID |
| `measurements` | `List<Map<String, Any>>` | Yes | Array of measurements; each may contain `targetAngles` (or `jointAngles`), `sensorData`, `errors` |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — e.g., `{"inserted": 2, "sessionId": 1}`.

**Usage Example**:

```kotlin
val measurements = listOf(
    mapOf(
        "targetAngles" to listOf(mapOf("timestamp" to "2026-05-02T13:43:38.549Z", "angleID" to "knee", "angle" to 45.2)),
        "sensorData" to emptyList<Map<String, Any>>(),
        "errors" to emptyList<Map<String, Any>>()
    ),
    mapOf(
        "targetAngles" to listOf(mapOf("timestamp" to "2026-05-02T13:43:38.582Z", "angleID" to "knee", "angle" to 38.0)),
        "sensorData" to emptyList<Map<String, Any>>(),
        "errors" to emptyList<Map<String, Any>>()
    )
)
val result = v2.uploadMeasurementsBatch(1, measurements, token)
Log.i("Batch", "Inserted ${result["inserted"]} measurements")
```

### 6.3 `uploadRawMeasurement(payload, token)`

**Description**: Uploads a measurement in S2 raw format. Same request body shape as `uploadMeasurement`, but hits a different endpoint.

**Parameters**: Same as `uploadMeasurement`.

**Returns**: Same as `uploadMeasurement`.

**Usage Example**:

```kotlin
val rawPayload = PayloadConverter.formatDataToPayload(s2Data)
val stored = v2.uploadRawMeasurement(rawPayload, token)
```

### 6.4 `getMeasurements(sessionId, startDate, endDate, token)`

**Description**: Retrieves all measurements for a session, optionally filtered by a date range.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | `Int` | Yes | Session ID |
| `startDate` | `String?` | No | Start date (ISO 8601, e.g., `2026-05-01T00:00:00Z`) |
| `endDate` | `String?` | No | End date |
| `token` | `String` | Yes | JWT token |

**Returns**: `List<Map<String, Any?>>` — Array of measurement objects.

**Usage Example**:

```kotlin
val measurements = v2.getMeasurements(
    sessionId = 1,
    startDate = "2026-05-01T00:00:00Z",
    endDate = "2026-05-03T23:59:59Z",
    token = token
)
```

---

## 7. Recommendations

### 7.1 `getEngineRecommendations(userId, token)`

**Description**: Retrieves AI-generated recommendations based on the user's last 10 sessions. Used for the "View AI Evaluation" feature.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int` | Yes | Patient ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — Contains `userId`, `sessions_analysed`, `generated_at`, and a `suggestions` array. Each suggestion contains `joint`, `accuracy_percent`, `total_measurements`, `priority`, `suggestion`.

**Usage Example**:

```kotlin
val rec = v2.getEngineRecommendations(1, token)
val suggestions = rec["suggestions"] as List<Map<String, Any?>>
suggestions.forEach { s ->
    val joint = s["joint"] as String
    val accuracy = (s["accuracy_percent"] as Double).toInt()
    val priority = s["priority"] as String
    Log.d("AI", "$joint: $accuracy% (priority: $priority)")
}
```

### 7.2 `getSessionRecommendations(sessionId, token)`

**Description**: Retrieves recommendations attached to a specific session.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | `Int` | Yes | Session ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `List<Map<String, Any?>>` — Array of recommendation records.

### 7.3 `createRecommendation(sessionId, movement, confidence, notes, token)`

**Description**: Creates a rehabilitation recommendation record. Usually called by the M2 doctor portal or V2 internal AI; M1 typically does not call this directly.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | `Int` | Yes | Associated session ID |
| `movement` | `String` | Yes | Movement name |
| `confidence` | `Double` | Yes | Confidence score (0.0 - 1.0) |
| `notes` | `String?` | No | Optional notes |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — The created recommendation object.

### 7.4 `updateRecommendation(id, status, token)`

**Description**: Updates a recommendation's status, e.g., when a patient accepts or rejects a suggestion.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Int` | Yes | Recommendation ID |
| `status` | `String` | Yes | New status: `pending`, `accepted`, `rejected` |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — The updated recommendation object.

**Usage Example**:

```kotlin
v2.updateRecommendation(10, "accepted", token)
```

---

## 8. Schedule (Plans)

### 8.1 `getSchedule(userId, token)`

**Description**: Retrieves the rehabilitation schedule list for a patient, ordered by date ascending.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int` | Yes | Patient ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `List<Map<String, Any?>>` — Array of schedule items, each containing `id`, `user_id`, `exercise`, `date`, `duration`, `notes`, `video_url`, `status`, `doctor_name`, `created_at`.

**Usage Example**:

```kotlin
val schedule = v2.getSchedule(1, token)
schedule.forEach { item ->
    val status = item["status"] as String
    val videoUrl = item["video_url"] as? String
    Log.d("Schedule", "Exercise: ${item["exercise"]}, Status: $status")
}
```

### 8.2 `createSchedule(userId, exercise, date, duration, notes, videoUrl, status, token)`

**Description**: Creates a new rehabilitation schedule item. Usually called by the M2 doctor portal; M1 is read-only.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int` | Yes | Patient ID |
| `exercise` | `String` | Yes | Exercise name (e.g., `"squat"`) |
| `date` | `String` | Yes | Schedule date (ISO 8601) |
| `duration` | `Int?` | No | Estimated duration in minutes |
| `notes` | `String?` | No | Task description / notes |
| `videoUrl` | `String?` | No | Demo video URL |
| `status` | `String?` | No | Initial status, defaults to `pending` |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — The created schedule object.

### 8.3 `updateSchedule(scheduleId, exercise, date, duration, notes, videoUrl, status, token)`

**Description**: Updates a schedule item. The most common M1 use case is **marking a schedule as completed** (pass `status = "completed"`).

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `scheduleId` | `Int` | Yes | Schedule ID |
| `exercise` | `String?` | No | New exercise name |
| `date` | `String?` | No | New date |
| `duration` | `Int?` | No | New duration |
| `notes` | `String?` | No | New description |
| `videoUrl` | `String?` | No | New video URL |
| `status` | `String?` | No | New status: `pending`, `completed`, `skipped` |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — The updated full schedule object.

**Usage Example**:

```kotlin
// Mark schedule as completed
try {
    val updated = v2.updateSchedule(scheduleId = 1, status = "completed", token = token)
    Log.i("Schedule", "Status updated to: ${updated["status"]}")
} catch (e: RuntimeException) {
    // 400: invalid status / 404: not found / 409: already completed
    Log.e("Schedule", "Update failed: ${e.message}")
}
```

### 8.4 `deleteSchedule(scheduleId, token)`

**Description**: Deletes a schedule item and its associated exercises. On success, returns HTTP 204.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `scheduleId` | `Int` | Yes | Schedule ID |
| `token` | `String` | Yes | JWT token |

**Returns**: None (`Unit`).

### 8.5 `getScheduleExercises(scheduleId, token)`

**Description**: Retrieves the detailed exercise list under a schedule (Plan Details screen).

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `scheduleId` | `Int` | Yes | Schedule ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — Contains schedule metadata plus an `exercises` array. Each exercise contains `id`, `name`, `phase`, `sets`, `reps`, `holdSeconds`, `completed`, `lastPainLevel`.

**Usage Example**:

```kotlin
val detail = v2.getScheduleExercises(1, token)
val exercises = detail["exercises"] as List<Map<String, Any?>>
exercises.forEach { ex ->
    val completed = ex["completed"] as Boolean
    val phase = ex["phase"] as String
    Log.d("Exercise", "${ex["name"]} [$phase] — Completed: $completed")
}
```

### 8.6 `addScheduleExercise(scheduleId, name, phase, sets, reps, holdSeconds, token)`

**Description**: Adds an exercise to a schedule. Usually called by M2.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `scheduleId` | `Int` | Yes | Schedule ID |
| `name` | `String` | Yes | Exercise name |
| `phase` | `String?` | No | Phase: `Warm Up`, `Strength`, `Mobility`, `Cooldown` |
| `sets` | `Int?` | No | Number of sets |
| `reps` | `Int?` | No | Number of repetitions |
| `holdSeconds` | `Int?` | No | Hold duration in seconds, defaults to `0` |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — The created exercise object.

### 8.7 `completeScheduleExercise(scheduleId, exerciseId, painLevel, token)`

**Description**: Marks an exercise in a schedule as completed, optionally including a pain level report.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `scheduleId` | `Int` | Yes | Schedule ID |
| `exerciseId` | `Int` | Yes | Exercise ID |
| `painLevel` | `Int?` | No | Pain level (1-10), optional |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — e.g., `{"exerciseId": 101, "completed": true, "painLevel": 3, "completedAt": "2026-05-03T14:45:00Z"}`.

**Usage Example**:

```kotlin
try {
    val result = v2.completeScheduleExercise(1, 101, painLevel = 3, token)
    Log.i("Exercise", "Completed at: ${result["completedAt"]}")
} catch (e: RuntimeException) {
    // 400: pain level out of range / 404: exercise not found
    Log.e("Exercise", "Complete failed: ${e.message}")
}
```

---

## 9. Progress

### 9.1 `getProgress(userId, token)`

**Description**: Retrieves combined patient progress statistics, including Range of Motion (ROM), adherence, and pain trends.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int` | Yes | Patient ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — Contains `userId`, `generated_at`, `weekLabel`, `rom`, `adherence`, `pain`, `weeklySummary`.

**Usage Example**:

```kotlin
val progress = v2.getProgress(1, token)
val rom = progress["rom"] as Map<String, Any?>
val currentDegrees = (rom["currentDegrees"] as Double).toInt()
val adherence = progress["adherence"] as Map<String, Any?>
val weeklyPercent = (adherence["weeklyPercent"] as Double).toInt()
Log.i("Progress", "ROM: $currentDegrees°, Adherence: $weeklyPercent%")
```

---

## 10. Push Notifications

### 10.1 `registerPushToken(userId, deviceToken, platform, token)`

**Description**: Registers an FCM or APNS device token with V2 so the server can send push notifications to this device.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int` | Yes | Patient ID |
| `deviceToken` | `String` | Yes | FCM or APNS device token |
| `platform` | `String` | Yes | Platform identifier, e.g., `"android"` or `"ios"` |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — Registration result.

**Usage Example**:

```kotlin
FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
    try {
        v2.registerPushToken(userId = 1, deviceToken = fcmToken, platform = "android", token = jwtToken)
        Log.i("Push", "Token registered")
    } catch (e: RuntimeException) {
        Log.e("Push", "Register failed: ${e.message}")
    }
}
```

### 10.2 `getPushTokens(userId, token)`

**Description**: Lists all push tokens registered for a user.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int` | Yes | User ID |
| `token` | `String` | Yes | JWT token |

**Returns**: `List<Map<String, Any?>>` — Array of token records.

---

## 11. Feedback

### 11.1 `getFeedback(status, token)` / `getFeedbackById(id, token)`

**Description**: Retrieves a list of feedback entries (optionally filtered by status) or a single feedback entry.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `status` | `String?` | No | Filter by status |
| `id` | `Int` | Yes | Feedback ID (only for `getFeedbackById`) |
| `token` | `String` | Yes | JWT token |

**Returns**: `List<Map<String, Any?>>` or `Map<String, Any?>`.

### 11.2 `createFeedback(userId, content, token)`

**Description**: Submits new user feedback.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int` | Yes | Submitter ID |
| `content` | `String` | Yes | Feedback content |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — The created feedback object.

**Usage Example**:

```kotlin
val feedback = v2.createFeedback(1, "App crashes when connecting sensor", token)
```

### 11.3 `updateFeedback(id, status, response, token)`

**Description**: Updates a feedback entry's status (e.g., mark as resolved) and adds an admin response.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Int` | Yes | Feedback ID |
| `status` | `String?` | No | New status |
| `response` | `String?` | No | Admin response text |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>` — The updated feedback object.

---

## 12. Announcements

### 12.1 `getAnnouncements(token)`

**Description**: Retrieves the list of system announcements.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `token` | `String` | Yes | JWT token |

**Returns**: `List<Map<String, Any?>>`.

### 12.2 `createAnnouncement(title, content, createdBy, status, token)`

**Description**: Creates a new announcement. Admin or M2 use.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `title` | `String` | Yes | Title |
| `content` | `String` | Yes | Content |
| `createdBy` | `String` | Yes | Creator identifier |
| `status` | `String?` | No | Status |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>`.

### 12.3 `updateAnnouncement(id, title, content, createdBy, status, token)`

**Description**: Updates an announcement.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Int` | Yes | Announcement ID |
| `title` | `String?` | No | New title |
| `content` | `String?` | No | New content |
| `createdBy` | `String?` | No | New creator |
| `status` | `String?` | No | New status |
| `token` | `String` | Yes | JWT token |

**Returns**: `Map<String, Any?>`.

### 12.4 `deleteAnnouncement(id, token)`

**Description**: Deletes an announcement.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Int` | Yes | Announcement ID |
| `token` | `String` | Yes | JWT token |

**Returns**: None (`Unit`).

---

## 13. Audit Logs

### 13.1 `getAuditLogs(userId, action, targetType, token)`

**Description**: Queries admin audit logs with multi-dimensional filtering.

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `Int?` | No | Operator user ID |
| `action` | `String?` | No | Action type |
| `targetType` | `String?` | No | Target type |
| `token` | `String` | Yes | JWT token |

**Returns**: `List<Map<String, Any?>>` — Array of audit records.

**Usage Example**:

```kotlin
val logs = v2.getAuditLogs(userId = 1, action = "DELETE", targetType = "session", token)
```

---

## 14. WebSocket Real-Time Feedback

### 14.1 `connectWebSocket(sessionId, token, listener)`

**Description**: Establishes a WebSocket connection to receive real-time motion feedback events pushed by V2 during an active workout (e.g., `movement_feedback`, `session_ended`).

**Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | `Int` | Yes | Current workout session ID |
| `token` | `String` | Yes | JWT token for authentication |
| `listener` | `WebSocketListener` | Yes | OkHttp WebSocket callback listener |

**Returns**: `WebSocket` — OkHttp WebSocket instance, which can be used to actively close the connection.

**Usage Example**:

```kotlin
val listener = object : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.i("WS", "Connected to real-time feedback")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val event = gson.fromJson(text, JsonObject::class.java)
        when (event.get("type").asString) {
            "connected" -> {
                val sessionId = event.getAsJsonObject("data").get("sessionId").asInt
                Log.i("WS", "Session confirmed: $sessionId")
            }
            "movement_feedback" -> {
                val data = event.getAsJsonObject("data")
                val isCorrect = data.get("isCorrect").asBoolean
                val joint = data.get("joint").asString
                val angle = data.get("angle").asDouble
                updateRealtimeUI(joint, angle, isCorrect)
            }
            "session_ended" -> {
                Log.i("WS", "Session ended by server")
                webSocket.close(1000, "Client closing")
            }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("WS", "WebSocket error: ${t.message}")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
    }
}

val ws = v2.connectWebSocket(sessionId = 1, token = jwtToken, listener = listener)

// Close actively when workout ends
ws.close(1000, "Exercise finished")
```

**Event Types**:

| `type` | Trigger | `data` Content |
|---|---|---|
| `connected` | On connect | `{ "sessionId": 1 }` |
| `movement_feedback` | After each measurement upload | `{ "sessionId", "timestamp", "isCorrect", "joint", "angle" }` |
| `session_ended` | Session closed | `{ "sessionId", "timestamp" }` |

---

## 15. Exception Handling & Best Practices

### 15.1 Unified Exceptions

All methods throw `RuntimeException` in the following cases:
- Network unreachable (DNS failure, connection timeout, no connectivity)
- HTTP status >= 400 (e.g., 400 Bad Request, 401 Unauthorized, 404 Not Found, 409 Conflict)
- Response body cannot be parsed as JSON

The error message prioritizes the `error` field returned by V2; otherwise falls back to `HTTP {code}`.

### 15.2 Recommended Call Pattern

```kotlin
class V2Repository(private val v2: V2ApiClient) {
    suspend fun safeLogin(email: String, password: String): Result<Map<String, Any?>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(v2.login(email, password))
            } catch (e: RuntimeException) {
                Result.failure(e)
            }
        }
}
```

### 15.3 Token Management

- After successful login/registration, store the `token` in Android Keystore or EncryptedSharedPreferences.
- Every authenticated method must receive a valid token before calling.
- On receiving 401, redirect to the login screen to re-authenticate.

### 15.4 PayloadConverter Usage Guidelines

- All POST/PATCH request bodies sent to V2 **should be generated via `PayloadConverter`** to ensure field naming (camelCase) and date formatting (ISO 8601) comply with V2 specifications.
- S2 `FormatData` is directly converted via `PayloadConverter.formatDataToPayload()` for measurement uploads.
- When manually constructing Maps, date strings must be converted using `PayloadConverter.msToIso(timestampMs)`.

---

*Document Version: v2.0 | Covers all V2ApiClient.kt endpoints | Base URL: `http://113.44.220.94:3000`*
