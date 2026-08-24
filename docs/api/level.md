# Level

Endpoint for listing a project's active curriculum levels.

## Endpoints

### `POST {base_url}/api/levels`

Returns the active `lms_levels` rows for a project, ordered by `level_number` ascending.

**Authorization:** `Bearer <SECRET_KEY>` — see `docs/api/auth.md` for what this token is.

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
      "name": "Foundations"
    },
    {
      "id": 2,
      "level_number": 2,
      "name": "Advanced Prompting"
    }
  ]
}
```

Levels with `status = 'inactive'` are excluded, and levels belonging to other projects never appear regardless of `project_id`. An empty array means the project has no active levels.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Bearer token is invalid` | header present, but doesn't match `SECRET_KEY` |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
