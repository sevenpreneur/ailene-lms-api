# Student

Endpoints for a logged-in learner to check their own progress within a specific project.

## Endpoints

### `POST {base_url}/api/v1/student/status`

Returns the caller's XP, current level, and pre-assessment status for one project.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F"
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "student status retrieved successfully",
  "data": {
    "xp_count": 350,
    "current_level_number": 2,
    "has_pre_assessment": true
  }
}
```

`xp_count` is the sum of `lms_xp_earnings.xp_earned` for the caller's access in this project (`0` if none earned yet). `current_level_number` is `lms_levels.level_number` for the access's `current_level_id`, or `null` if no level has been set. `has_pre_assessment` is whether an `lms_pre_assessments` row exists for this access.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for this `project_id` |

Example error response (`404 Not Found`):

```json
{
  "success": false,
  "code": 404,
  "status": "NOT_FOUND",
  "message": "No access found for this project"
}
```

### `POST {base_url}/api/v1/student/chapters`

Returns a project's active chapters, ordered by `session_date` ascending, each annotated with the caller's own completion progress.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F"
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "chapters retrieved successfully",
  "data": [
    {
      "id": 5,
      "name": "Paham AI & pakai dengan benar",
      "description": "Paham cara kerja AI tanpa jargon...",
      "session_date": "2026-06-26T02:00:00Z",
      "duration_minutes": 120,
      "location_name": "Kantor Pusat Hutama Karya",
      "location_url": "https://maps.app.goo.gl/sEBqLrLxZWiLmXGp8",
      "method": "offline",
      "level": {
        "id": 1,
        "level_number": 1,
        "name": "Foundation"
      },
      "done_tasks": 0,
      "total_tasks": 5,
      "progress": "not_started"
    }
  ]
}
```

`total_tasks` is the number of active quizzes + videos + materials in that chapter. `done_tasks` counts the caller's own completed items among those (a quiz counts once it has any `is_completed` submission, videos/materials count via `lms_video_completions`/`lms_material_completions`). `progress` is derived: `not_started` when nothing's done, `completed` when `done_tasks >= total_tasks`, otherwise `in_progress`. Only chapters whose level belongs to `project_id` are returned; inactive chapters are excluded.

**Errors**

Same shape and cases as `student/status` above (missing/invalid auth, expired session, blank `project_id`, or no access for the project).

### `POST {base_url}/api/v1/student/levels`

Returns a project's active `lms_levels` rows, ordered by `level_number` ascending.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F"
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "levels retrieved successfully",
  "data": [
    {
      "id": 1,
      "level_number": 1,
      "name": "Foundation"
    },
    {
      "id": 2,
      "level_number": 2,
      "name": "Operator Dasar & Lanjut"
    }
  ]
}
```

Levels with `status = 'inactive'` are excluded, and levels belonging to other projects never appear regardless of `project_id`. An empty array means the project has no active levels.

**Errors**

Same shape and cases as `student/chapters` above (missing/invalid auth, expired session, or blank `project_id` — no `404`, since this endpoint doesn't require an `lms_accesses` row for the caller).
