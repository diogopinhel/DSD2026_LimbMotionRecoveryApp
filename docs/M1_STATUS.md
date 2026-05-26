# M1 — Patient Mobile App · Status Report

**Team:** M1 (Diogo Pinhel)  
**Date:** 2026-05-14  
**Repository:** https://github.com/diogopinhel/DSD2026_LimbMotionRecoveryApp  
**Branch:** `dpinhel`

---

## What Has Been Implemented

### 1. Authentication (MOD-M1-01) ✅
- **Login screen** — email + password, calls `POST /auth/login`, stores token and user info in `SharedPreferences`
- **Register screen** — full name + email + password with live password strength indicator and terms checkbox; calls `POST /auth/register` then auto-logs in
- **Auto-skip** — if a token is already stored, the app bypasses the login screen on launch
- **Logout** — clears stored token and returns to login

### 2. Home Screen ✅
- Personalised greeting (time-aware: morning / afternoon / evening) with the patient's first name
- **Recovery progress bar** — calculates current week and total weeks from the active plan's `startDate` / `endDate` (sourced from `GET /schedule/{userId}`)
- **Sensor status banner** — two states:
  - 🟠 **Not connected** — amber banner with "Connect" button
  - 🟢 **Connected** — green banner with pulsing dot and "Tracking ready" label
- **Live sensor data card** — appears only when sensor is connected; shows joint angle (ROM) and movement acceleration (populated during an active session via S2 module)
- **Today at a glance** — sessions completed vs. total and plan progress % from V2 schedule API
- **Start Exercises** button — navigates to the Plans tab

### 3. Plans Screen (MOD-M1-05) ✅
- Fetches rehab schedule via `GET /schedule/{userId}`
- Groups plans into **Active / Upcoming / Completed** sections
- Summary strip (active plan count, total sessions, overall % complete) computed client-side from the schedule list
- Tapping a plan opens **Plan Details** — exercise list grouped by phase (Warm Up / Strength / Cooldown), with To Do and Completed tabs

### 4. Progress Screen ✅
- Three tabs: **Overview**, **Pain**, and **Range of Motion**
- Custom `LineChartView` (ROM over time) and `DonutChartView` (adherence %) — both built as standalone canvas views
- Weekly day-by-day adherence calendar
- Currently renders with placeholder structure; will populate with real data once V2 progress endpoints exist (see `docs/V2_API_REQUIREMENTS.md`)

### 5. Profile Screen ✅
- Displays user name and email from `GET /auth/me`
- Sensor section shows paired device info
- Settings rows (Help, Privacy, Logout)

### 6. BLE Sensor Connection Flow (MOD-M1-02) ✅ *(new, ready for testing)*
- **`SensorRepository`** — Kotlin singleton that manages the full BLE lifecycle:
  - Scans for WitMotion sensors using `BluetoothLeScanner` with a service UUID filter (`0000ffe0-0000-1000-8000-00805f9b34fb`)
  - 10-second scan timeout with automatic error state
  - Connects via `SensorService` (S1 module) using the discovered MAC address
  - Polls connection status every 500 ms until confirmed; 12-second connect timeout
  - Monitors connection health after connect; reverts to IDLE if sensor drops
- **`SensorActivity`** — full-screen sensor connection UI with four states:
  1. **Searching** — animated pulsing rings + checklist (sensor on, BT enabled, scanning)
  2. **Found** — device card showing sensor name and MAC; "Connect LIMBS Sensor" button
  3. **Connecting** — spinner while GATT handshake completes
  4. **Connected** — green success screen with device name and "Done" button (returns to Home)
- Error state with friendly message and "Try again" button
- "Continue without sensor" option available at every step
- Home screen banner updates automatically when sensor connects or disconnects

---

## Module Boundaries (Do Not Modify)

| Module | Path | Status |
|--------|------|--------|
| S1 — BLE driver | `com.dsd.s1` | Read-only (Java, owned by S1 team) |
| S2 — Data processing | `com.dsd.s2` | Read-only (Kotlin) |
| m1/api — V2 client | `com.dsd.m1.api` | Extendable (Kotlin) |
| App UI | `com.example.limbmotionrecoveryapp` | M1 owns this |

---

## What Is Left to Implement

### High Priority
| # | Module | Feature | Blocker |
|---|--------|---------|---------|
| 1 | MOD-M1-03 | **Exercise session controller** — start/stop S2 session, bind sensor to joint, stream data to V2 | Needs `POST /sessions` wired to S2; also needs `GET /schedule/{scheduleId}/exercises` (❌ missing from V2) |
| 2 | MOD-M1-04 | **Real-time feedback UI** — joint angle display during a session, deviation alerts | Needs MOD-M1-03 complete first |
| 3 | MOD-M1-05 | **Plan details with real exercises** | Needs `GET /schedule/{scheduleId}/exercises` (❌ missing from V2) |

### Medium Priority
| # | Module | Feature | Blocker |
|---|--------|---------|---------|
| 4 | MOD-M1-05 | Wire **Progress screen** with real data | Needs `GET /progress/{userId}` (❌ missing from V2) |
| 5 | MOD-M1-01 | Wire **Profile screen** stats (condition, ROM, adherence, streak) | Needs extra fields in `GET /auth/me` (⚠️ incomplete) |
| 6 | MOD-M1-06 | **Push notifications** — register FCM token via `POST /push/register` | FCM project not yet configured |

### Low Priority / Future
- "Next appointment" card on Home — requires `GET /appointments/{userId}/next` (❌ not in V2)
- "Pain check-in" card on Home — requires `POST /pain/{userId}/checkin` (❌ not in V2)
- "Learn & recover" section — static educational content (no backend needed, can be hardcoded)
- Offline data caching

---

## Known Technical Notes

### S1/S2 Constraints
- **S1 has no BLE scanner** — it connects by MAC address. Scanning is done by M1 using Android's `BluetoothLeScanner`.
- **S2 requires 2 sensors** to compute a joint angle (`JointAngleComputer` needs a sensor pair per joint). A single sensor gives raw acceleration and orientation but not ROM.
- **No battery level** — `SensorSample` (S1 output) does not include battery percentage. The "Battery X%" value from the design is not achievable with the current S1 protocol.
- **No heart rate** — WitMotion IMU sensors measure acceleration + gyroscope + orientation. No heart rate or temperature.
- **S2 data only during active session** — `S2Module.data.read()` throws if called outside a session. Live sensor data on the Home screen will only be available when a session is running.

### BLE Permissions
All required permissions are declared in `AndroidManifest.xml`:
- `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` (Android 12+)
- `ACCESS_FINE_LOCATION` (required for BLE scanning on Android ≤ 11)

`SensorActivity` requests permissions at runtime if not yet granted.

---

## What the Team Should Test Right Now

### Priority 1 — BLE Sensor Connection
This is the first end-to-end hardware flow and needs testing on a **physical Android 12+ device** (emulators cannot use BLE).

**Setup:**
1. Build and install the app (`./gradlew installDebug`)
2. Have a WitMotion BLE sensor (WT901BLECL or similar) powered on and within 2 metres

**Test cases:**

| # | Scenario | Expected result |
|---|----------|----------------|
| T1 | Open app → log in → Home screen | Banner shows "Sensor not connected" in amber |
| T2 | Tap "Connect" on the amber banner | `SensorActivity` opens; animated rings + "Searching for sensor…" |
| T3 | Sensor is on and nearby | After a few seconds: "Sensor found!" screen with device name and MAC |
| T4 | Tap "Connect LIMBS Sensor" | Connecting spinner → "Sensor connected!" screen |
| T5 | Tap "Done" | Returns to Home; banner is now green "LIMBS Sensor connected" |
| T6 | Turn sensor off after connecting | Home banner should revert to amber within ~5 seconds |
| T7 | Tap "Connect" when sensor is OFF | Scan times out after 10 s → Error state with "No WitMotion sensor found nearby" |
| T8 | Tap "Try again" from error state | Restarts scan correctly |
| T9 | Tap "Continue without sensor" at any step | Closes activity, Home stays in amber state |
| T10 | Deny Bluetooth permission | Error state with "Bluetooth permissions are required" message |

**What to report if something fails:**
- Which test case failed (T1–T10)
- The exact error message shown (or logcat output via `adb logcat -s SensorWorker SensorState S1RealModule`)
- Sensor model and Android version

### Priority 2 — Login / Register Flow
Already tested internally, but please confirm with the V2 backend running:
- Register a new account → verify `POST /auth/register` works
- Login → verify `POST /auth/login` returns `token` + `user.id` + `user.name`
- Home screen shows greeting with the user's first name

### Priority 3 — Plans Screen
- Log in and navigate to Plans tab
- If `GET /schedule/{userId}` returns data, plans should appear grouped by status
- **If the screen shows an error**, the V2 `/schedule` response shape is likely different from what M1 expects — check `docs/V2_API_REQUIREMENTS.md` section 2 for required field names

---

## Files for Other Teams

| File | Recipient | Purpose |
|------|-----------|---------|
| `docs/V2_API_REQUIREMENTS.md` | **V2 team** | Full list of endpoints M1 needs, required response shapes, missing endpoints that block implementation |

---

## How to Build

```bash
# Prerequisites: Android Studio Panda 4+ or Android SDK command-line tools
# Physical device required for BLE (minSdk 31 — Android 12+)

./gradlew assembleDebug      # Build APK
./gradlew installDebug       # Install on connected device
adb logcat -s SensorWorker SensorState SensorActivity SensorRepository   # Monitor BLE logs
```

---

*For questions contact Diogo Pinhel (M1 team lead) on WeChat or the project group chat.*
