# V2 API Requirements — M1 Team (Patient Mobile App)

**From:** M1 Team (Diogo Pinhel)  
**Date:** 2026-05-13  
**Context:** M1 is the Android patient app. This document lists every API endpoint needed per screen, the expected request/response shapes, and format issues found during implementation.

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Endpoint exists and works |
| ⚠️ | Endpoint exists but response shape is unclear or incomplete |
| ❌ | Endpoint does not exist — needs to be created |

---

## 1. Home Screen

The Home screen shows the patient's current recovery status, sensor connection state, and a quick-start button for today's exercises.

| Status | Method | Path | Notes |
|--------|--------|------|-------|
| ⚠️ | GET | `/schedule/{userId}` | Used to derive active plan name, week number and session counts. Response shape still unconfirmed — see section 2. |
| ❌ | GET | `/progress/{userId}/rom` | **Current ROM degree and weekly gain.** Needed for the "Today at a glance" ROM card. Returns current angle + delta vs last week. |
| ❌ | GET | `/schedule/{userId}/today` | **Count of today's remaining exercises.** Shown in the "Today at a glance" session card. Can also be a `todayExercises` field in the schedule response (see section 2). |
| ❌ | GET | `/appointments/{userId}/next` | **Next appointment card.** Returns the next scheduled appointment (date, time, doctor name, location type). |
| ❌ | POST | `/pain/{userId}/checkin` | **Pain check-in.** Record current pain level (1–10). Used to update the pain card on the home screen. |
| ❌ | GET | `/pain/{userId}/latest` | **Latest pain level.** Retrieves the most recent pain entry and the delta from the previous day. |

**Required fields for `GET /progress/{userId}/rom` (home card variant):**
```json
{
  "currentDegrees": 120,
  "weeklyGainDegrees": 15
}
```

**Required fields for `GET /appointments/{userId}/next`:**
```json
{
  "date": "2024-04-25",
  "time": "10:30",
  "doctorName": "Dr. Ana Rodrigues",
  "locationType": "In Clinic"
}
```

**Required fields for `GET /pain/{userId}/latest`:**
```json
{
  "level": 4,
  "label": "Moderate",
  "changeFromYesterday": -1
}
```

**Notes:**
- The "Today at a glance" glance grid currently shows `completedSessions/totalSessions` and plan progress % from the schedule endpoint as a fallback. Once `/progress/{userId}/rom` and `todayExercises` exist, those will fill in automatically.
- The "Next appointment", "Pain check-in" and "Learn & recover" sections are **hidden** in the current build because their endpoints don't exist yet.
- Sensor data (ROM, movement) for the live card comes from the S2 local module — no V2 endpoint needed for real-time sensor data.

---

## 2. Auth Screens (Login & Register)

| Status | Method | Path | Notes |
|--------|--------|------|-------|
| ✅ | POST | `/auth/register` | Works. Returns user object (no token). M1 calls `/auth/login` immediately after to obtain the token. **If V2 returns the token directly on register, please confirm — it avoids the double call.** |
| ✅ | POST | `/auth/login` | Works. Returns `{ token, user: { id, name, email } }`. |
| ✅ | GET | `/auth/me` | Works. Used to refresh user info. |

**Format issues:**
- `POST /auth/register` — if the response already contains a `token` field, M1 can skip the second login call. Please confirm response shape.

---

## 2. Plans List Screen

| Status | Method | Path | Notes |
|--------|--------|------|-------|
| ⚠️ | GET | `/schedule/{userId}` | Exists but **response shape unknown**. M1 needs the fields below per item. |

**Required fields per schedule item:**

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

**`status` must be one of:** `"active"` | `"upcoming"` | `"completed"`

**Missing data for this screen:**
- ❌ `todayExercises` — number of exercises planned for today. If this doesn't exist in the schedule response, please add it or provide a separate endpoint: `GET /schedule/{userId}/today`.
- ❌ `phases[]` — array of phase names per plan. Please include in the schedule response.
- ❌ `doctorName` — name of the assigned doctor/therapist per plan. Please include in the schedule response.

---

## 3. Plan Details Screen

This screen shows exercises inside a specific plan, grouped by phase (Warm Up / Strength / Cooldown), with a "To Do" and "Completed" tab.

| Status | Method | Path | Notes |
|--------|--------|------|-------|
| ❌ | GET | `/schedule/{scheduleId}/exercises` | **Does not exist. Critical.** Needed to show the exercise list inside a plan. |
| ❌ | PATCH | `/schedule/{scheduleId}/exercises/{exerciseId}/complete` | Mark a single exercise as done after the patient completes it. |

**Required response shape for `GET /schedule/{scheduleId}/exercises`:**

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
      "completed": false,
      "lastPainLevel": 4
    }
  ]
}
```

**Notes:**
- `phase` groups the exercises visually (Warm Up → Strength → Cooldown/Cool Down). The UI renders section headers based on this field.
- `lastPainLevel` (nullable int 1–10) shows the last reported pain for that exercise. Used to display the "Pain X last time" badge.
- `completed` (boolean) determines which tab (To Do vs Completed) the exercise appears in.

---

## 4. Progress Screen

This screen has 3 tabs: **Overview**, **Pain**, and **Range of Motion**.

| Status | Method | Path | Notes |
|--------|--------|------|-------|
| ❌ | GET | `/progress/{userId}/rom` | ROM data points over time for the line chart. |
| ❌ | GET | `/progress/{userId}/adherence` | Adherence % + weekly day-by-day completion for the donut and calendar. |
| ❌ | GET | `/progress/{userId}/pain` | Daily pain levels for the bar chart. |
| ❌ | GET | `/progress/{userId}/summary` | Weekly summary stats (avg session time, active days, ROM gain). |

**Alternative:** A single endpoint `GET /progress/{userId}?week=3` returning all of the above would also work and is preferred to reduce round trips.

**Additional required fields:**
- `weekLabel` (string) — displayed in the header badge, e.g. `"Week 3 of 6"`. M1 cannot derive this without knowing the total plan duration; please include it in the response root.
- `skippedExercises` (int) — number of skipped exercises this week, shown in the adherence card (e.g. "2 skipped this week").

**Required response shape:**

```json
{
  "weekLabel": "Week 3 of 6",
  "rom": {
    "currentDegrees": 120,
    "targetDegrees": 135,
    "weeklyGainDegrees": 15,
    "history": [
      { "date": "2024-04-04", "degrees": 80 },
      { "date": "2024-04-11", "degrees": 95 },
      { "date": "2024-04-18", "degrees": 105 },
      { "date": "2024-04-22", "degrees": 112 },
      { "date": "2024-04-25", "degrees": 120 }
    ]
  },
  "adherence": {
    "weeklyPercent": 85,
    "completedExercises": 12,
    "totalExercises": 14,
    "skippedExercises": 2,
    "streakWeeks": 3,
    "weekDays": [
      { "day": "Mon", "done": true },
      { "day": "Tue", "done": true },
      { "day": "Wed", "done": false },
      { "day": "Thu", "done": true },
      { "day": "Fri", "done": true },
      { "day": "Sat", "done": false }
    ]
  },
  "pain": {
    "averageThisWeek": 4,
    "changeFromLastWeek": -2,
    "daily": [
      { "date": "2024-04-19", "level": 6 },
      { "date": "2024-04-20", "level": 6 },
      { "date": "2024-04-21", "level": 5 },
      { "date": "2024-04-22", "level": 4 },
      { "date": "2024-04-23", "level": 3 },
      { "date": "2024-04-24", "level": 4 },
      { "date": "2024-04-25", "level": 4 }
    ]
  },
  "weeklySummary": {
    "avgSessionMinutes": 35,
    "activeDays": 5,
    "romGainDegrees": 8
  }
}
```

---

## 5. Profile Screen

| Status | Method | Path | Notes |
|--------|--------|------|-------|
| ✅ | GET | `/auth/me` | Returns basic user info. M1 reads `name` and `email` from here. |
| ⚠️ | GET | `/auth/me` | Missing fields — see below. |

**Fields currently missing from `GET /auth/me` response:**

```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@email.com",
  "conditionLabel": "Right Knee Surgery",
  "conditionDate": "2024-03-28",
  "currentRomDegrees": 120,
  "adherencePercent": 85,
  "streakWeeks": 3
}
```

The profile screen displays: condition label + date, current ROM, adherence %, and streak. These need to either come from `GET /auth/me` or from the progress endpoint above.

---

## 6. Sessions (Exercise Flow)

| Status | Method | Path | Notes |
|--------|--------|------|-------|
| ✅ | POST | `/sessions` | Create a new session. Returns `session_id`. |
| ✅ | PATCH | `/sessions/{id}/end` | End session. |
| ✅ | POST | `/measurements/raw` | Upload sensor data (FormatData). |
| ✅ | GET | `/measurements/{sessionId}` | Get measurements for a session. |
| ✅ | GET | `/recommendations/session/{sessionId}` | Post-session recommendations. |
| ✅ | GET | `/recommendations/engine/{userId}` | AI recommendations for user. |

---

## 7. Push Notifications

| Status | Method | Path | Notes |
|--------|--------|------|-------|
| ✅ | POST | `/push/register` | Register FCM device token. |

---

## 8. Global Format Requirements

These apply to **all** endpoints and must be consistent:

| # | Requirement | Reason |
|---|-------------|--------|
| 1 | **Use camelCase for all JSON field names** (e.g. `startDate`, not `start_date`). M1 is parsing with Gson and mapping manually — inconsistent casing causes silent null values. | Consistency |
| 2 | **Dates in ISO 8601 format: `YYYY-MM-DD`** (e.g. `"2024-04-04"`). Do not use `DD/MM/YYYY` or timestamp integers for date-only fields. | PayloadConverter already uses ISO 8601 for timestamps. |
| 3 | **Numeric IDs as integers**, not strings (e.g. `"id": 1`, not `"id": "1"`). Gson deserializes all numbers as `Double` from `Map<String,Any?>` — M1 calls `.toInt()` which fails on strings. | Type safety |
| 4 | **Error responses must use `{ "error": "message string" }`**. V2ApiClient already parses `response.body.error`. Any other shape (e.g. `{ "message": "..." }`) will surface as a generic HTTP error code. | Already implemented in V2ApiClient |
| 5 | **HTTP only (no TLS redirect)**. The base URL is `http://113.44.220.94:3000`. `android:usesCleartextTraffic="true"` is already set in the manifest — do not enable HTTPS redirect without coordinating with M1. | Manifest config |

---

## Priority Order for V2

1. 🔴 `GET /schedule/{scheduleId}/exercises` — blocks PlansDetails screen entirely
2. 🔴 `GET /schedule/{userId}` response shape confirmation — blocks Plans list data
3. 🟠 `GET /progress/{userId}` (or equivalent) — blocks Progress screen
4. 🟠 Missing fields in `GET /auth/me` — blocks Profile screen stats
5. 🟡 `PATCH /schedule/{scheduleId}/exercises/{exerciseId}/complete` — needed for session flow
6. 🟡 `todayExercises` field in schedule response — nice-to-have for Plans list
