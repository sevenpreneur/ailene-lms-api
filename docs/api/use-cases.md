# Use Cases

Endpoint for browsing a project's use case library (curated use cases only — `is_self_created = false`), with search, pagination, and the caller's own submission status per use case. Mirrors `docs/api/prompts.md` exactly, just backed by `lms_use_cases`/`lms_use_case_submissions`.

## Endpoints

### `POST {base_url}/api/use-cases`

Returns a paginated, searchable list of a project's active, non-self-created use cases.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "search": "generate",
  "page": 1,
  "page_size": 20
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `project_id` | string | yes | |
| `search` | string | no | Case-insensitive substring match on the use case's `name`. |
| `page` | integer | no | Defaults to `1`; clamped to `1` if less. |
| `page_size` | integer | no | Defaults to `20`; clamped to `100` max. |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "use cases retrieved successfully",
  "data": {
    "list": [
      {
        "id": 2,
        "name": "Generate Job Description Multi-Platform",
        "description": "HR perlu menulis ulang job description untuk berbagai kanal...",
        "level_number": 2,
        "categories": [
          { "id": 87, "name": "Human Capital" }
        ],
        "deadline_at": "2026-08-29T17:28:16.541534Z",
        "submitted_at": "2026-08-24T17:28:16.541534Z",
        "is_accepted": false
      }
    ],
    "metapaging": {
      "total_data": 1,
      "total_page": 1,
      "current_page": 1,
      "page_size": 20
    }
  }
}
```

`description` is `lms_use_cases.description` directly. `categories` is an array (a use case can belong to more than one). `deadline_at`/`submitted_at`/`is_accepted` reflect the caller's own submission for each use case (from their `lms_accesses` row in this project) — all `null` if they have no submission yet, or if they have no access to this project at all. Sorted by `level_number` ascending, then `name` ascending. `metapaging` follows the same shape as every other paginated list endpoint in this API.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
