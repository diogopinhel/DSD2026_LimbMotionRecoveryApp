# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

**Limb Motion Recovery App** — Android rehab app (Kotlin/Java), package `com.example.limbmotionrecoveryapp`. Built on top of shared modules (S1, S2, m1/api) ported from the original integration project at `C:\Users\diogo\OneDrive\Ambiente de Trabalho\UTAD\3ºAno\2º Semestre\Trabalho DSD-CHINA\V2 Integration - AndroidStudio`.

Part of a multi-team distributed systems project (UTAD + Jilin University). This app is the **M1 Patient Mobile Application** — the only team that interacts directly with the patient.

**Data flow:** WitMotion BLE sensors → S1 (Java, BLE driver) → S2 (Kotlin, processing) → UI + V2 backend (Kotlin)

## Build & Run

Open in **Android Studio Panda 4+**, sync Gradle, connect an Android 12+ device (minSdk 31), click Run.

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew test
```

Emulators won't work for BLE sessions — use a physical device.

## SDK Versions

| Setting | Value |
|---|---|
| compileSdk | 36 |
| targetSdk | 35 |
| minSdk | 31 (Android 12) |
| AGP | 9.0.0 |
| Kotlin | 1.9.24 |
| Java | 11 |

## Architecture

The app has three layers of copied/shared modules under `app/src/main/java/com/dsd/`, plus the app's own code under `com/example/limbmotionrecoveryapp/`.

### S1 — BLE Sensor Driver (Java, do not modify)
`com.dsd.s1` — owned by the S1 team in the original project. Copy-only, never modify.

- `SensorService`: manages N `SensorWorker` threads (one per MAC address). Call `initialize()` then `startSensors()` / `stopSensors()`. Read samples via `readSamples()`, status via `getStatus()`.
- `SensorWorker`: GATT connection → service discovery → notification subscription → parses frames via `WitMotionParser`
- `WitMotionParser`: WitMotion binary protocol — BLE IMU frame (0x61, 20 bytes) and serial frames (0x50–0x5F, 11 bytes). Checksum = 8-bit sum of first 10 bytes.
- `SensorState`: per-sensor state machine with `ConcurrentLinkedQueue<SensorSample>`
- **GATT characteristic UUID:** `0000ffe4-0000-1000-8000-00805f9a34fb`

### S2 — Data Processing (Kotlin, do not modify)
`com.dsd.s2` — shared processing layer.

- `S2Module`: public facade. `s2.session.start(...)` / `s2.session.stop()` / `s2.data.read()`
- `DataAcqCore`: polls S1 at 50 Hz (20 ms coroutine loop), validates samples, computes angles, routes to `AsyncBuffer`
- `JointAngleComputer`: two sensors → one joint angle via dot product of unit vectors. Bind mode: `BINDMODE_BACK` uses screen-normal (roll+pitch); `BINDMODE_PORT`/`BINDMODE_SCREEN` use long-edge (roll+pitch+yaw)
- `AsyncBuffer`: thread-safe buffer with per-consumer cursors. Call `drain(consumerId)` to get data since last call.
- `SimulatedS1Module` / `SimulatedSensorService`: sinusoidal 50 Hz generator for testing without hardware
- `S1RealModule`: adapter bridging Java `SensorService` to Kotlin `S1Module` interface

### m1/api — V2 Backend Client (Kotlin, can extend)
`com.dsd.m1.api`

- `V2ApiClient`: OkHttp + Gson client. Base URL: `http://113.44.220.94:3000`. All calls are synchronous — run on a background thread or inside a coroutine with `Dispatchers.IO`.
- `PayloadConverter`: converts `FormatData` (S2 output) → V2 `/measurements/raw` payload (ISO 8601 timestamps)

### App UI — Your code
`com.example.limbmotionrecoveryapp` — build your Activities, Fragments, ViewModels here.

## Cross-Module Interface Contract

Do not add, remove, or rename interface methods in `s2/core/S1Interfaces.kt` or `s2/S2Module.kt`.

| Interface | File | Direction |
|---|---|---|
| IF-S1-S2 | `s2/core/S1Interfaces.kt` | S1 → S2 |
| IF-S2-M1 | `s2/S2Module.kt` | S2 → App |
| IF-M1-V2 | `m1/api/V2ApiClient.kt` | App → V2 backend |

## How to Wire Everything Up

```kotlin
// 1. Configure sensors (real BLE)
val sensorService = SensorService(context)
sensorService.initialize(
    listOf(SensorConfig("AA:BB:CC:DD:EE:FF", "0000ffe4-0000-1000-8000-00805f9a34fb")),
    ServiceConfig()
)
val s1Real = S1RealModule(sensorService)

// 2. Simulator (no hardware needed)
val s1Sim = SimulatedS1Module()

// 3. Build S2 module
val s2 = S2Module(s1RealModule = s1Real, s1SimModule = s1Sim, context = context)

// 4. Switch to sim mode for testing
s2.session.setMode(useSimulator = true)

// 5. Start a session
val v2 = V2ApiClient()
// (on IO thread) login and create session first, get sessionId + token
val result = s2.session.start(
    sessionId = sessionId,
    userId = userId,
    sensorJointMapping = mapOf("AA:BB:CC:DD:EE:FF" to "knee", "11:22:33:44:55:66" to "knee"),
    payloadStatus = "bend_knee_10-back"
)

// 6. Poll data (e.g. every 500 ms for UI)
val data: FormatData = s2.data.read()

// 7. Upload to V2 (on IO thread)
val payload = PayloadConverter.formatDataToPayload(data)
v2.uploadMeasurement(payload, token)

// 8. Stop session
s2.session.stop()
v2.endSession(sessionId, token)
```

## V2 Backend Key Endpoints

Base URL: `http://113.44.220.94:3000` (HTTP only — TLS not yet enabled on the server)

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | Create account |
| POST | `/auth/login` | Returns `token` + `user` |
| GET | `/auth/me` | Get current user |
| POST | `/sessions` | Create session, returns `session_id` |
| PATCH | `/sessions/{id}/end` | Close session |
| POST | `/measurements/raw` | Upload FormatData payload |
| GET | `/measurements/{sessionId}` | Get measurements |
| GET | `/recommendations/session/{sessionId}` | Session recommendations |
| GET | `/recommendations/engine/{userId}` | AI recommendations |
| GET | `/schedule/{userId}` | Get rehab schedule |
| PATCH | `/schedule/{scheduleId}` | Update schedule item |
| POST | `/push/register` | Register FCM push token |

All V2 network calls must run off the main thread. Use `viewModelScope.launch(Dispatchers.IO) { }`.

## Key Behaviours & Invariants

- **Bind modes** (set via `payloadStatus` suffix): `back` uses screen-normal vector; `port`/`screen` use long-edge vector.
- **Validation:** `DataAcqCore.validateSample()` rejects non-finite fields or timestamp ≤ 0.
- **Timeouts:** S1 BLE worker triggers 2 s no-data timeout → `SensorState.markTimeout()`.
- **Auto-reconnect:** `SensorWorker` reconnects after `reconnectDelaySec` (default 2 s).
- **CSV log:** `DataAcqCore` writes angle log to `context.getExternalFilesDir(null)/log/angles_<timestamp>.csv`.
- **Coroutines vs threads:** S2 uses `Dispatchers.IO` coroutines; S1 uses Java threads.
- **Simulator toggle:** Call `s2.session.setMode(useSimulator = true/false)` only when no session is active.

## BLE Permissions

Required at runtime (Android 12+): `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT`. Already declared in `AndroidManifest.xml`. Request them before calling `sensorService.startSensors()`.

## M1 Module Responsibilities (from HLD v1.3)

| Module | What to build |
|---|---|
| MOD-M1-01 | Auth & session management (login, register, token storage) |
| MOD-M1-02 | BLE sensor manager (scan, pair, connect via S1) |
| MOD-M1-03 | Exercise controller (session lifecycle, coordinate S2 + V2) |
| MOD-M1-04 | Real-time feedback UI (joint angle display, deviation alerts) |
| MOD-M1-05 | Data sync & cache (history, offline, rehab plan) |
| MOD-M1-06 | Push notifications (FCM token registration + display) |

## Pending V2 Endpoint Clarifications

These gaps were found during UI implementation and need to be resolved with the V2 team before wiring up real data.

### Plans / Schedule screen (`PlansFragment`)
| # | Question | Endpoint | Priority |
|---|---|---|---|
| 1 | **Response shape of `/schedule/{userId}`** — M1 assumes fields: `id`, `name`, `status` (active/upcoming/completed), `startDate`, `endDate`, `totalSessions`, `completedSessions`, `phases[]`, `doctorName`. If field names differ the parsing fails silently. | `GET /schedule/{userId}` | High |
| 2 | **"Today: X exercises"** shown in the active plan card footer — no known endpoint for this. Need either a `todayExercises` field in the schedule response or a new endpoint. | Unknown | Medium |
| 3 | **Summary strip stats** (active plan count, total sessions, overall % complete) — currently computed client-side from the schedule list. If V2 has a dedicated summary endpoint it would be more reliable. | Unknown | Low |
| 4 | **Phase tags per plan** — assumed to be a `phases[]` array in the schedule item. If not present, tags won't appear. | `GET /schedule/{userId}` | Low |
| 5 | **Doctor name per plan** — assumed field `doctorName` in the schedule item. If absent, the doctor row stays blank. | `GET /schedule/{userId}` | Low |
