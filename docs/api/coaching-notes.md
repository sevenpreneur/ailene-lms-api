# Coaching Notes

Endpoint for listing `lms_coaching_notes` rows for a project, from either side of the relationship: as the student who received them, or as the champion who sent them.

## Endpoints

### `POST {base_url}/api/v1/coaching-notes`

Returns the caller's own coaching notes for a project, newest first. `role` picks which side of the note the caller is filtered as — it isn't validated against the caller's actual `lms_accesses.role`, so asking for a side the caller isn't on simply returns an empty list.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "role": "student"
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `role` | string (`student` / `champion`) | yes |

`role: "student"` returns every note where the caller is the receiving student (`lms_coaching_notes.student_access_id` = the caller's access in this project) — notes that came in to them. `role: "champion"` returns every note where the caller is the sender (`champion_access_id` = the caller's access) — notes they sent to students in this project.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "coaching notes retrieved successfully",
  "data": [
    {
      "student": {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "name": "Budi Santoso",
        "avatar": "https://example.com/avatars/budi.png"
      },
      "champion": {
        "id": "9c858901-8a57-4791-81fe-4c455b099bc9",
        "name": "Siti Champion",
        "avatar": null
      },
      "text": "Kerja bagus di quiz minggu ini, coba lebih konsisten latihan prompt ya.",
      "created_at": "2026-08-20T09:12:00.000Z"
    }
  ]
}
```

`student`/`champion` are that note's `lms_users` rows (`id`, `name` from `full_name`, `avatar`, `null` if not set). An empty array means the caller has no notes on that side of the relationship for this project.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 400 | `BAD_REQUEST` | `role: must not be null` | missing/invalid `role` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |

### `POST {base_url}/api/v1/coaching-notes/create`

Sends a coaching note from the caller (as champion) to a student in the project. Champion-only — any other role on the caller's access is rejected.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "student_access_id": "N2rTk8pXQz1LBcVmoA9-K",
  "text": "Kerja bagus di quiz minggu ini, coba lebih konsisten latihan prompt ya."
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `student_access_id` | string | yes |
| `text` | string | yes |

`student_access_id` is the target student's `lms_accesses.id` within this project (e.g. from `POST /api/v1/student/leaderboard`, whose entries carry `access_id`).

**Response** — `201 Created`

```json
{
  "success": true,
  "code": 201,
  "status": "CREATED",
  "message": "coaching note created successfully",
  "data": {
    "student": {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "name": "Budi Santoso",
      "avatar": "https://example.com/avatars/budi.png"
    },
    "champion": {
      "id": "9c858901-8a57-4791-81fe-4c455b099bc9",
      "name": "Siti Champion",
      "avatar": null
    },
    "text": "Kerja bagus di quiz minggu ini, coba lebih konsisten latihan prompt ya.",
    "created_at": "2026-08-27T09:12:00.000Z"
  }
}
```

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 400 | `BAD_REQUEST` | `studentAccessId: must not be blank` | missing/empty `student_access_id` field |
| 400 | `BAD_REQUEST` | `text: must not be blank` | missing/empty `text` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |
| 403 | `FORBIDDEN` | `Only champions can send coaching notes.` | the caller's access in `project_id` isn't `role = 'champion'` |
| 404 | `NOT_FOUND` | `Student access not found in this project` | `student_access_id` doesn't exist, or belongs to a different project than `project_id` |
