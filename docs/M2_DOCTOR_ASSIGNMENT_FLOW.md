# Doctor Assignment Flow — M1 ↔ M2 Integration

**Author:** M1 Team (Patient Mobile App)  
**Audience:** M2 Team (Admin Dashboard)  
**Date:** 2026-06-07  
**Status:** Proposal — awaiting M2 confirmation

---

## 1. Problem

The current M1 registration flow requires the patient to manually enter a doctor ID during sign-up. This is impractical: patients do not know their doctor's system ID. The proposed solution moves doctor assignment entirely to the admin dashboard (M2), where it belongs.

---

## 2. Proposed Flow Overview

```
[Patient]                      [V2 Backend]                    [Admin / M2 Dashboard]
    │                               │                                    │
    │  POST /auth/register          │                                    │
    │  { name, email, password }    │                                    │
    │──────────────────────────────►│                                    │
    │  ◄── { token, user }          │                                    │
    │  (doctor_id = null)           │                                    │
    │                               │                                    │
    │  POST /auth/login             │                                    │
    │──────────────────────────────►│                                    │
    │  ◄── { token, user }          │                                    │
    │                               │                                    │
    │  GET /auth/me                 │                                    │
    │──────────────────────────────►│                                    │
    │  ◄── { ..., doctor_id: null } │                                    │
    │                               │                                    │
    │  [Shows "Pending Assignment"  │                                    │
    │   screen — app locked]        │                                    │
    │                               │                                    │
    │                               │  GET /users?role=patient           │
    │                               │◄───────────────────────────────────│
    │                               │  ──► [ list of all patients ]      │
    │                               │                                    │
    │                               │  GET /users?role=clinician         │
    │                               │◄───────────────────────────────────│
    │                               │  ──► [ list of all clinicians ]    │
    │                               │                                    │
    │                               │  PATCH /users/:patientId           │
    │                               │  { "doctor_id": selectedDoctorId } │
    │                               │◄───────────────────────────────────│
    │                               │  ──► 200 OK                        │
    │                               │                                    │
    │  [Patient taps "Refresh"]     │                                    │
    │  GET /auth/me                 │                                    │
    │──────────────────────────────►│                                    │
    │  ◄── { ..., doctor_id: 42 }   │                                    │
    │  [App unlocks → MainActivity] │                                    │
```

---

## 3. M1 Behaviour (Patient App)

### 3.1 Registration

- M1 registers the patient with **name, email, and password only** — no doctor ID field.
- Endpoint: `POST /auth/register`
- After successful registration the patient is taken to the login screen.

### 3.2 Login & Gate Check

After every login, M1 calls `GET /auth/me` and inspects the `doctor_id` field:

| `doctor_id` value | M1 action |
|---|---|
| `null` or `0` | Show **"Pending Doctor Assignment"** screen. App is locked. |
| any positive integer | Proceed to **MainActivity** (normal app). |

### 3.3 Pending Screen

The pending screen shows a message to the patient informing them that their account is awaiting doctor assignment by the clinic administrator. It includes a **Refresh** button that re-calls `GET /auth/me`. When `doctor_id` becomes a positive integer the app unlocks automatically.

---

## 4. M2 Responsibilities (Admin Dashboard)

### 4.1 Unassigned Patients List

The dashboard should display a section listing all patients who have not yet been assigned a doctor.

**How to fetch:**

```
GET /users?role=patient
Authorization: Bearer <admin_token>
```

Filter the response client-side (or ask V2 to add a query param) for entries where `doctor_id` is `null` or `0`.

**Expected response shape per user:**
```json
{
  "id": 7,
  "name": "João Silva",
  "email": "joao@example.com",
  "role": "patient",
  "doctor_id": null,
  "created_at": "2026-06-07T10:00:00Z"
}
```

### 4.2 Assign Doctor — UI Flow

1. Admin clicks **"Assign Doctor"** next to an unassigned patient.
2. Dashboard fetches the full list of clinicians:

```
GET /users?role=clinician
Authorization: Bearer <admin_token>
```

3. Dashboard shows a modal/dropdown with clinician names and IDs.
4. Admin selects a clinician and confirms.
5. Dashboard sends:

```
PATCH /users/:patientId
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "doctor_id": <selectedClinicianId>
}
```

6. On `200 OK`, the patient row is removed from the "Unassigned" list and moves to "Assigned".

---

## 5. V2 Endpoints Used

| Step | Method | Endpoint | Who calls it |
|---|---|---|---|
| Register patient | `POST` | `/auth/register` | M1 |
| Login | `POST` | `/auth/login` | M1 |
| Check doctor assignment | `GET` | `/auth/me` | M1 |
| List all patients | `GET` | `/users?role=patient` | M2 |
| List all clinicians | `GET` | `/users?role=clinician` | M2 |
| Assign doctor to patient | `PATCH` | `/users/:patientId` | M2 |

All endpoints require a valid `Authorization: Bearer <token>` header except `POST /auth/register` and `POST /auth/login`.

---

## 6. Request / Response Examples

### 6.1 Register (M1 → V2)

**Request:**
```json
POST /auth/register
{
  "name": "João Silva",
  "email": "joao@example.com",
  "password": "secret123",
  "role": "patient"
}
```

**Response:**
```json
{
  "token": "eyJ...",
  "user": {
    "id": 7,
    "name": "João Silva",
    "email": "joao@example.com",
    "role": "patient",
    "doctor_id": null
  }
}
```

### 6.2 Check Doctor Assignment (M1 → V2)

**Request:**
```
GET /auth/me
Authorization: Bearer eyJ...
```

**Response (not yet assigned):**
```json
{
  "id": 7,
  "name": "João Silva",
  "role": "patient",
  "doctor_id": null
}
```

**Response (assigned):**
```json
{
  "id": 7,
  "name": "João Silva",
  "role": "patient",
  "doctor_id": 42
}
```

### 6.3 Assign Doctor (M2 → V2)

**Request:**
```json
PATCH /users/7
Authorization: Bearer <admin_token>

{
  "doctor_id": 42
}
```

**Response:**
```json
{
  "id": 7,
  "name": "João Silva",
  "role": "patient",
  "doctor_id": 42
}
```

---

## 7. Open Questions for V2 Team

These need to be confirmed with V2 before implementation:

| # | Question | Priority |
|---|---|---|
| 1 | Does `GET /users?role=patient` return all patients regardless of their `doctor_id` status? Or does it already filter? | High |
| 2 | Is there a dedicated endpoint to list only unassigned patients, e.g. `GET /users?role=patient&doctor_id=null`? If not, M2 will filter client-side. | Medium |
| 3 | Does `PATCH /users/:id` with `{ "doctor_id": X }` require admin role, or can any authenticated user call it? | High |
| 4 | Should `PATCH /auth/approve/:userId` also be called after doctor assignment, or is it a separate approval concept? | Medium |

---

## 8. Summary of Changes by Team

| Team | Change Required |
|---|---|
| **M1** | Remove doctor ID field from registration. Add gate check after login (`doctor_id == null` → pending screen). |
| **M2** | Add "Unassigned Patients" admin view. Add "Assign Doctor" action with clinician picker modal. |
| **V2** | Confirm `GET /users?role=patient` returns `doctor_id` in response. Confirm `PATCH /users/:id` accepts `doctor_id`. Clarify admin role requirement. |
