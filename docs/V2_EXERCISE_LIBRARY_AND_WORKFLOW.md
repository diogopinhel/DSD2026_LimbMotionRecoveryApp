# V2 Implementation Request — Exercise Library & Prescription Workflow

**From:** M1 Team (Patient Mobile App)  
**To:** V2 Team (Backend)  
**Date:** 2026-06-07  
**Priority:** 🔴 Critical — blocks Plan Details screen and the full exercise prescription flow

---

## Overview

We need V2 to implement a global exercise library and wire it into the existing schedule system so that:

- The **M2 doctor dashboard** can pick exercises from a catalog and assign them to a patient's plan with sets/reps
- The **M1 patient app** can display those exercises (with an animated GIF demonstration) and let the patient mark them as done

The schedule and schedule-exercises endpoints already exist. What is missing is the exercise catalog and a few field additions to existing responses.

---

## 1. New Database Table — `exercises`

Create a table with the following schema:

| Column | Type | Description |
|---|---|---|
| `id` | integer (PK, auto-increment) | Unique exercise ID |
| `name` | string | Display name |
| `category` | string | Exercise category |
| `description` | string | Short instructions for the patient |
| `gif_url` | string (nullable) | URL of an animated GIF demonstrating the exercise |

### Seed Data (10 exercises)

Please seed the table with the following entries on first deploy. For `gif_url`, use any appropriate publicly accessible GIF demonstrating the exercise (Physiopedia, Giphy, or similar):

| ID | Name | Category | Description |
|---|---|---|---|
| 1 | Squat | Lower Body | 3 reps, ~5 s each. Feet shoulder-width apart, knees aligned with toes. Do not let knees cave inward. |
| 2 | Walking Test | Gait | Walk forward 5 m at a natural pace. Eyes forward, arms relaxed. |
| 3 | Stair Climbing | Lower Body | Climb 10 steps. Body upright, one step at a time, hold the rail if needed. |
| 4 | Straight Leg Raise | Lower Body | Lie flat on back. Lift one leg to 45°, hold 2 s, lower slowly. |
| 5 | Knee Extension | Lower Body | Seated on a chair. Extend knee fully, hold 3 s, lower slowly. |
| 6 | Ankle Pumps | Lower Body | Seated or lying. Flex and point the ankle repeatedly. Good for circulation post-surgery. |
| 7 | Hip Abduction | Lower Body | Side-lying. Lift top leg to 30–45°, hold 2 s, lower slowly. |
| 8 | Calf Raises | Lower Body | Stand with feet flat. Rise onto toes, hold 2 s, lower slowly. |
| 9 | Hamstring Stretch | Flexibility | Seated, legs extended. Reach forward towards feet, hold 20–30 s. Do not bounce. |
| 10 | Single-Leg Balance | Balance | Stand on one leg for 30 s. Switch sides. Hold a wall if needed. |

---

## 2. New Endpoint — `GET /exercises`

Returns the full exercise catalog. Used by M2 to populate the exercise picker when building a patient plan.

### Request

```
GET /exercises
Authorization: Bearer <token>
```

### Response

```json
[
  {
    "id": 1,
    "name": "Squat",
    "category": "Lower Body",
    "description": "3 reps, ~5 s each. Feet shoulder-width apart, knees aligned with toes.",
    "gif_url": "https://www.youtube.com/watch?v=..."
  },
  {
    "id": 2,
    "name": "Walking Test",
    "category": "Gait",
    "description": "Walk forward 5 m at a natural pace. Eyes forward, arms relaxed.",
    "gif_url": "https://www.youtube.com/watch?v=..."
  }
]
```

### Field Specification

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | integer | Yes | |
| `name` | string | Yes | |
| `category` | string | Yes | |
| `description` | string | Yes | |
| `gif_url` | string or null | Yes | Return `null` if no video available, never omit the field |

### HTTP Errors

| Status | When |
|---|---|
| 401 | Token missing or expired |

---

## 3. Updated Endpoint — `POST /schedule/{scheduleId}/exercises`

This endpoint already exists. We need it to also accept and store `gif_url`, `description`, and `notes`.

### Request Body

```json
{
  "name": "Squat",
  "phase": "Strength",
  "sets": 3,
  "reps": 10,
  "hold_seconds": 2,
  "notes": "Stop immediately if you feel sharp knee pain.",
  "gif_url": "https://www.youtube.com/watch?v=...",
  "description": "3 reps, ~5 s each. Feet shoulder-width apart."
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | Yes | Exercise name, copied from the catalog |
| `phase` | string | Yes | `Warm Up` / `Strength` / `Mobility` / `Cooldown` |
| `sets` | integer | Yes | Number of sets (set by doctor) |
| `reps` | integer | Yes | Reps per set (set by doctor) |
| `hold_seconds` | integer | No | Hold duration in seconds, default `0` |
| `notes` | string | No | Doctor's note to the patient |
| `gif_url` | string | No | Copied from the exercise catalog entry |
| `description` | string | No | Copied from the exercise catalog entry |

---

## 4. Updated Endpoint — `GET /schedule/{scheduleId}/exercises`

This endpoint already exists. We need it to return `gif_url`, `description`, and `notes` per exercise so the M1 patient app can display them.

### Response

```json
{
  "exercises": [
    {
      "id": 101,
      "name": "Squat",
      "phase": "Strength",
      "sets": 3,
      "reps": 10,
      "holdSeconds": 2,
      "notes": "Stop immediately if you feel sharp knee pain.",
      "gif_url": "https://www.youtube.com/watch?v=...",
      "description": "3 reps, ~5 s each. Feet shoulder-width apart.",
      "completed": false,
      "lastPainLevel": null
    }
  ]
}
```

### Full Field Specification Per Exercise

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | integer | Yes | |
| `name` | string | Yes | |
| `phase` | string | Yes | |
| `sets` | integer | Yes | |
| `reps` | integer | Yes | |
| `holdSeconds` | integer | Yes | Use `0` if not applicable, never `null` |
| `notes` | string or null | Yes | Doctor's note |
| `gif_url` | string or null | Yes | Used to show video button in M1 |
| `description` | string or null | Yes | Short instructions shown below exercise name |
| `completed` | boolean | Yes | Whether patient completed this exercise |
| `lastPainLevel` | integer or null | Yes | Pain reported (1–10), `null` if not reported |

---

## 5. Existing Endpoint — `PATCH /schedule/{scheduleId}/exercises/{exerciseId}/complete`

Please confirm this endpoint exists and accepts the following:

### Request Body

```json
{
  "pain_level": 3
}
```

### Response

```json
{
  "exerciseId": 101,
  "completed": true,
  "painLevel": 3,
  "completedAt": "2026-06-07T14:30:00Z"
}
```

---

## 6. Full Workflow — End to End

### Doctor side (M2)

```
1. Doctor opens a patient's profile
2. Doctor creates a new schedule/plan:
      POST /schedule  { userId, date, ... }

3. Doctor opens the plan and clicks "Add Exercise"
4. M2 fetches exercise catalog:
      GET /exercises

5. Doctor selects an exercise from the list (e.g. "Squat")
6. Doctor fills in: sets, reps, hold, notes (optional)
7. Doctor saves:
      POST /schedule/{scheduleId}/exercises
      {
        "name": "Squat",
        "phase": "Strength",
        "sets": 3,
        "reps": 10,
        "hold_seconds": 2,
        "notes": "Stop if sharp pain.",
        "gif_url": "<from catalog>",
        "description": "<from catalog>"
      }

8. Repeat steps 3–7 for each exercise in the plan
```

### Patient side (M1)

```
1. Patient opens Plans tab:
      GET /schedule/{userId}  →  list of plans

2. Patient taps a plan:
      GET /schedule/{scheduleId}/exercises  →  list of exercises

3. For each exercise the app shows:
      - Name and phase
      - Sets × Reps (e.g. "3 sets × 10 reps")
      - Hold duration (if > 0)
      - Description (short instruction text)
      - Doctor's note (if present)
      - Animated GIF demonstration shown inline (loaded from gif_url, if not null)
      - [Mark as Done] button

4. Patient completes an exercise and taps "Mark as Done":
      PATCH /schedule/{scheduleId}/exercises/{exerciseId}/complete
      { "pain_level": 3 }

5. Exercise moves from "To Do" tab to "Completed" tab
```

---

## 7. Endpoint Summary

| Priority | Method | Path | Who Calls | Status |
|---|---|---|---|---|
| 🔴 1 | `GET` | `/exercises` | M2 (exercise picker) | ❌ Missing |
| 🔴 2 | `GET` | `/schedule/{scheduleId}/exercises` | M1 | ⚠️ Exists — needs `gif_url`, `description`, `notes` added |
| 🔴 3 | `POST` | `/schedule/{scheduleId}/exercises` | M2 | ⚠️ Exists — needs `gif_url`, `description`, `notes` accepted |
| 🟠 4 | `PATCH` | `/schedule/{scheduleId}/exercises/{exerciseId}/complete` | M1 | ❓ Unconfirmed |

---

## 8. Format Rules

All responses must follow these rules (consistent with existing V2 endpoints):

| Rule | Required Format |
|---|---|
| Field names | camelCase (`holdSeconds`, not `hold_seconds`) in responses |
| Dates | ISO 8601 `YYYY-MM-DD` |
| Timestamps | ISO 8601 `YYYY-MM-DDTHH:MM:SSZ` |
| Numeric IDs | integer (not string) |
| Booleans | JSON `true` / `false` (not `"true"` string) |
| Empty lists | `[]` (never `null`) |
| Null fields | Return field with `null` value — never omit the field entirely |
| Error body | `{ "error": "message" }` |

---

## 9. Open Questions for V2

| # | Question | Priority |
|---|---|---|
| 1 | Does `GET /exercises` require authentication or is it public? | High |
| 2 | Does `GET /schedule/{scheduleId}/exercises` currently return `gif_url`, `description`, `notes`? If not, when can these be added? | High |
| 3 | Is `PATCH /schedule/{scheduleId}/exercises/{exerciseId}/complete` implemented? | High |
| 4 | Can V2 confirm the exact response shape of `GET /schedule/{userId}` so M1 can fix its field mapping? | Medium |
