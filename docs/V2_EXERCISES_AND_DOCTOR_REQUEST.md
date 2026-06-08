# V2 API Request — Exercises & Doctor Assignment

**From:** M1 Team (Diogo Pinhel)  
**Date:** 2026-06-04  
**Priority:** 🔴 Critical — these endpoints block two screens entirely  
**Context:** M1 is the Android patient app. The UI, ViewModels, and data models for the Plans/Exercises screens are fully built on our side. We are blocked waiting for V2 to expose the endpoints described below.

---

## Summary of what is needed

| # | Status | Method | Path | Blocks |
|---|--------|--------|------|--------|
| 1 | ❌ Missing | GET | `/schedule/{scheduleId}/exercises` | Plan Details screen — exercise list |
| 2 | ❌ Missing | PATCH | `/schedule/{scheduleId}/exercises/{exerciseId}/complete` | Marking an exercise as done |
| 3 | ⚠️ Unconfirmed | GET | `/schedule/{userId}` | Plans List — doctor name, phases, today's exercise count |

---

## 1. `GET /schedule/{scheduleId}/exercises`

**This is the most critical missing endpoint.** The Plan Details screen (which shows the list of exercises inside a rehab plan) currently displays a hard-coded error state because this endpoint does not exist.

### Request

```
GET /schedule/{scheduleId}/exercises
Authorization: Bearer <token>
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `scheduleId` | integer (path) | ID of the schedule/plan, obtained from `GET /schedule/{userId}` |

### Required Response Shape

```json
{
  "planId": 1,
  "planName": "Post-Op Knee Recovery — Phase 1",
  "status": "active",
  "progressPercent": 62,
  "completedSessions": 13,
  "totalSessions": 21,
  "startDate": "2024-04-04",
  "endDate": "2024-05-15",
  "estimatedMinutesPerSession": 35,
  "doctorName": "Dr. Ana Rodrigues",
  "exercises": [
    {
      "id": 101,
      "name": "Ankle Pumps",
      "phase": "Warm Up",
      "sets": 3,
      "reps": 20,
      "holdSeconds": 0,
      "completed": false,
      "lastPainLevel": null
    },
    {
      "id": 102,
      "name": "Quadriceps Set",
      "phase": "Strength",
      "sets": 3,
      "reps": 10,
      "holdSeconds": 5,
      "completed": true,
      "lastPainLevel": 4
    },
    {
      "id": 103,
      "name": "Hamstring Stretch",
      "phase": "Cooldown",
      "sets": 2,
      "reps": 1,
      "holdSeconds": 30,
      "completed": false,
      "lastPainLevel": null
    }
  ]
}
```

### Field Specification

**Root object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `planId` | integer | Yes | Same as `scheduleId` in the path |
| `planName` | string | Yes | Human-readable name of the plan |
| `status` | string | Yes | `"active"`, `"upcoming"`, or `"completed"` |
| `progressPercent` | integer (0–100) | Yes | Percentage of sessions completed |
| `completedSessions` | integer | Yes | How many sessions the patient has finished |
| `totalSessions` | integer | Yes | Total sessions in the plan |
| `startDate` | string | Yes | ISO 8601 date: `YYYY-MM-DD` |
| `endDate` | string | Yes | ISO 8601 date: `YYYY-MM-DD` |
| `estimatedMinutesPerSession` | integer | No | Estimated duration per session in minutes. Shown in the UI header. |
| `doctorName` | string | Yes | Full name of the assigned doctor/therapist (e.g. `"Dr. Ana Rodrigues"`). See section 3 for more detail. |
| `exercises` | array | Yes | List of exercise objects. Can be empty `[]` but must not be `null`. |

**Each exercise object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | integer | Yes | Unique ID for this exercise |
| `name` | string | Yes | Display name (e.g. `"Ankle Pumps"`) |
| `phase` | string | Yes | Phase/group label. See allowed values below. |
| `sets` | integer | Yes | Number of sets |
| `reps` | integer | Yes | Repetitions per set |
| `holdSeconds` | integer | Yes | Hold duration in seconds. Use `0` if not applicable (not `null`). |
| `completed` | boolean | Yes | Whether the patient has completed this exercise in the current session |
| `lastPainLevel` | integer or null | Yes | Pain level (1–10) the patient reported last time. `null` if never reported. |

**Allowed values for `phase`:**

The UI renders section headers based on this field. The allowed values are:

| Value | Displayed as |
|-------|-------------|
| `"Warm Up"` | Warm Up |
| `"Strength"` | Strength |
| `"Mobility"` | Mobility |
| `"Cooldown"` or `"Cool Down"` | Cooldown |

Other phase names will still render as a section header with whatever string is provided. The M1 app is flexible here — just be consistent across exercises in the same plan.

### HTTP Error Cases

| Status | When |
|--------|------|
| 401 | Token missing or expired |
| 403 | Token belongs to a different user (patient trying to access another patient's plan) |
| 404 | `scheduleId` does not exist |

Error body must follow the standard V2 format:
```json
{ "error": "Schedule not found" }
```

---

## 2. `PATCH /schedule/{scheduleId}/exercises/{exerciseId}/complete`

Used to mark a single exercise as done when the patient finishes it during a session.

### Request

```
PATCH /schedule/{scheduleId}/exercises/{exerciseId}/complete
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "painLevel": 3
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `painLevel` | integer (1–10) or null | No | Pain reported by the patient after doing the exercise. Can be omitted if the patient skips the pain check. |

### Response

```json
{
  "exerciseId": 102,
  "completed": true,
  "painLevel": 3,
  "completedAt": "2024-04-25T10:45:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `exerciseId` | integer | Echoes the ID |
| `completed` | boolean | Should always be `true` on success |
| `painLevel` | integer or null | Echoes what was sent |
| `completedAt` | string | ISO 8601 timestamp of when it was recorded |

### HTTP Error Cases

| Status | When |
|--------|------|
| 400 | `painLevel` is outside 1–10 |
| 404 | `scheduleId` or `exerciseId` not found |

---

## 3. `GET /schedule/{userId}` — Confirmation of Response Shape

This endpoint already exists. However, M1 is parsing several fields that have not been confirmed in V2's response. Without these fields, parts of the Plans List screen show blank or default values.

### Fields M1 needs confirmed in each schedule item

```json
{
  "id": 1,
  "name": "Post-Op Knee Recovery — Phase 1",
  "status": "active",
  "startDate": "2024-04-04",
  "endDate": "2024-05-15",
  "totalSessions": 21,
  "completedSessions": 13,
  "phases": ["Warm Up", "Strength", "Mobility"],
  "doctorName": "Dr. Ana Rodrigues",
  "todayExercises": 5
}
```

| Field | Type | Status | Impact if missing |
|-------|------|--------|-------------------|
| `id` | integer | Required | Plan cannot be opened (no ID to pass to exercises endpoint) |
| `name` | string | Required | Card shows `"Unnamed plan"` |
| `status` | string | Required | Cards won't filter into Active/Upcoming/Completed |
| `startDate` | string | Required | Dates show blank |
| `endDate` | string | Required | Dates show blank |
| `totalSessions` | integer | Required | Progress bar stays at 0% |
| `completedSessions` | integer | Required | Progress bar stays at 0% |
| `phases` | string array | Needed | Phase tags don't appear on the card |
| `doctorName` | string | Needed | Doctor row shows blank |
| `todayExercises` | integer | Needed | "X exercises today" footer shows 0 |

**`status` must be exactly one of:** `"active"` | `"upcoming"` | `"completed"` (lowercase).

**Note on `doctorName`:** M1 also accepts `doctor_name` (snake_case) as a fallback, but camelCase is strongly preferred for consistency — see section 4.

---

## 4. Doctor Assignment — How M1 Uses It

The doctor/therapist name is shown in two places:

1. **Plans List screen** — small label at the bottom of each plan card ("Dr. Ana Rodrigues")
2. **Plan Details screen** — header row showing the assigned doctor alongside the plan dates

M1 does **not** need the full doctor profile (ID, email, specialty) — only the display name is needed at this stage. If V2 has a separate doctor/therapist object, please flatten just the name into the schedule and exercise responses as `doctorName`.

If a plan has no assigned doctor yet, return an empty string `""` rather than `null`, so M1 doesn't need a null check.

---

## 5. Global Format Rules (reminder)

These apply to all endpoints above:

| Rule | Required format | Why |
|------|----------------|-----|
| Field names | camelCase (`startDate`, not `start_date`) | Gson + manual mapping on M1 side — snake_case causes silent nulls |
| Dates | ISO 8601 `YYYY-MM-DD` | Displayed directly in the UI |
| Timestamps | ISO 8601 `YYYY-MM-DDTHH:MM:SSZ` | Consistent with existing session/measurement endpoints |
| Numeric IDs | integer (not string) | M1 calls `.toInt()` on IDs — fails silently if sent as string |
| Error body | `{ "error": "message" }` | Already parsed by V2ApiClient — other shapes surface as raw HTTP errors |
| Boolean | `true` / `false` (JSON boolean, not `"true"` string) | Direct mapping to Kotlin `Boolean` |
| Empty lists | `[]` (not `null`) | M1 iterates over exercise arrays without null checks |

---

## 6. Implementation Priority

| Priority | Endpoint | Reason |
|----------|----------|--------|
| 🔴 1 | `GET /schedule/{scheduleId}/exercises` | The Plan Details screen shows a hard error state until this exists |
| 🔴 2 | Confirm `GET /schedule/{userId}` response shape (section 3) | Several fields may already exist with different names — confirmation unblocks the Plans List |
| 🟠 3 | `PATCH /schedule/{scheduleId}/exercises/{exerciseId}/complete` | Needed for the session exercise flow |

---

## 7. What M1 Already Has Built

For V2's reference — these are the data models and code already in place on the M1 side, waiting for the endpoints:

**`Exercise` data class** (`screens/plans/Exercise.kt`):
```kotlin
data class Exercise(
    val id: Int,
    val name: String,
    val phase: String,
    val sets: Int,
    val reps: Int,
    val holdSeconds: Int,
    val completed: Boolean,
    val lastPainLevel: Int?   // nullable
)
```

**`Plan` data class** (`screens/plans/Plan.kt`):
```kotlin
data class Plan(
    val id: Int,
    val name: String,
    val status: String,       // "active" | "upcoming" | "completed"
    val startDate: String,
    val endDate: String,
    val totalSessions: Int,
    val completedSessions: Int,
    val phases: List<String>,
    val doctorName: String,
    val todayExercises: Int
)
```

**`V2ApiClient` method to be added** (once V2 confirms the endpoint exists):
```kotlin
fun getPlanExercises(scheduleId: Int, token: String): Map<String, Any?> {
    val req = Request.Builder().url(url("/schedule/$scheduleId/exercises"))
        .header("Authorization", "Bearer $token").get().build()
    return parseResponse(client.newCall(req).execute())
}
```

This method is already stub-commented in `PlanDetailsViewModel.kt` and will be wired up as soon as V2 confirms the endpoint.
