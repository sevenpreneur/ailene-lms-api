# Student

Endpoints for a logged-in learner to check their own progress within a specific project.

## Endpoints

### `POST {base_url}/api/student/status`

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
