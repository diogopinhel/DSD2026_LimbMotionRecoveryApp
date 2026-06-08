# V2ApiClient 接口文档

> **Base URL**: `http://113.44.220.94:3000`  
> **语言**: Kotlin  
> **HTTP 客户端**: OkHttp + Gson  
> **数据转换**: 所有请求体均通过 `PayloadConverter` 构造，响应统一解析为 `Map<String, Any?>` 或 `List<Map<String, Any?>>`。  
> **异常**: 所有接口在请求失败时抛出 `RuntimeException`，错误信息包含 HTTP 状态码或 V2 返回的 `error` 字段。

---

## 快速参考表

下表列出所有可用接口、一行调用示例及核心功能。点击接口名可跳转至详细说明。

| 接口方法 | 示例调用 | 功能简述 |
|---|---|---|
| `healthCheck` | `v2.healthCheck()` | 检查后端服务是否存活 |
| `register` | `v2.register(name, email, password)` | 患者注册新账户并自动获取 Token |
| `login` | `v2.login(email, password)` | 用户登录，返回 JWT Token |
| `getMe` | `v2.getMe(token)` | 获取当前登录用户的完整信息 |
| `getAuthStatus` | `v2.getAuthStatus(token)` | 获取当前用户的角色与审批状态 |
| `approveUser` | `v2.approveUser(userId, token)` | 管理员批准待审批的医生账户 |
| `rejectUser` | `v2.rejectUser(userId, token)` | 管理员拒绝待审批的医生账户 |
| `getUsers` | `v2.getUsers(role, token)` | 查询用户列表，可按角色筛选 |
| `getUser` | `v2.getUser(id, token)` | 查询指定用户详情（常用于验证医生ID） |
| `createUser` | `v2.createUser(name, email, role, token)` | 管理员手动创建用户（无密码） |
| `updateUser` | `v2.updateUser(id, doctorId = 5, token)` | 更新用户信息，包括绑定/更换医生 |
| `getUserLicense` | `v2.getUserLicense(id, token)` | 下载医生执照文件（返回 ByteArray） |
| `updateUserLicense` | `v2.updateUserLicense(id, file, token)` | 上传或替换医生执照文件 |
| `getPatients` | `v2.getPatients(token)` | 获取所有患者列表 |
| `getPatient` | `v2.getPatient(id, token)` | 获取指定患者详情 |
| `createSession` | `v2.createSession(userId, token)` | 创建新的康复训练会话（Session） |
| `getSessions` | `v2.getSessions(userId, token)` | 获取某用户的所有会话列表 |
| `getSession` | `v2.getSession(sessionId, token)` | 获取单个会话详情（含嵌套测量数据） |
| `endSession` | `v2.endSession(sessionId, token)` | 结束指定的活跃会话 |
| `deleteSession` | `v2.deleteSession(sessionId, token)` | 删除会话及其级联的测量与推荐数据 |
| `uploadMeasurement` | `v2.uploadMeasurement(payload, token)` | 上传单条测量数据（角度、传感器、错误） |
| `uploadMeasurementsBatch` | `v2.uploadMeasurementsBatch(sessionId, measurements, token)` | 批量上传多条测量数据 |
| `uploadRawMeasurement` | `v2.uploadRawMeasurement(payload, token)` | 上传 S2 原始格式的测量数据 |
| `getMeasurements` | `v2.getMeasurements(sessionId, token)` | 获取某会话下的所有测量数据 |
| `getEngineRecommendations` | `v2.getEngineRecommendations(userId, token)` | 获取 AI 引擎基于近10次会话的评估建议 |
| `getSessionRecommendations` | `v2.getSessionRecommendations(sessionId, token)` | 获取某次会话的推荐记录 |
| `createRecommendation` | `v2.createRecommendation(sessionId, movement, confidence, token)` | 创建一条康复推荐记录 |
| `updateRecommendation` | `v2.updateRecommendation(id, status, token)` | 更新推荐状态（pending/accepted/rejected） |
| `getSchedule` | `v2.getSchedule(userId, token)` | 获取患者的康复日程（Schedule）列表 |
| `createSchedule` | `v2.createSchedule(userId, exercise, date, token)` | 创建新的康复日程 |
| `updateSchedule` | `v2.updateSchedule(scheduleId, status, token)` | 更新日程信息（如标记为 completed） |
| `deleteSchedule` | `v2.deleteSchedule(scheduleId, token)` | 删除指定日程 |
| `getScheduleExercises` | `v2.getScheduleExercises(scheduleId, token)` | 获取日程下的具体运动（Exercise）列表 |
| `addScheduleExercise` | `v2.addScheduleExercise(scheduleId, name, token)` | 为日程添加一项运动 |
| `completeScheduleExercise` | `v2.completeScheduleExercise(scheduleId, exerciseId, token)` | 标记日程中的某项运动已完成 |
| `getProgress` | `v2.getProgress(userId, token)` | 获取患者综合进度统计（ROM、依从性、疼痛） |
| `registerPushToken` | `v2.registerPushToken(userId, deviceToken, platform, token)` | 注册 FCM/APNS 推送设备 Token |
| `getPushTokens` | `v2.getPushTokens(userId, token)` | 查询用户的已注册推送 Token |
| `getFeedback` | `v2.getFeedback(status, token)` | 获取用户反馈列表 |
| `getFeedbackById` | `v2.getFeedbackById(id, token)` | 获取单条反馈详情 |
| `createFeedback` | `v2.createFeedback(userId, content, token)` | 提交新的用户反馈 |
| `updateFeedback` | `v2.updateFeedback(id, status, response, token)` | 更新反馈状态并添加管理员回复 |
| `getAnnouncements` | `v2.getAnnouncements(token)` | 获取系统公告列表 |
| `createAnnouncement` | `v2.createAnnouncement(title, content, createdBy, token)` | 创建新公告 |
| `updateAnnouncement` | `v2.updateAnnouncement(id, title, content, token)` | 更新公告内容 |
| `deleteAnnouncement` | `v2.deleteAnnouncement(id, token)` | 删除指定公告 |
| `getAuditLogs` | `v2.getAuditLogs(userId, action, token)` | 查询管理员审计日志 |
| `connectWebSocket` | `v2.connectWebSocket(sessionId, token, listener)` | 建立 WebSocket 连接，接收实时运动反馈 |

---

## 1. 健康检查 (Health)

### 1.1 `healthCheck()`

**功能**：检查 V2 后端服务是否在线，常用于应用启动时的网络连通性探测。

**参数**：无。

**返回值**：`Map<String, Any?>` — 通常包含状态字段，例如 `{"status": "ok"}`。

**使用示例**：

```kotlin
try {
    val response = v2.healthCheck()
    Log.i("Health", "Server status: $response")
} catch (e: RuntimeException) {
    Log.e("Health", "Server unreachable: ${e.message}")
}
```

---

## 2. 认证 (Auth)

### 2.1 `register(name, email, password, role)`

**功能**：注册新用户。患者（patient）可直接注册并立即获得 Token；医生（clinician）注册后进入 pending 状态，需管理员审批。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | `String` | 是 | 用户姓名 |
| `email` | `String` | 是 | 邮箱地址，唯一标识 |
| `password` | `String` | 是 | 登录密码 |
| `role` | `String` | 否 | 角色，默认为 `"patient"`；可选 `"clinician"` |

**返回值**：`Map<String, Any?>` — 患者注册成功时返回 `{"token": "jwt...", "user": {...}}`；医生注册返回 `{"userId": 3, "status": "pending"}`。

**使用示例**：

```kotlin
try {
    val result = v2.register("Ana Costa", "ana@utad.pt", "123456", "patient")
    val token = result["token"] as String
    val user = result["user"] as Map<String, Any?>
    Log.i("Auth", "Registered, token: $token, userId: ${user["id"]}")
} catch (e: RuntimeException) {
    Log.e("Auth", "Register failed: ${e.message}") // 400: 字段缺失 / 409: 邮箱已存在
}
```

### 2.2 `login(email, password)`

**功能**：使用邮箱和密码登录，获取 JWT Token。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `email` | `String` | 是 | 注册邮箱 |
| `password` | `String` | 是 | 密码 |

**返回值**：`Map<String, Any?>` — 结构同 `register`，包含 `token` 和 `user` 对象。

**使用示例**：

```kotlin
try {
    val result = v2.login("ana@utad.pt", "123456")
    val token = result["token"] as String
    val user = result["user"] as Map<String, Any?>
    val doctorId = user["doctor_id"] as? Double ?: 0.0
    Log.i("Auth", "Login OK, doctorId: ${doctorId.toInt()}")
} catch (e: RuntimeException) {
    Log.e("Auth", "Login failed: ${e.message}") // 401: 密码错误 / 404: 用户不存在
}
```

### 2.3 `getMe(token)`

**功能**：获取当前 Token 对应的用户完整信息，常用于登录后加载主页数据或检查 doctor_id 绑定状态。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `token` | `String` | 是 | JWT Token，从 `login` 或 `register` 获取 |

**返回值**：`Map<String, Any?>` — 用户对象，包含 `id`, `name`, `email`, `role`, `doctor_id`, `age`, `condition_label`, `currentRomDegrees`, `adherencePercent`, `streakWeeks` 等字段。

**使用示例**：

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

**功能**：获取当前用户的角色与审批状态，用于判断医生是否已通过审批。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 例如 `{"userId": 1, "role": "patient", "status": "active"}`。

**使用示例**：

```kotlin
val status = v2.getAuthStatus(token)
val userStatus = status["status"] as String
if (userStatus == "pending") {
    showPendingApprovalDialog()
}
```

### 2.5 `approveUser(userId, token)` / `rejectUser(userId, token)`

**功能**：管理员专用。批准或拒绝处于 `pending` 状态的医生账户。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int` | 是 | 待审批的用户ID |
| `token` | `String` | 是 | 管理员 JWT Token |

**返回值**：`Map<String, Any?>` — 更新后的用户状态，例如 `{"userId": 3, "role": "clinician", "status": "active"}`。

**使用示例**：

```kotlin
try {
    v2.approveUser(3, adminToken)
    Log.i("Admin", "Clinician approved")
} catch (e: RuntimeException) {
    Log.e("Admin", "Approval failed: ${e.message}") // 404: 用户不存在 / 409: 非 pending 状态
}
```

---

## 3. 用户与医生绑定 (Users & Doctor Binding)

### 3.1 `getUsers(role, token)`

**功能**：获取用户列表，支持按角色筛选（`patient` 或 `clinician`）。M1 常用于验证医生ID时查询所有医生。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `role` | `String?` | 否 | 筛选角色，传 `"clinician"` 获取医生列表，传 `null` 获取全部 |
| `token` | `String` | 是 | JWT Token |

**返回值**：`List<Map<String, Any?>>` — 用户对象数组。

**使用示例**：

```kotlin
val clinicians = v2.getUsers(role = "clinician", token)
clinicians.forEach { doc ->
    Log.d("Doctors", "${doc["id"]}: ${doc["name"]}")
}
```

### 3.2 `getUser(id, token)`

**功能**：获取指定用户详情。M1 在注册或更换医生时调用此接口验证 `doctorId` 是否真实存在且角色为 `clinician`。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Int` | 是 | 用户ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 用户详情，含 `id`, `name`, `email`, `role`, `doctor_id`, `session_count`。

**使用示例**：

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

**功能**：管理员手动创建用户（无密码），通常用于后台管理或 M2 医生端操作。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | `String` | 是 | 姓名 |
| `email` | `String` | 是 | 邮箱 |
| `role` | `String` | 是 | 角色 |
| `age` | `Int?` | 否 | 年龄 |
| `doctorId` | `Int?` | 否 | 绑定医生ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 创建后的用户对象。

**使用示例**：

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

**功能**：更新用户信息。M1 最核心的用法是**绑定/更换医生**（通过 `doctorId` 参数）。只传需要修改的字段，其余保持 `null` 即可。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Int` | 是 | 要更新的用户ID |
| `name` | `String?` | 否 | 新姓名 |
| `age` | `Int?` | 否 | 新年龄 |
| `role` | `String?` | 否 | 新角色 |
| `status` | `String?` | 否 | 状态 |
| `conditionLabel` | `String?` | 否 | 病情标签 |
| `conditionDate` | `String?` | 否 | 病情日期（ISO 8601） |
| `doctorId` | `Int?` | 否 | **绑定/更换医生ID**；传 `0` 表示解绑 |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 更新后的完整用户对象。

**使用示例**：

```kotlin
// 注册后绑定医生
try {
    val updated = v2.updateUser(id = 1, doctorId = 5, token = token)
    Log.i("Bind", "Bound to doctor: ${updated["doctor_id"]}")
} catch (e: RuntimeException) {
    // 403: 目标不是 clinician / 404: 用户不存在 / 409: 已绑定同一医生
    Log.e("Bind", "Binding failed: ${e.message}")
}

// 在设置中更换医生
try {
    val updated = v2.updateUser(id = 1, doctorId = 8, token = token)
    showSnackbar("Doctor updated successfully")
} catch (e: RuntimeException) {
    showError(e.message ?: "Update failed")
}
```

### 3.5 `getUserLicense(id, token)`

**功能**：下载指定医生的执照文件。返回原始字节数组，可用于本地存储或展示。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Int` | 是 | 医生用户ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`ByteArray` — 文件二进制内容。

**使用示例**：

```kotlin
try {
    val bytes = v2.getUserLicense(5, token)
    val file = File(context.cacheDir, "license_5.pdf")
    file.writeBytes(bytes)
    openPdfViewer(file)
} catch (e: RuntimeException) {
    Log.e("License", "Download failed: ${e.message}") // 404: 无执照文件
}
```

### 3.6 `updateUserLicense(id, licenseFile, token)`

**功能**：上传或替换医生的执照文件。使用 `multipart/form-data` 格式提交。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Int` | 是 | 医生用户ID |
| `licenseFile` | `File` | 是 | 执照文件（PDF 或图片） |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 更新结果。

**使用示例**：

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

## 4. 患者 (Patients)

### 4.1 `getPatients(token)` / `getPatient(id, token)`

**功能**：获取患者列表或单个患者详情。返回对象与 `/users` 一致，但 `role` 固定为 `patient`，并包含 `session_count` 和 `doctor_id`。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `token` | `String` | 是 | JWT Token |
| `id` | `Int` | 是 | 患者ID（仅 `getPatient`） |

**返回值**：`List<Map<String, Any?>>` 或 `Map<String, Any?>`。

**使用示例**：

```kotlin
val patients = v2.getPatients(token)
val target = patients.find { (it["id"] as Double).toInt() == 1 }
```

---

## 5. 康复会话 (Sessions)

### 5.1 `createSession(userId, token)`

**功能**：创建新的康复训练会话。训练开始前必须调用，获取 `sessionId` 后传递给 S2 和后续测量上传接口。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int` | 是 | 患者用户ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 新创建的会话对象，含 `id`, `user_id`, `user_name`, `started_at`, `ended_at`。

**使用示例**：

```kotlin
try {
    val session = v2.createSession(userId = 1, token = token)
    val sessionId = (session["id"] as Double).toInt()
    // 将 sessionId 传递给 S2 开始采集
    s2.session.start("session-$sessionId", "patient-1", sensorMap, "bend_knee_10")
} catch (e: RuntimeException) {
    Log.e("Session", "Create failed: ${e.message}")
}
```

### 5.2 `getSessions(userId, token)`

**功能**：获取指定用户的所有会话列表，用于历史记录页面展示。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int?` | 否 | 筛选用户ID，传 `null` 获取全部（需权限） |
| `token` | `String` | 是 | JWT Token |

**返回值**：`List<Map<String, Any?>>` — 会话摘要数组，每项含 `id`, `user_id`, `started_at`, `ended_at`, `measurement_count`。

**使用示例**：

```kotlin
val sessions = v2.getSessions(userId = 1, token = token)
sessions.forEach { s ->
    val id = (s["id"] as Double).toInt()
    val count = (s["measurement_count"] as Double).toInt()
    Log.d("History", "Session $id has $count measurements")
}
```

### 5.3 `getSession(sessionId, token)`

**功能**：获取单个会话的完整详情，包括嵌套的测量数据（`measurements` 数组）。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sessionId` | `Int` | 是 | 会话ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 完整会话对象，含 `measurements` 数组，每项测量含 `target_angles`, `joint_angles`, `errors`, `sensor_data`, `is_correct`。

**使用示例**：

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

**功能**：结束指定的活跃会话。调用后 `ended_at` 字段被设置为当前时间，后续不能再向该会话上传测量数据。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sessionId` | `Int` | 是 | 要结束的会话ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 更新后的会话对象，`ended_at` 为 ISO 8601 时间字符串。

**使用示例**：

```kotlin
try {
    val ended = v2.endSession(sessionId = 1, token = token)
    Log.i("Session", "Ended at: ${ended["ended_at"]}")
} catch (e: RuntimeException) {
    // 404: 会话不存在 / 409: 已结束
    Log.e("Session", "End failed: ${e.message}")
}
```

### 5.5 `deleteSession(sessionId, token)`

**功能**：删除会话及其级联数据（测量、推荐）。成功时返回 HTTP 204，无响应体。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sessionId` | `Int` | 是 | 要删除的会话ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：无（`Unit`）。失败时抛出异常。

**使用示例**：

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

## 6. 测量数据 (Measurements)

### 6.1 `uploadMeasurement(payload, token)`

**功能**：上传单条测量数据到指定会话。请求体需通过 `PayloadConverter` 构造，或手动构建符合 V2 格式的 Map。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `payload` | `Map<String, Any>` | 是 | 测量数据，必须含 `sessionId`；可选 `targetAngles`, `sensorData`, `errors` |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 存储后的测量对象。

**使用示例**：

```kotlin
// 方式1：使用 PayloadConverter 转换 S2 数据
val formatData: FormatData = s2.data.read()
val payload = PayloadConverter.formatDataToPayload(formatData)
val stored = v2.uploadMeasurement(payload, token)

// 方式2：手动构建
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

**功能**：批量上传多条测量数据，减少网络请求次数。适用于训练结束后一次性补发缓存数据。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sessionId` | `Int` | 是 | 会话ID |
| `measurements` | `List<Map<String, Any>>` | 是 | 测量数据数组，每项含 `targetAngles`（或 `jointAngles`）, `sensorData`, `errors` |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 例如 `{"inserted": 2, "sessionId": 1}`。

**使用示例**：

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

**功能**：上传 S2 原始格式的测量数据。与 `uploadMeasurement` 端点不同，但请求体格式相同。

**参数**：同 `uploadMeasurement`。

**返回值**：同 `uploadMeasurement`。

**使用示例**：

```kotlin
val rawPayload = PayloadConverter.formatDataToPayload(s2Data)
val stored = v2.uploadRawMeasurement(rawPayload, token)
```

### 6.4 `getMeasurements(sessionId, startDate, endDate, token)`

**功能**：获取某会话下的所有测量数据，支持按日期范围筛选。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sessionId` | `Int` | 是 | 会话ID |
| `startDate` | `String?` | 否 | 起始日期（ISO 8601，如 `2026-05-01T00:00:00Z`） |
| `endDate` | `String?` | 否 | 结束日期 |
| `token` | `String` | 是 | JWT Token |

**返回值**：`List<Map<String, Any?>>` — 测量数据数组。

**使用示例**：

```kotlin
val measurements = v2.getMeasurements(
    sessionId = 1,
    startDate = "2026-05-01T00:00:00Z",
    endDate = "2026-05-03T23:59:59Z",
    token = token
)
```

---

## 7. 推荐与评估 (Recommendations)

### 7.1 `getEngineRecommendations(userId, token)`

**功能**：获取 AI 引擎基于该用户最近 10 次会话的评估建议，用于"查看 AI 评估"功能。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int` | 是 | 患者ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 含 `userId`, `sessions_analysed`, `generated_at`, `suggestions` 数组。`suggestions` 每项含 `joint`, `accuracy_percent`, `total_measurements`, `priority`, `suggestion`。

**使用示例**：

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

**功能**：获取某次特定会话的推荐记录。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sessionId` | `Int` | 是 | 会话ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`List<Map<String, Any?>>` — 推荐记录数组。

### 7.3 `createRecommendation(sessionId, movement, confidence, notes, token)`

**功能**：创建一条康复推荐记录。通常由 M2 医生端或 V2 内部 AI 调用，M1 一般无需主动调用。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sessionId` | `Int` | 是 | 关联会话ID |
| `movement` | `String` | 是 | 动作名称 |
| `confidence` | `Double` | 是 | 置信度（0.0 - 1.0） |
| `notes` | `String?` | 否 | 备注说明 |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 创建的推荐对象。

### 7.4 `updateRecommendation(id, status, token)`

**功能**：更新推荐状态，如患者接受或拒绝某条建议。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Int` | 是 | 推荐记录ID |
| `status` | `String` | 是 | 新状态：`pending`, `accepted`, `rejected` |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 更新后的推荐对象。

**使用示例**：

```kotlin
v2.updateRecommendation(10, "accepted", token)
```

---

## 8. 康复日程 (Schedule)

### 8.1 `getSchedule(userId, token)`

**功能**：获取指定患者的康复日程列表，按日期升序排列。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int` | 是 | 患者ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`List<Map<String, Any?>>` — 日程数组，每项含 `id`, `user_id`, `exercise`, `date`, `duration`, `notes`, `video_url`, `status`, `doctor_name`, `created_at`。

**使用示例**：

```kotlin
val schedule = v2.getSchedule(1, token)
schedule.forEach { item ->
    val status = item["status"] as String
    val videoUrl = item["video_url"] as? String
    Log.d("Schedule", "Exercise: ${item["exercise"]}, Status: $status")
}
```

### 8.2 `createSchedule(userId, exercise, date, duration, notes, videoUrl, status, token)`

**功能**：创建新的康复日程。通常由 M2 医生端调用，M1 仅展示。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int` | 是 | 患者ID |
| `exercise` | `String` | 是 | 运动名称（如 `"squat"`） |
| `date` | `String` | 是 | 日程日期（ISO 8601） |
| `duration` | `Int?` | 否 | 预计时长（分钟） |
| `notes` | `String?` | 否 | 任务描述/注意事项 |
| `videoUrl` | `String?` | 否 | 演示视频 URL |
| `status` | `String?` | 否 | 初始状态，默认 `pending` |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 创建的日程对象。

### 8.3 `updateSchedule(scheduleId, exercise, date, duration, notes, videoUrl, status, token)`

**功能**：更新日程信息。M1 最常用的场景是**标记日程完成**（传 `status = "completed"`）。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `scheduleId` | `Int` | 是 | 日程ID |
| `exercise` | `String?` | 否 | 新运动名称 |
| `date` | `String?` | 否 | 新日期 |
| `duration` | `Int?` | 否 | 新时长 |
| `notes` | `String?` | 否 | 新描述 |
| `videoUrl` | `String?` | 否 | 新视频 URL |
| `status` | `String?` | 否 | 新状态：`pending`, `completed`, `skipped` |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 更新后的完整日程对象。

**使用示例**：

```kotlin
// 标记日程完成
try {
    val updated = v2.updateSchedule(scheduleId = 1, status = "completed", token = token)
    Log.i("Schedule", "Status updated to: ${updated["status"]}")
} catch (e: RuntimeException) {
    // 400: 无效状态 / 404: 日程不存在 / 409: 已完成
    Log.e("Schedule", "Update failed: ${e.message}")
}
```

### 8.4 `deleteSchedule(scheduleId, token)`

**功能**：删除日程及其关联运动。成功返回 HTTP 204。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `scheduleId` | `Int` | 是 | 日程ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：无（`Unit`）。

### 8.5 `getScheduleExercises(scheduleId, token)`

**功能**：获取某日程下的具体运动列表（Plan Details 页面）。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `scheduleId` | `Int` | 是 | 日程ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 含日程基本信息 + `exercises` 数组。每项运动含 `id`, `name`, `phase`, `sets`, `reps`, `holdSeconds`, `completed`, `lastPainLevel`。

**使用示例**：

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

**功能**：为日程添加一项运动。通常由 M2 调用。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `scheduleId` | `Int` | 是 | 日程ID |
| `name` | `String` | 是 | 运动名称 |
| `phase` | `String?` | 否 | 阶段：`Warm Up`, `Strength`, `Mobility`, `Cooldown` |
| `sets` | `Int?` | 否 | 组数 |
| `reps` | `Int?` | 否 | 次数 |
| `holdSeconds` | `Int?` | 否 | 保持秒数，默认 `0` |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 创建的运动对象。

### 8.7 `completeScheduleExercise(scheduleId, exerciseId, painLevel, token)`

**功能**：标记日程中的某项运动已完成，可附带疼痛等级反馈。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `scheduleId` | `Int` | 是 | 日程ID |
| `exerciseId` | `Int` | 是 | 运动ID |
| `painLevel` | `Int?` | 否 | 疼痛等级（1-10），可选 |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 例如 `{"exerciseId": 101, "completed": true, "painLevel": 3, "completedAt": "2026-05-03T14:45:00Z"}`。

**使用示例**：

```kotlin
try {
    val result = v2.completeScheduleExercise(1, 101, painLevel = 3, token)
    Log.i("Exercise", "Completed at: ${result["completedAt"]}")
} catch (e: RuntimeException) {
    // 400: 疼痛等级越界 / 404: 运动不存在
    Log.e("Exercise", "Complete failed: ${e.message}")
}
```

---

## 9. 进度统计 (Progress)

### 9.1 `getProgress(userId, token)`

**功能**：获取患者的综合进度统计，包括关节活动度（ROM）、训练依从性（Adherence）、疼痛趋势（Pain）。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int` | 是 | 患者ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 含 `userId`, `generated_at`, `weekLabel`, `rom`, `adherence`, `pain`, `weeklySummary`。

**使用示例**：

```kotlin
val progress = v2.getProgress(1, token)
val rom = progress["rom"] as Map<String, Any?>
val currentDegrees = (rom["currentDegrees"] as Double).toInt()
val adherence = progress["adherence"] as Map<String, Any?>
val weeklyPercent = (adherence["weeklyPercent"] as Double).toInt()
Log.i("Progress", "ROM: $currentDegrees°, Adherence: $weeklyPercent%")
```

---

## 10. 推送通知 (Push)

### 10.1 `registerPushToken(userId, deviceToken, platform, token)`

**功能**：将 FCM 或 APNS 获取的设备 Token 注册到 V2，使服务端可以向该设备发送推送通知。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int` | 是 | 患者ID |
| `deviceToken` | `String` | 是 | FCM 或 APNS 设备 Token |
| `platform` | `String` | 是 | 平台标识，如 `"android"` 或 `"ios"` |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 注册结果。

**使用示例**：

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

**功能**：查询某用户已注册的所有推送 Token。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int` | 是 | 用户ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：`List<Map<String, Any?>>` — Token 记录列表。

---

## 11. 反馈 (Feedback)

### 11.1 `getFeedback(status, token)` / `getFeedbackById(id, token)`

**功能**：获取反馈列表（可按状态筛选）或单条反馈详情。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `status` | `String?` | 否 | 筛选状态 |
| `id` | `Int` | 是 | 反馈ID（仅 `getFeedbackById`） |
| `token` | `String` | 是 | JWT Token |

**返回值**：`List<Map<String, Any?>>` 或 `Map<String, Any?>`。

### 11.2 `createFeedback(userId, content, token)`

**功能**：提交用户反馈。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int` | 是 | 提交者ID |
| `content` | `String` | 是 | 反馈内容 |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 创建的反馈对象。

**使用示例**：

```kotlin
val feedback = v2.createFeedback(1, "App crashes when connecting sensor", token)
```

### 11.3 `updateFeedback(id, status, response, token)`

**功能**：更新反馈状态（如标记为已处理）并添加管理员回复。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Int` | 是 | 反馈ID |
| `status` | `String?` | 否 | 新状态 |
| `response` | `String?` | 否 | 管理员回复内容 |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>` — 更新后的反馈对象。

---

## 12. 公告 (Announcements)

### 12.1 `getAnnouncements(token)`

**功能**：获取系统公告列表。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `token` | `String` | 是 | JWT Token |

**返回值**：`List<Map<String, Any?>>`。

### 12.2 `createAnnouncement(title, content, createdBy, status, token)`

**功能**：创建新公告。管理员或 M2 使用。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `title` | `String` | 是 | 标题 |
| `content` | `String` | 是 | 内容 |
| `createdBy` | `String` | 是 | 创建者标识 |
| `status` | `String?` | 否 | 状态 |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>`。

### 12.3 `updateAnnouncement(id, title, content, createdBy, status, token)`

**功能**：更新公告内容。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Int` | 是 | 公告ID |
| `title` | `String?` | 否 | 新标题 |
| `content` | `String?` | 否 | 新内容 |
| `createdBy` | `String?` | 否 | 新创建者 |
| `status` | `String?` | 否 | 新状态 |
| `token` | `String` | 是 | JWT Token |

**返回值**：`Map<String, Any?>`。

### 12.4 `deleteAnnouncement(id, token)`

**功能**：删除公告。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Int` | 是 | 公告ID |
| `token` | `String` | 是 | JWT Token |

**返回值**：无（`Unit`）。

---

## 13. 审计日志 (Audit Logs)

### 13.1 `getAuditLogs(userId, action, targetType, token)`

**功能**：查询管理员操作审计日志，支持多维度筛选。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Int?` | 否 | 操作用户ID |
| `action` | `String?` | 否 | 操作类型 |
| `targetType` | `String?` | 否 | 目标类型 |
| `token` | `String` | 是 | JWT Token |

**返回值**：`List<Map<String, Any?>>` — 审计记录数组。

**使用示例**：

```kotlin
val logs = v2.getAuditLogs(userId = 1, action = "DELETE", targetType = "session", token)
```

---

## 14. WebSocket 实时反馈

### 14.1 `connectWebSocket(sessionId, token, listener)`

**功能**：建立 WebSocket 连接，接收 V2 在训练过程中推送的实时运动反馈事件（如 `movement_feedback`, `session_ended`）。

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sessionId` | `Int` | 是 | 当前训练会话ID |
| `token` | `String` | 是 | JWT Token，用于鉴权 |
| `listener` | `WebSocketListener` | 是 | OkHttp 的 WebSocket 回调监听器 |

**返回值**：`WebSocket` — OkHttp WebSocket 实例，可用于主动关闭连接。

**使用示例**：

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

// 训练结束时主动关闭
ws.close(1000, "Exercise finished")
```

**事件类型说明**：

| `type` | 触发时机 | `data` 内容 |
|---|---|---|
| `connected` | 连接成功 | `{ "sessionId": 1 }` |
| `movement_feedback` | 每次测量上传后 | `{ "sessionId", "timestamp", "isCorrect", "joint", "angle" }` |
| `session_ended` | 会话被关闭 | `{ "sessionId", "timestamp" }` |

---

## 15. 异常处理与最佳实践

### 15.1 统一异常

所有接口在以下情况抛出 `RuntimeException`：
- 网络不可达（DNS 失败、连接超时、无网络）
- HTTP 状态码 >= 400（如 400 Bad Request, 401 Unauthorized, 404 Not Found, 409 Conflict）
- 响应体无法解析为 JSON

异常消息优先使用 V2 返回的 `error` 字段，否则回退到 `HTTP {code}`。

### 15.2 建议的调用模式

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

### 15.3 Token 管理

- 登录/注册成功后，将 `token` 保存到 Android Keystore 或 EncryptedSharedPreferences。
- 每个需要鉴权的接口必须在调用前确保 Token 有效。
- 收到 401 时，应跳转登录页重新获取 Token。

### 15.4 PayloadConverter 使用规范

- 所有向 V2 发送的 POST/PATCH 请求体，**优先使用 `PayloadConverter` 生成**，确保字段命名（camelCase）和日期格式（ISO 8601）符合 V2 规范。
- S2 的 `FormatData` 通过 `PayloadConverter.formatDataToPayload()` 直接转换为测量上传格式。
- 手动构建 Map 时，日期字符串必须使用 `PayloadConverter.msToIso(timestampMs)` 转换。

---

*文档版本：v2.0 | 对应 V2ApiClient.kt 全量接口 | Base URL: `http://113.44.220.94:3000`*
