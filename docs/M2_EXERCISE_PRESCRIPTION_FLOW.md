# Exercise Prescription Flow — M2 Doctor Dashboard

**Author:** M1 Team (Patient Mobile App)  
**Audience:** M2 Team (Doctor Dashboard) + V2 Team (Backend)  
**Date:** 2026-06-07  
**Status:** Proposal — awaiting M2 + V2 confirmation

---

## 1. Problem

Currently the schedule system stores exercises as a free-text name only. There is no exercise library, no video reference, and no structured prescription format. This means:

- The doctor types the exercise name manually every time — error-prone and inconsistent.
- The patient has no video to follow when doing the exercise.
- Sets, reps, and notes exist in the schema but are not exposed in M2's UI.

---

## 2. Recommended Approach: Static Exercise Library in V2

### Why not a free exercise API (ExerciseDB, wger, etc.)?

| Factor | Free API | Static V2 Library |
|---|---|---|
| Rehab-specific exercises | No — gym focused | Yes — custom seeded |
| Video content | No built-in videos | YouTube links (free) |
| External dependency | Yes | No |
| Clinical control | None | Full |
| Implementation complexity | High (API key, mapping) | Low (seed once) |

**Decision: V2 seeds a static exercise library table. M2 fetches it to populate the exercise picker.**

---

## 3. Exercise Library

V2 should create an `exercises` table and seed it with the following entries. The 3 standard actions from Dr. Yin are included, plus additional common rehab exercises.

| ID | Name | Category | Description | `video_url` |
|---|---|---|---|---|
| 1 | Squat | Lower Body | 3 reps, ~5 s each. Feet shoulder-width, knees aligned with toes. | YouTube link (rehab squat tutorial) |
| 2 | Walking Test | Gait | Walk forward 5 m. Natural pace, eyes forward. | YouTube link |
| 3 | Stair Climbing | Lower Body | Climb 10 steps. Body upright, one step at a time. | YouTube link |
| 4 | Straight Leg Raise | Lower Body | Lie flat, lift leg to 45°, hold 2 s. | YouTube link |
| 5 | Knee Extension | Lower Body | Seated, extend knee fully, hold 3 s. | YouTube link |
| 6 | Ankle Pumps | Lower Body | Flex/point ankle repeatedly. Good for circulation. | YouTube link |
| 7 | Hip Abduction | Lower Body | Side-lying, lift leg 30–45°, hold 2 s. | YouTube link |
| 8 | Calf Raises | Lower Body | Stand, rise onto toes, hold 2 s. | YouTube link |
| 9 | Hamstring Stretch | Flexibility | Seated, reach forward, hold 20–30 s. | YouTube link |
| 10 | Single-Leg Balance | Balance | Stand on one leg, 30 s per side. | YouTube link |

> V2 team: please populate `video_url` with appropriate YouTube URLs when seeding. Use existing public rehab tutorial videos.

---

## 4. New V2 Endpoint Required

```
GET /exercises
Authorization: Bearer <token>   (or public — TBD)
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "Squat",
    "category": "Lower Body",
    "description": "3 reps, ~5 s each. Feet shoulder-width.",
    "video_url": "https://www.youtube.com/watch?v=..."
  },
  ...
]
```

Also, confirm that `POST /schedule/:id/exercises` stores and returns `video_url` and `notes` fields:

**Request body for `POST /schedule/:scheduleId/exercises`:**
```json
{
  "name": "Squat",
  "phase": "Strength",
  "sets": 3,
  "reps": 10,
  "hold_seconds": 2,
  "notes": "Stop if you feel sharp knee pain.",
  "video_url": "https://www.youtube.com/watch?v=..."
}
```

**Response should include all fields above + `id` + `completed` + `completed_at`.**

---

## 5. M2 Prescription Flow (Doctor Dashboard)

### 5.1 Open patient's plan

1. Doctor opens a patient's profile (already bound via doctor-patient binding).
2. Doctor navigates to the patient's active rehab plan (schedule).

### 5.2 Add exercise to plan

```
[Patient Plan Page]
  └── [+ Add Exercise] button
        └── Modal: Exercise Picker
              ├── Search / filter by category
              ├── List from GET /exercises
              │     Each row: name · category · [Preview Video] link
              └── [Select] → prefills form below

[Exercise Form]
  ├── Exercise: Squat             ← prefilled from library
  ├── Phase: [Warm Up / Strength / Cooldown]
  ├── Sets: [3]
  ├── Reps: [10]
  ├── Hold (seconds): [2]
  ├── Notes: "Stop if sharp pain"
  └── [Assign to Patient] → POST /schedule/:scheduleId/exercises
```

### 5.3 View assigned exercises

After assigning, the plan detail page lists all exercises with:
- Name, sets × reps, hold
- Notes
- Status (pending / completed)
- Completion date (if done)

---

## 6. M1 Patient App Behaviour

When the doctor assigns exercises via M2, the patient app does:

```
GET /schedule/:userId          → list of plans
GET /schedule/:planId/exercises → exercises in selected plan

For each exercise row:
  - Name, sets × reps × hold
  - Notes from doctor
  - [Watch Video] button → opens video_url in YouTube app (if present)
  - [Mark as Done] button → PATCH /schedule/:planId/exercises/:exerciseId/complete
                             body: { "pain_level": 0–10 }
```

The "To Do" / "Completed" tabs in the plan detail screen separate pending from finished exercises.

---

## 7. Full Endpoint Summary

| Step | Method | Endpoint | Who calls |
|---|---|---|---|
| Get exercise library | `GET` | `/exercises` | M2 (picker) |
| Get patient's plans | `GET` | `/schedule/:userId` | M1 |
| Get exercises in plan | `GET` | `/schedule/:scheduleId/exercises` | M1 |
| Assign exercise to plan | `POST` | `/schedule/:scheduleId/exercises` | M2 |
| Patient marks done | `PATCH` | `/schedule/:scheduleId/exercises/:exerciseId/complete` | M1 |

---

## 8. Open Questions for V2

| # | Question | Priority |
|---|---|---|
| 1 | Will `GET /exercises` be public or require admin token? | High |
| 2 | Does `GET /schedule/:scheduleId/exercises` currently return `video_url` and `notes`? If not, add them. | High |
| 3 | Does `PATCH .../complete` accept `{ "pain_level": N }`? | Medium |
| 4 | Should `GET /schedule/:userId` include a summary of exercise counts per plan? | Low |

---

## 9. Summary of Changes by Team

| Team | Work Required |
|---|---|
| **V2** | Create `exercises` table + seed data + `GET /exercises` endpoint. Confirm `video_url` + `notes` stored in schedule exercises. |
| **M2** | Add exercise picker modal (from `GET /exercises`). Add prescription form with sets/reps/hold/notes. Show assigned exercises per patient plan. |
| **M1** | Display exercises with video button and Mark Done. Wire `GET /schedule/:planId/exercises` to plan detail screen. *(in progress)* |
