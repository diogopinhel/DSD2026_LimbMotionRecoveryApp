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
| `category` | string | Exercise category (`"Lower Body"`, `"Flexibility"`, `"Balance"`, `"Gait"`) |
| `description` | string | One or two sentences summarising the exercise — shown below the exercise name in the UI |
| `instructions` | JSON array of strings | Step-by-step instructions. Each string is one numbered step. Rendered as a list in both M1 and M2. |
| `gif_url` | string (nullable) | URL of an animated GIF demonstrating the exercise. Return `null` if unavailable — never omit the field. |
| `thumbnail_url` | string (nullable) | URL of a static image for card previews. **See note below.** |

> **Why `thumbnail_url` in addition to `gif_url`?**
> The M1 exercise list shows all exercises for a session at once. Loading 10 animated GIFs simultaneously causes significant battery drain, data usage, and UI jank. `thumbnail_url` is used as a static preview in list/card contexts; the GIF is only loaded when the patient opens the exercise detail screen. `thumbnail_url` is also used in the M2 doctor dashboard exercise picker and works as a fallback if the GIF URL is unreachable.

### Seed Data (10 exercises)

The complete seed payload — including full step-by-step `instructions`, real `gif_url` values sourced from JEFIT and Physiopedia, and `thumbnail_url` for each exercise — is in:

```
docs/v2_exercise_seed_data.json
```

Summary of GIF sources used:

| ID | Name | gif_url source | thumbnail_url source |
|---|---|---|---|
| 1 | Squat | JEFIT #493 | JEFIT |
| 2 | Walking Test | JEFIT #1373 | JEFIT |
| 3 | Stair Climbing | JEFIT #1225 | JEFIT |
| 4 | Straight Leg Raise | JEFIT #982 | JEFIT |
| 5 | Knee Extension | JEFIT #130 | JEFIT |
| 6 | Ankle Pumps | Physiopedia (archive) | `null` |
| 7 | Hip Abduction | JEFIT #1361 | JEFIT |
| 8 | Calf Raises | JEFIT #1227 | JEFIT |
| 9 | Hamstring Stretch | JEFIT #932 | JEFIT |
| 10 | Single-Leg Balance | JEFIT #662 | JEFIT |

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
    "description": "Strengthens quadriceps, glutes and core. Essential for regaining functional leg strength after lower limb surgery.",
    "instructions": [
      "Stand with feet shoulder-width apart, toes pointing slightly outward.",
      "Extend your arms forward for balance and keep your chest up.",
      "Slowly bend your knees and sit back with your hips, as if sitting into a chair.",
      "Lower until your knees are parallel with your glutes, or as far as comfortable.",
      "Return to the starting position, pressing through your heels."
    ],
    "gif_url": "https://cdn.jefit.com/assets/img/exercises/gifs/493.gif",
    "thumbnail_url": "https://www.jefit.com/images/exercises/960_590/1972.jpg"
  },
  {
    "id": 2,
    "name": "Walking Test",
    "category": "Gait",
    "description": "Assesses basic gait quality and mobility after lower limb injury or surgery.",
    "instructions": [
      "Stand upright with eyes forward and arms relaxed at your sides.",
      "Walk forward at a natural, comfortable pace for 5 metres.",
      "Turn around and return to the starting position."
    ],
    "gif_url": null,
    "thumbnail_url": null
  }
]
```

### Field Specification

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | integer | Yes | |
| `name` | string | Yes | |
| `category` | string | Yes | |
| `description` | string | Yes | Short summary shown below the exercise name |
| `instructions` | array of strings | Yes | Step-by-step list. Return `[]` if empty, never `null`. |
| `gif_url` | string or null | Yes | Return `null` if unavailable — never omit the field |
| `thumbnail_url` | string or null | Yes | Return `null` if unavailable — never omit the field |

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
      "description": "Strengthens quadriceps, glutes and core. Essential for regaining functional leg strength after lower limb surgery.",
      "instructions": [
        "Stand with feet shoulder-width apart, toes pointing slightly outward.",
        "Slowly bend your knees and sit back with your hips.",
        "Lower until knees are parallel with glutes, or as far as comfortable.",
        "Return to starting position pressing through your heels."
      ],
      "gif_url": "https://cdn.jefit.com/assets/img/exercises/gifs/493.gif",
      "thumbnail_url": "https://www.jefit.com/images/exercises/960_590/1972.jpg",
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
| `notes` | string or null | Yes | Doctor's note to the patient |
| `description` | string or null | Yes | Short summary shown below the exercise name |
| `instructions` | array of strings | Yes | Step-by-step list. Return `[]` if empty, never `null`. |
| `gif_url` | string or null | Yes | Animated GIF shown on the exercise detail screen |
| `thumbnail_url` | string or null | Yes | Static image used in card/list previews |
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
