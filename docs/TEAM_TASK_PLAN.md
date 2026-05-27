# M1 — Team Task Plan

**Project:** Limb Motion Recovery App (M1 — Patient Mobile Application)
**Repository:** https://github.com/diogopinhel/DSD2026_LimbMotionRecoveryApp
**Plan date:** 2026-05-16
**Team lead:** Diogo Pinhel (`dpinhel`) — reviews every PR from Sara and Enhe, and co-owns the heavy session track together with Yiding.
**Team members:**
- Yiding Wang
- Sara Costa
- Enhe Zhang

---

## 1. Purpose of This Document

This document divides the remaining M1 work into three independent tracks. Diogo Pinhel **co-owns the heaviest track (session controller + plan wiring + real-time feedback) together with Yiding Wang** because it has the highest integration risk and needs an extra pair of hands. Diogo is also the **sole reviewer** of Sara's and Enhe's pull requests, so he stays close to those tracks without taking individual tasks in them.

Each track lists:
- **Scope** — what to build.
- **Files to create / touch** — exact paths in this repo.
- **Designs to follow** — the HTML mockup in `/design/`.
- **Dependencies** — which V2 endpoints are needed and who to contact if missing.
- **Definition of done** — what "finished" looks like.

---

## 2. Current State of the App (Snapshot)

### Already implemented (do not redo)

| Module | Feature | Files |
|---|---|---|
| MOD-M1-01 | Login / Register / Logout (`POST /auth/login`, `POST /auth/register`, token in `SharedPreferences`) | `screens/auth/*` |
| MOD-M1-01 | Bottom navigation (Home, Plans, Progress, Profile) | `MainActivity.kt` |
| — | Home screen (greeting, week progress, sensor banner, "Today at a glance") | `screens/home/*` |
| MOD-M1-05 | Plans list grouped by Active / Upcoming / Completed from `GET /schedule/{userId}` | `screens/plans/PlansFragment.kt`, `PlansViewModel.kt`, `PlanAdapter.kt` |
| MOD-M1-05 | Plan Details UI shell (exercise list **blocked** by missing V2 endpoint) | `screens/plans/PlanDetailsActivity.kt`, `PlanDetailsViewModel.kt`, `ExerciseAdapter.kt` |
| — | Progress screen UI (Overview / Pain / ROM tabs, custom `LineChartView` + `DonutChartView`) — placeholders only | `screens/progress/*` |
| — | Profile screen UI (initials, user info, settings rows — all "Coming soon") | `screens/profile/*` |
| MOD-M1-02 | BLE sensor connection flow (`SensorActivity` + `SensorRepository`, 4 states: Searching / Found / Connecting / Connected, plus Error) | `sensor/SensorActivity.kt`, `sensor/SensorRepository.kt` |

### Still missing (this is what we divide)

| # | Module | Feature | Blocker |
|---|---|---|---|
| 1 | MOD-M1-03 | Exercise session controller — start / stop S2 session, sensor ↔ joint mapping, stream `FormatData` → `POST /measurements/raw`, end session | Needs `GET /schedule/{scheduleId}/exercises` from V2 (currently missing). |
| 2 | MOD-M1-04 | Real-time feedback UI during a session — live joint angle, ROM counter, rep counter, deviation alerts | Depends on #1 being far enough along; can be developed against the simulator (`s2.session.setMode(useSimulator = true)`). |
| 3 | MOD-M1-05 | Plan Details — wire real exercises into the existing UI | Same V2 endpoint as #1. |
| 4 | MOD-M1-05 | Progress screen — replace placeholders with real data | Needs `GET /progress/{userId}` (missing from V2). |
| 5 | MOD-M1-01 | Profile sub-screens (My Recovery, Appointments, Medications, Settings, Help, Privacy) — currently all show a `Toast`. | Some need new V2 endpoints. |
| 6 | MOD-M1-06 | Push notifications — FCM token register via `POST /push/register`, foreground notification handling | Needs an FCM project to be created in Firebase Console. |

---

## 3. Track Assignments

### 3.1 — Yiding Wang + Diogo Pinhel — Exercise Controller, Plans Wiring & Real-Time Feedback

**Track focus:** MOD-M1-03, MOD-M1-04 and the data-wiring part of MOD-M1-05.

This is the most integration-heavy track on the project — it bridges the S2 module (sensor data), the V2 backend (session storage) and the live feedback the patient sees on screen. Because the workload is high and the risk of integration bugs is high, **Yiding Wang and Diogo Pinhel pair on this track**. Yiding has the strongest technical background on the team and leads the work; Diogo helps on implementation alongside reviewing the other two tracks.

**Split of responsibilities inside the pair:**
- **Yiding** — leads. Owns the `SessionController` design + S2/V2 plumbing, drives the rep-counter heuristic, and is the **single point of contact with Sergio Moniz** for V2 endpoints.
- **Diogo** — helps. Picks up sub-tasks Yiding hands off (typically the `LiveFeedbackFragment` UI work and the `PlanDetailsActivity` wiring once `getPlanExercises` lands), integrates the pieces, and keeps the rest of the team unblocked.

The pair agrees on who picks up what at the start of each day and writes it in their daily reports (see §4.4).

#### Scope
1. **Wire the exercise list into `PlanDetailsActivity`** (data only — UI is already built)
   - Today the activity shows `State.EndpointMissing` because the V2 endpoint does not exist.
   - When the endpoint is delivered, replace the placeholder in `PlanDetailsViewModel.load()` (see lines 40–52) with the real call.
   - Parse the response into the existing `PlanDetails` / `Exercise` model.

2. **Exercise Session Controller** (the backend of the session — Sara builds the UI shell around it)
   - Provide a `SessionController` class that exposes a clean API to Sara's `SessionPlayerActivity`:
     ```kotlin
     class SessionController(context: Context) {
         val samples: Flow<FormatData>        // hot flow, ticks at ~10 Hz
         val state: StateFlow<SessionState>   // IDLE / RUNNING / PAUSED / ENDED
         suspend fun start(scheduleId: Int, sensorJointMapping: Map<String,String>)
         fun pause(); fun resume(); suspend fun stop()
     }
     ```
   - Internally, call `s2.session.start(...)`, poll `s2.data.read()`, upload each `FormatData` to V2 with `PayloadConverter.formatDataToPayload(...)` + `V2ApiClient.uploadMeasurement(...)`.
   - On `stop()`, call `PATCH /sessions/{id}/end` and `PATCH /schedule/{scheduleId}` to mark the session done.
   - Must work against both the real S1 module and the simulator (`s2.session.setMode(useSimulator = true)`).

3. **Real-time feedback UI** (MOD-M1-04) — embedded inside Sara's session screen
   - **Live joint angle gauge** — custom view (same pattern as `progress/DonutChartView.kt`) that draws the current angle as an arc, updated at ~10 Hz from the `samples` Flow.
   - **ROM live tracker** — current / min / max angle, with a "You reached 95° — keep going to 110°" coaching line.
   - **Rep counter** — heuristic based on angle peaks and troughs in `FormatData`. Visual confirmation animation when a rep is counted.
   - **Deviation alerts** — non-blocking warning if the patient exceeds the safe ROM ("Slow down — you went past 130°"); also "Sensor lost — check the strap" after 2 s without samples.
   - Expose this as a single fragment that Sara hosts inside her `SessionPlayerActivity`:
     ```kotlin
     class LiveFeedbackFragment : Fragment() {
         fun bind(samples: Flow<FormatData>, exercise: Exercise)
         fun currentSummary(): SessionSummary
     }
     ```

#### V2 dependencies — Yiding is the single point of contact with Sergio Moniz
Yiding owns the conversation with **Sergio Moniz** (V2 team lead) about every endpoint M1 needs for the session/plans area. Diogo CC'd on the thread but does not message Sergio directly to avoid duplicate requests. Refer Sergio to `docs/V2_API_REQUIREMENTS.md`. Open the conversation on day 1.

| Priority | Endpoint | Why |
|---|---|---|
| 🔴 High | `GET /schedule/{scheduleId}/exercises` | Without this, Plan Details cannot show exercises. |
| 🔴 High | Confirm exact shape of `POST /sessions` response (must return `session_id`). | Needed to start streaming measurements. |
| 🟡 Medium | Confirm `POST /measurements/raw` accepts the `FormatData` payload built by `PayloadConverter.kt`. | Needed to upload sensor data. |

While waiting on V2, develop against the **simulator** so the controller and feedback fragment can be finished before any endpoint lands.

#### Files to touch / create
- Touch: `screens/plans/PlanDetailsActivity.kt`, `screens/plans/PlanDetailsViewModel.kt`.
- Extend: `com/dsd/m1/api/V2ApiClient.kt` (add `getPlanExercises(planId, token)` — this file is in the "extendable" list).
- Create: `session/SessionController.kt` (controller backbone — pure logic, no Activity dependency).
- Create: `screens/session/feedback/LiveFeedbackFragment.kt`
- Create: `screens/session/feedback/JointAngleGaugeView.kt` (custom view)
- Create: `screens/session/feedback/RepCounter.kt` (pure Kotlin, unit-testable)
- Layouts: `res/layout/fragment_live_feedback.xml`

#### Designs
- `design/PlansDetails.html` — already implemented, only needs data wiring.
- ⚠️ **No design exists yet** for the live feedback gauge, ROM tracker or deviation alerts. Send a WeChat message to Diogo specifying:
  > "I need designs for: (1) Live feedback area (gauge + ROM + rep counter) inside the session screen, (2) Deviation alert visual style."

#### Definition of done
- [ ] `PlanDetailsActivity` shows the real exercise list when the V2 endpoint is delivered.
- [ ] `SessionController.start(...)` runs a session end-to-end: S2 starts, samples flow, payloads upload, session ends, schedule item is marked done.
- [ ] Live gauge updates smoothly (≥ 10 Hz) against the simulator.
- [ ] Rep counter detects at least 3 consecutive reps in a sinusoidal signal.
- [ ] Deviation alert fires when angle crosses a configurable threshold.
- [ ] Works against both the real S1 module and the simulator.

---

### 3.2 — Sara Costa — Session Player UI Shell & Surrounding Screens

**Track focus:** the UI scaffolding around Yiding's controller — everything the patient sees in the session flow **except the live feedback area** (that one belongs to Yiding).

Sara's track is essentially "make the session flow feel like an app": navigation in and out of the session, the player layout, the sensor↔joint mapping step before starting, and the summary screen at the end.

#### Scope
1. **Session Player Activity (UI shell)**
   - New activity launched when the user taps **"Start Session"** in `PlanDetailsActivity` (currently shows a `Toast` — see `PlanDetailsActivity.kt:88`).
   - Layout: top bar with exercise name + progress (`Exercise 2 of 5`), a content area where Yiding's `LiveFeedbackFragment` is embedded, and bottom controls (**Pause / Resume / Stop**).
   - The activity **does not** poll S2 or talk to V2 — it only forwards user actions to Yiding's `SessionController` and observes its `state` StateFlow.
   - "Next exercise" and "Skip" buttons that advance through the exercise list of the plan.

2. **Sensor↔Joint mapping step**
   - Small screen or bottom-sheet shown before the session starts.
   - Two WitMotion sensors are required to compute one joint angle. The user has to confirm which two sensors map to which joint (e.g. `"AA:BB" → "knee"`, `"CC:DD" → "knee"`).
   - Use `SensorRepository` to read the currently connected sensors.
   - Hands the resulting `Map<String, String>` to `SessionController.start(...)`.

3. **End-of-session summary screen**
   - Reached automatically when `SessionController.state` transitions to `ENDED`.
   - Reads the totals from `LiveFeedbackFragment.currentSummary()` (Yiding exposes this): total reps, average ROM, max ROM, total time.
   - "Save & Continue" button that finishes the activity and returns to the Plan Details screen.

#### Coordination
Sara talks to **Yiding and Diogo** — not to V2 / Sergio. The three must agree on the `SessionController` + `LiveFeedbackFragment` contracts on **day 1** (15-min WeChat call). Document the agreement in your end-of-day report.

#### Files to create
- `screens/session/SessionPlayerActivity.kt`, `SessionPlayerViewModel.kt`
- `screens/session/SensorJointMappingFragment.kt` (or bottom sheet)
- `screens/session/SessionSummaryActivity.kt`
- Layouts: `res/layout/activity_session_player.xml`, `res/layout/fragment_sensor_joint_mapping.xml`, `res/layout/activity_session_summary.xml`

#### Designs
- ⚠️ **No design exists yet** for any of these screens. Send a WeChat message to Diogo specifying:
  > "I need designs for: (1) Session Player screen layout (top bar + feedback slot + bottom controls), (2) Sensor↔Joint mapping step, (3) End-of-session summary screen."

#### Definition of done
- [ ] User can tap "Start Session" on a plan and reach the player activity.
- [ ] The mapping step appears before the session starts and produces a valid `Map<String, String>`.
- [ ] Pause / Resume / Stop buttons correctly call `SessionController` and reflect its state.
- [ ] Next / Skip advance the exercise correctly.
- [ ] On stop, the summary screen appears with totals from Yiding's fragment.
- [ ] No memory leaks or freezes when the session ends abruptly (e.g. user backs out).

---

### 3.3 — Enhe Zhang — Notifications, Profile & Progress Wiring

**Track focus:** MOD-M1-05 (Progress data) + MOD-M1-06 (Push Notifications) + Profile sub-screens.

Smaller individual features but spread across the app — good for someone who wants variety.

#### Scope
1. **Push notifications (MOD-M1-06)**
   - Add Firebase Cloud Messaging dependency in `app/build.gradle.kts`.
   - Create a `FirebaseMessagingService` subclass that:
     - Retrieves the FCM token at login time.
     - Calls `V2ApiClient.registerPushToken(userId, deviceToken, "android", token)` (already implemented in `V2ApiClient.kt:123`).
     - Displays incoming notifications as system notifications.
   - **You will need to create an FCM project in Firebase Console** — ask Diogo to add you to the project or to forward the `google-services.json`.

2. **Wire the Progress screen with real data**
   - The Progress screen UI is fully built (`screens/progress/ProgressFragment.kt`) but uses placeholders.
   - Endpoint needed: `GET /progress/{userId}` — does **not exist yet** in V2. Send a message to Sergio Moniz to request it. Required fields:
     ```json
     {
       "weeklyRom": [85, 90, 92, 95, 98, 100, 105],
       "weeklyPain":[6, 5, 5, 4, 4, 3, 3],
       "adherencePct": 78,
       "streakDays": 12
     }
     ```
   - Until then, work with mock data inside `ProgressViewModel`.

3. **Profile sub-screens — replace "Coming soon" `Toast`s with real screens**
   - Current placeholders are in `screens/profile/ProfileFragment.kt:39-56`.
   - Priority order:
     1. **Settings** — language, units (degrees vs radians), notification preferences, dark mode toggle.
     2. **Help** — static FAQ + "Contact support" mail intent.
     3. **Privacy** — static text + link to the privacy policy.
     4. **My Recovery** — recovery history (depends on a new V2 endpoint, request from Sergio).
     5. **Appointments** — depends on `/appointments/{userId}` (not in V2 yet).
     6. **Medications** — depends on `/medications/{userId}` (not in V2 yet).

   Build the first three (no backend dependency), then unblock the rest as endpoints arrive.

#### V2 dependencies — message Sergio Moniz
| Priority | Endpoint | For |
|---|---|---|
| 🟡 Medium | `GET /progress/{userId}` | Progress screen |
| 🟢 Low | `GET /appointments/{userId}` | Profile → Appointments |
| 🟢 Low | `GET /medications/{userId}` | Profile → Medications |

#### Files to create / touch
- Touch: `app/build.gradle.kts` (add `com.google.firebase:firebase-messaging`).
- Touch: `screens/profile/ProfileFragment.kt`, `screens/progress/ProgressFragment.kt`, `screens/progress/ProgressViewModel.kt`.
- Touch: `screens/auth/LoginViewModel.kt` (register FCM token after successful login).
- Create: `notifications/FcmService.kt`.
- Create: `screens/profile/settings/SettingsActivity.kt` (+ layout).
- Create: `screens/profile/help/HelpActivity.kt` (+ layout).
- Create: `screens/profile/privacy/PrivacyActivity.kt` (+ layout).

#### Designs
- `design/PerfilScreen.html` — already implemented for the main Profile page.
- ⚠️ **No design exists** for Settings / Help / Privacy / Appointments / Medications. Send a WeChat message to Diogo specifying which one you need next.

#### Definition of done
- [ ] FCM token is registered with V2 right after login (verify in Logcat).
- [ ] A test notification sent from Firebase Console reaches the device.
- [ ] At least Settings / Help / Privacy screens replace their `Toast` placeholders.
- [ ] Progress screen renders mock data with no crashes, and switches to live data when the endpoint exists.

---

## 4. Team Workflow Rules

These rules apply to **every** teammate. They are non-negotiable so the repo stays clean and reviewable.

### 4.1 — Branches
- Each member works on their own branch named after themselves, all lowercase, no spaces:
  - Enhe Zhang → `ezhang`
  - Yiding Wang → `ywang`
  - Sara Costa → `scosta`
- Never commit to `master` or to anyone else's branch.
- Pull from `master` daily before starting work:
  ```bash
  git checkout master
  git pull
  git checkout <your-branch>
  git rebase master
  ```

### 4.2 — Designs
- The design source of truth is the folder `/design/` at the repo root. It contains the HTML mockups for every implemented screen.
- **Match the design exactly** — colours, spacing, font sizes, icons. Use the existing `colors.xml` / `themes.xml` rather than hard-coding hex values.
- **If a design is missing**, do not invent one — send a WeChat message to Diogo specifying:
  > "I need the design for: [screen name] — for the [feature] track. Blocking me from continuing."

### 4.3 — Commits
- Commit messages must be **in English**.
- Commit messages must be **explanatory** — the title should describe *what* changed, the body (optional) should explain *why*.
- Use Conventional Commits style (the repo already uses this — see `git log`):
  ```
  feat(session): add live joint angle gauge with rep counter
  fix(plans): correct status badge color when plan is upcoming
  refactor(profile): extract Settings row into reusable view
  docs(team): update task plan with v2 dependencies
  ```
- One concern per commit. Avoid "fix stuff" or "update".

### 4.4 — End-of-Day Report
Every working day, before logging off, each teammate creates a Markdown file in `/docs/daily/<your-name>/` named `YYYY-MM-DD.md`. The template is:

```markdown
# Daily Report — <Your Name>

**Date:** YYYY-MM-DD
**Branch:** <your-branch>
**Track:** <Session controller / Real-time feedback / Notifications & Profile>

## Done today
- ...

## Blockers
- (e.g. waiting on V2 endpoint X; waiting on design Y)

## Planned for tomorrow
- ...

## Questions for Diogo
- ...
```

Commit this file at the end of the day in the same push. This is how Diogo reviews progress and unblocks people without needing a daily standup.

### 4.5 — Weekly Evaluation (Professor ZHANG)

Every week, **each teammate** must complete the weekly evaluation required by Professor ZHANG using **his official base template**. This is a mandatory academic requirement.

- **Frequency:** once per week, at the end of each working week (before Sunday midnight).
- **Template:** use the base template provided by Professor ZHANG — do not create your own format.
- **Where to submit:** as instructed by Professor ZHANG (platform / WeChat / email — follow his instructions directly).
- **Content expected (to fill in the template):**
  - Tasks completed this week (reference branch, PR, or commit where applicable).
  - Blockers encountered and how they were resolved.
  - Tasks planned for the following week.
  - Any questions or impediments requiring professor attention.
- **Diogo is not the reviewer for this** — submit directly to Professor ZHANG.
- Missing a weekly evaluation without prior notice is not acceptable.

---

### 4.6 — Pull Requests
- When a feature is done (matching the Definition of Done), open a Pull Request from your branch → `master`.
- PR title in English, same Conventional Commits style as commit titles.
- PR description must include:
  - **Summary** — 1–3 bullets of what changed.
  - **How to test** — exact steps on a device or emulator.
  - **Screenshots** — at least one for any UI change.
- Diogo is the reviewer for Sara's and Enhe's PRs.
- For the session track (Yiding + Diogo paired), the **author who did not write the code reviews** — i.e. Yiding reviews Diogo's PRs and vice-versa. Never merge your own PR.

### 4.7 — Communication
- Day-to-day chat: WeChat group.
- For V2 endpoint questions: contact **Sergio Moniz** (V2 team lead) directly on WeChat — CC Diogo so he knows it's in progress.
- For design requests: contact **Diogo** on WeChat with the exact screen / feature name.

---

## 5. Cross-Track Dependencies (Read Before Starting)

```
Yiding + Diogo (SessionController + V2 upload)
    │
    └──> exposes Flow<FormatData> + StateFlow<SessionState>
                  │
                  └──> Yiding + Diogo (LiveFeedbackFragment) consume the Flow
                  │
                  └──> Sara (SessionPlayerActivity) hosts the fragment and
                       drives Pause / Resume / Stop on the controller

Enhe (FCM)
    │
    └──> registers token in LoginViewModel after login   (no impact on the session track)

Enhe (Profile sub-screens + Progress wiring)
    │
    └──> independent of the session track
```

**Action for day 1:** a 15-min WeChat call with Yiding + Diogo + Sara to lock the `SessionController` and `LiveFeedbackFragment` contracts. Enhe does not need to join.

---

## 6. Files You Must NOT Modify

These are owned by other teams and copied as-is. Touching them will break cross-team compatibility.

| Path | Owner |
|---|---|
| `app/src/main/java/com/dsd/s1/**` | S1 team (BLE driver, Java) |
| `app/src/main/java/com/dsd/s2/**` | S2 team (processing, Kotlin) — but `S2Module.kt` is the public facade you call from M1 |
| `app/src/main/java/com/dsd/s2/core/S1Interfaces.kt` | Interface contract — frozen |
| `app/src/main/java/com/dsd/s2/S2Module.kt` interface methods | Interface contract — frozen |

You **can** extend `app/src/main/java/com/dsd/m1/api/V2ApiClient.kt` — add new methods, do not remove or rename existing ones.

---

## 7. Reference Documents

- `CLAUDE.md` — full architecture, build instructions, V2 endpoint summary.
- `docs/M1_STATUS.md` — what is already implemented, with test cases for the BLE flow.
- `docs/V2_API_REQUIREMENTS.md` — the formal endpoint request list to send to V2.
- `design/*.html` — design source of truth for each screen.

---

*Last updated by Diogo Pinhel on 2026-05-16. If you have questions, message Diogo on WeChat.*
