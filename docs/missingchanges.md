# Missing Changes — M1 Team

> Generated: 2026-06-04  
> Branch: `dpinhel` (after merges of `origin/YidingWang` + `origin/EnheZhang`)  
> Sara Costa (ArasTacos) — commit `6401fa5` in `origin/dpinhel`, **not yet merged here**

---

## Sara Costa (`ArasTacos` / `origin/dpinhel`, commit `6401fa5`)

> ⚠️ Os ficheiros dela **não estão no branch `dpinhel`** ainda. Precisam de ser integrados.

### Done ✅

- **`SensorJointMappingFragment.kt`** — `BottomSheetDialogFragment` completo e funcional. Recebe dois MACs por argumento, mostra um Spinner por sensor com 6 opções de articulação (knee, hip, ankle, shoulder, elbow, wrist), invoca callback `onMappingConfirmed(Map<String, String>)` ao confirmar. Bem feito.

- **`SessionPlayerActivity.kt`** — Shell de UI completa: botões Pause / Resume / Stop / Next / Skip, state machine local (IDLE → RUNNING ↔ PAUSED → ENDED), navegação para `SessionSummaryActivity` no Stop. Layouts e views linkados correctamente.

- **`SessionSummaryActivity.kt`** — Ecrã de resumo com `sampleCount`, `errorCount`, `startTime`, `endTime`. Botão "Save & Continue" volta ao ecrã anterior.

- **`PlanDetailsActivity.kt`** — Botão "Start Session" lança `SessionPlayerActivity` (já não é Toast).

### Missing ❌

#### 1. `SessionPlayerActivity` — Não está ligado ao `SessionController` (apenas TODOs)
```kotlin
// btnPause:
// TODO: replace with controller.pause() when SessionController is available

// btnStop:
// TODO: replace with controller.stop() when SessionController is available
```
Needs to call `SessionController.getInstance(context)` para pause/resume/stop. O `tvAngleValue` existe no layout mas **nunca é atualizado** — o loop de leitura de dados S2 não está implementado.

#### 2. Fluxo de mapeamento antes da sessão — ausente
`PlanDetailsActivity.btnStartSession` lança `SessionPlayerActivity` directamente, sem mostrar primeiro `SensorJointMappingFragment`. O intent não passa nem a lista de exercícios nem os MACs dos sensores:
```kotlin
val intent = Intent(this, SessionPlayerActivity::class.java)
val exerciseNames = arrayListOf<String>()  // ← lista vazia, nunca preenchida
startActivity(intent)
```
O fluxo correcto deveria ser: **"Start Session" → SensorJointMappingFragment → SessionController.configure() → SessionPlayerActivity**.

#### 3. `SessionSummaryActivity` — Dados reais nunca chegam
`SessionPlayerActivity.btnStop` passa tudo a zeros:
```kotlin
intent.putExtra("sampleCount", 0)   // sempre 0
intent.putExtra("errorCount", 0)    // sempre 0
intent.putExtra("startTime", "")    // sempre vazio
```
Precisa de chamar `SessionController.stop()` para obter `M1SessionSummary` e passar os valores reais (amostras, erros, timestamps, ROM final, reps).

#### 4. Ângulo em tempo real não aparece no ecrã
`tvAngleValue` e `tvAngleId` existem na layout mas nunca são actualizados. Precisa de um loop de UI (coroutine ou LiveData) que leia `SessionController.liveData` e escreva os graus no ecrã.

---

## Yiding Wang (`ywang` branch)

### Done ✅
- `SessionController.kt` — full singleton with `start()` / `stop()` / `pause()` / `resume()`, S2 polling loop, V2 upload loop, download loop (655 lines)
- `LegView.kt` — canvas-based joint angle visual
- Test scaffolding: `ExerciseActivity`, `LegViewTestActivity`, `TestS2Activity`

### Missing ❌

#### 1. `LiveFeedbackFragment` (MOD-M1-04) — **Not in codebase**
The SessionController is implemented but there is no Fragment or Activity that shows live data to the user during a session. Needs to be created with:
- Joint angle gauge (use `LegView.kt` already written)
- ROM tracker (current degrees / target degrees)
- Rep counter (from `SessionController.liveRepCount`)
- Deviation alert bar (from `SessionController.liveDeviationAlert`)
- Real-time update via `SessionController.liveData` LiveData or a coroutine loop polling `SessionController.currentData`

Suggested: `screens/session/LiveFeedbackFragment.kt` + `fragment_live_feedback.xml`

#### 2. `PlanDetailsActivity` — Session start wiring (MOD-M1-03) — **Stub only**
`app/src/main/java/com/example/limbmotionrecoveryapp/screens/plans/PlanDetailsActivity.kt:88`
```kotlin
// Currently:
findViewById<MaterialButton>(R.id.btnStartSession).setOnClickListener {
    Toast.makeText(this, "Session flow — coming soon", Toast.LENGTH_SHORT).show()
}
```
Needs to:
1. Call `SessionController.getInstance(context).configure(sessionId, userId, sensorMacToJointMap, exerciseType)`
2. Launch `LiveFeedbackFragment` (or a `SessionPlayerActivity`)
3. On session end, show summary screen

#### 3. Sensor-to-joint mapping step
Before `SessionController.start()` the app needs to know which BLE MAC → which joint (e.g. `knee`). Currently this mapping is hardcoded in test activities. The actual UI step (pairing sensors to joints before a session) is absent. Coordinate with whoever owns `SensorJointMappingFragment` (was Sara Costa's scope — check `origin/scosta` branch).

---

## Enhe Zhang (`ezhang` branch)

### Done ✅
- `FcmService.kt` — FCM token refresh, `onMessageReceived`, registers token to `POST /push/register`
- `HelpActivity`, `PrivacyActivity`, `SettingsActivity` + layouts
- `ProfileFragment.kt` navigation wired to the three new activities
- `ProgressViewModel` — replaces "endpoint missing" state with structured mock data
- `ProgressFragment` + `fragment_progress.xml` — simplified loading/content/error flow

### Missing ❌

#### 1. Notification channel creation — **Runtime crash on Android O+**
`FcmService.showNotification()` uses `CHANNEL_ID = "default_channel"` but this channel is never created. On Android 8+ the notification is silently dropped; on some versions it crashes.

Fix: create the channel in `MainActivity.onCreate()` or in a custom `Application` class:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val channel = NotificationChannel(
        "default_channel", "Rehab Reminders", NotificationManager.IMPORTANCE_DEFAULT
    )
    getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
}
```

#### 2. Progress screen — real data from `GET /progress/{userId}` (MOD-M1-05) — **Mock only**
`ProgressViewModel.load()` currently returns hardcoded mock data.  
When V2 delivers `GET /progress/{userId}`, replace `buildMockProgressData()` with a real `V2ApiClient.getProgress(userId, token)` call and parse the response into `ProgressData`.

#### 3. Appointments and Medications screens — **Stub ("Coming soon")**
`ProfileFragment.kt:42–46` — clicks on `itemAppointments` and `itemMedications` still show Toast.  
These were in scope (MOD-M1-05): create `AppointmentsActivity` and `MedicationsActivity` when V2 has the corresponding endpoints.

#### 4. Duplicate chart files — **Cleanup needed**
After Enhe moved charts to `screens/progress/charts/`, the old files still exist:
- `screens/progress/DonutChartView.kt` ← duplicate, should be deleted
- `screens/progress/LineChartView.kt` ← duplicate, should be deleted

`ProgressFragment.kt` already imports from `charts.*` (correct). Delete the old files to avoid confusion.

---

## V2 Backend — Questions / Blockers for Sergio Moniz (V2 team)

### 🔴 CRITICAL — Doctor invite token blocks patient registration

**Error observed:** `V2ApiClient: Request failed: Patient registration requires a doctor invite token`

Our `POST /auth/register` sends: `{ name, email, password, role: "patient" }`.  
The server is rejecting patient self-registration without an invite token.

**M1 needs one of the following from V2:**
- [ ] An option `role: "patient_self"` (or similar) that bypasses the invite requirement for test/demo accounts
- [ ] A test invite token (static string) we can hardcode or add to the registration UI during development
- [ ] Clarification on the intended patient onboarding flow: does the patient receive an invite link from a doctor? If so M1 needs a UI field for it in `RegisterActivity`

**Without this, no new patient account can be created.** Existing test accounts still work.

---

### 🟠 HIGH — `GET /progress/{userId}` endpoint missing

Progress screen (`ProgressFragment`) shows mock data because this endpoint does not exist yet.

**M1 needs:**
- Endpoint: `GET /progress/{userId}` with `Authorization: Bearer <token>`
- Expected response shape (M1 will parse):
```json
{
  "weekLabel": "Week 3",
  "rom": {
    "currentDegrees": 96,
    "targetDegrees": 120,
    "weeklyGainDegrees": 5,
    "history": [{ "date": "06-01", "degrees": 91 }, ...]
  },
  "adherence": {
    "weeklyPercent": 78,
    "completedExercises": 11,
    "totalExercises": 14,
    "skippedExercises": 3,
    "streakWeeks": 4,
    "weekDays": [{ "label": "M", "done": true, "isToday": false }, ...]
  },
  "pain": {
    "averageThisWeek": 4,
    "changeFromLastWeek": -1,
    "daily": [{ "date": "06-01", "level": 5 }, ...]
  },
  "weeklySummary": {
    "avgSessionMinutes": 18,
    "activeDays": 5,
    "romGainDegrees": 5
  }
}
```

---

### 🟠 HIGH — `GET /schedule/{userId}` response shape unclear

`PlansFragment` parses the schedule response assuming:
```
id, name, status (active/upcoming/completed), startDate, endDate,
totalSessions, completedSessions, phases[], doctorName
```
If V2 field names differ, the plan cards render blank silently.

**M1 needs:** A sample JSON response from V2 so M1 can align field names.

---

### 🟡 MEDIUM — "Today: X exercises" count not available

The active plan card shows "Today: X exercises" but no known endpoint provides this count. Either:
- V2 adds a `todayExercises` field in the `/schedule/{userId}` response, OR
- V2 provides `GET /schedule/{userId}/exercises?date=today`

---

### 🟡 MEDIUM — `GET /schedule/{scheduleId}/exercises` still missing

`PlanDetailsActivity` shows "Waiting for V2 exercises endpoint" in the start session button sub-text.  
M1 assumes: `GET /schedule/{scheduleId}/exercises` returns `[{ id, name, sets, reps, durationSec, description }]`

---

### 🟢 LOW — Phase tags and doctor name in schedule

M1 assumes `phases[]` array and `doctorName` string in each schedule item.  
If absent, the plan detail tags and doctor row are blank but nothing breaks.
