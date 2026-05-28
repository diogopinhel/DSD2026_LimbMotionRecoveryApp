# Enhe Zhang — DSD2026 Task Completion Report

> Project: DSD2026_LimbMotionRecoveryApp  
> Owner: Enhe Zhang  
> Branch: `ezhang`  
> Generated: 2026-05-26  
> Based on: DSD2026_EnheZhang_Tasks_EN.md

---

## Task Completion Overview

| # | Task | Module | Priority | Completion Date | Status |
|---|------|--------|----------|-----------------|--------|
| 1 | Push Notifications (FCM) | MOD-M1-06 | High | 2026-05-23 | ✅ Completed |
| 2 | Progress Page - Real Data Integration | MOD-M1-05 | High | 2026-05-23 | ✅ Completed |
| 3 | Profile Subpages (Settings/Help/Privacy) | MOD-M1-01 | Medium | 2026-05-21 | ✅ Completed |
| 4 | Profile Subpages (My Recovery, etc.) | MOD-M1-01 | Low | Pending | ⏳ Blocked |

---

## Completed Tasks

### Task 3: Profile Subpages (Settings/Help/Privacy) — Completed 2026-05-21

**Priority:** Medium  
**Dependency:** None  
**Actual Completion:** 2026-05-21

#### Subtasks Completed

##### 3.1 Settings Page ✅
- Created `SettingsActivity.kt`
- Created layout `activity_settings.xml` with:
  - Language selection (中文 / English)
  - Unit switch (° / rad)
  - Notification toggle
  - Dark mode toggle
- Used `SharedPreferences` to store user preferences
- Replaced Toast in `ProfileFragment.kt` with navigation to SettingsActivity

##### 3.2 Help Page ✅
- Created `HelpActivity.kt`
- Created layout `activity_help.xml` with:
  - Static FAQ content in ScrollView
  - "Contact Support" button
- Implemented email intent: `support@limbmotion.com`
- Replaced Toast in `ProfileFragment.kt` with navigation to HelpActivity

##### 3.3 Privacy Page ✅
- Created `PrivacyActivity.kt`
- Created layout `activity_privacy.xml` with:
  - Privacy policy summary text
  - "View Full Privacy Policy" button
- Implemented web link intent to `https://limbmotion.com/privacy`
- Replaced Toast in `ProfileFragment.kt` with navigation to PrivacyActivity

#### Verification
- [x] All "Coming Soon" Toasts replaced with actual Activities
- [x] Settings page stores preferences correctly
- [x] Help page email intent works
- [x] Privacy page link opens correctly

---

### Task 1: Push Notifications (FCM) — Completed 2026-05-23

**Priority:** High  
**Dependency:** Diogo provided `google-services.json`  
**Actual Completion:** 2026-05-23

#### Steps Completed

##### Step 1: Firebase Config ✅
- Received `google-services.json` from Diogo
- Placed in `app/` directory

##### Step 2: Modify `app/build.gradle.kts` ✅
- Added `id("com.google.gms.google-services")` to plugins block
- Added Firebase dependencies:
  - `implementation(platform("com.google.firebase:firebase-bom:33.5.0"))`
  - `implementation("com.google.firebase:firebase-messaging")`

##### Step 3: Modify Project Root `build.gradle.kts` ✅
- Added `id("com.google.gms.google-services") version "4.4.2" apply false` to plugins block

##### Step 4: Create `FcmService.kt` ✅
- Created `app/src/main/java/com/example/limbmotionrecoveryapp/notifications/FcmService.kt`
- Implemented:
  - `onNewToken()`: Save token to SharedPreferences + register to backend
  - `onMessageReceived()`: Display system notification
  - `registerTokenToBackend()`: API call placeholder
  - `showNotification()`: Build and display notification

##### Step 5: Modify `LoginViewModel.kt` ✅
- Added FCM token retrieval on login success
- Token saved to SharedPreferences
- Prepared backend registration call

##### Step 6: Register Service in `AndroidManifest.xml` ✅
- Added `FcmService` declaration inside `<application>` tag
- Added intent filter for `com.google.firebase.MESSAGING_EVENT`

##### Step 7: Testing ✅
- [x] Tested on real Android device
- [x] FCM token obtained successfully (verified in Logcat)
- [x] Test notification sent from Firebase Console received on device

---

### Task 2: Progress Page - Real Data Integration — Completed 2026-05-23

**Priority:** High  
**Dependency:** Sergio provided `GET /progress/{userId}`  
**Actual Completion:** 2026-05-23

#### Steps Completed

##### Step 1: API Request ✅
- Requested `GET /progress/{userId}` from Sergio Moniz (CC: Diogo)
- API response format confirmed:
  ```json
  {
    "weeklyRom": [85, 90, 92, 95, 98, 100, 105],
    "weeklyPain": [6, 5, 5, 4, 4, 3, 3],
    "adherencePct": 78,
    "streakDays": 12
  }
  ```

##### Step 2: Add API Method in `V2ApiClient.kt` ✅
- Added `ProgressDto` data class
- Implemented `getProgress(userId, token)` suspend function
- Handled authorization header and JSON deserialization

##### Step 3: Mock Data Testing ✅
- Created `loadProgressMock()` in `ProgressViewModel.kt`
- Verified UI rendering with mock data:
  - `LineChartView` displays 7 ROM data points correctly
  - `DonutChartView` shows 78% adherence
  - No crashes observed

##### Step 4: Real Data Integration ✅
- Replaced `loadProgressMock()` with `loadProgress()`
- Implemented coroutine-based API call in `viewModelScope.launch`
- Bound API response to LiveData / StateFlow
- Called `viewModel.loadProgress()` in `ProgressFragment.kt`'s `onViewCreated()`

##### Step 5: Error Handling ✅
- Added try-catch block for API calls
- Implemented error state display (`_errorMessage`)
- App doesn't crash on offline or API 500 errors

##### Step 6: Testing ✅
- [x] Mock data phase: UI renders correctly
- [x] Real API phase: HTTP 200 in Logcat, UI displays real data
- [x] Error scenarios: App handles offline/500 gracefully

---

## Pending Tasks

### Task 4: Profile Subpages (My Recovery, Appointments, Medications)

**Priority:** Low  
**Dependency:** Waiting for Sergio to provide APIs  
**Status:** ⏳ Blocked

#### Required APIs
| Page | Required API | Contact | Status |
|------|-------------|---------|--------|
| My Recovery | `GET /recovery-history/{userId}` | Sergio Moniz | Waiting |
| Appointments | `GET /appointments/{userId}` | Sergio Moniz | Waiting |
| Medications | `GET /medications/{userId}` | Sergio Moniz | Waiting |

#### Next Steps (After APIs Ready)
1. Request APIs from Sergio (WeChat, CC Diogo)
2. Add methods in `V2ApiClient.kt`
4. Create Activities + Layouts
5. Replace corresponding Toasts in `ProfileFragment.kt`

---

## Summary

### Completed
- ✅ Task 3 (Profile Subpages - Settings/Help/Privacy) — 2026-05-21
- ✅ Task 1 (Push Notifications - FCM) — 2026-05-23
- ✅ Task 2 (Progress Page - Real Data Integration) — 2026-05-23

### Blocked (Waiting for APIs)
- ⏳ Task 4 (Profile Subpages - My Recovery/Appointments/Medications)

### Branch Status
- Working branch: `ezhang`
- All changes committed and pushed to `ezhang`
- Ready for PR review by Diogo

---
