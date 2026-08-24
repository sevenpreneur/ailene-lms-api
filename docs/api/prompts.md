# Prompts

Endpoint for browsing a project's prompt library (curated prompts only — `is_self_created = false`), with search, pagination, and the caller's own submission status per prompt.

## Endpoints

### `POST {base_url}/api/prompts`

Returns a paginated, searchable list of a project's active, non-self-created prompts.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "search": "draft",
  "page": 1,
  "page_size": 20
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `project_id` | string | yes | |
| `search` | string | no | Case-insensitive substring match on the prompt's `name`. |
| `page` | integer | no | Defaults to `1`; clamped to `1` if less. |
| `page_size` | integer | no | Defaults to `20`; clamped to `100` max. |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "prompts retrieved successfully",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "Draft Job Description",
        "description": "Anda adalah HR Generalist yang baru menerima permintaan rekrutmen...",
        "level_number": 2,
        "categories": [
          { "id": 87, "name": "Human Capital" }
        ],
        "deadline_at": "2026-08-27T17:16:17.902873Z",
        "submitted_at": "2026-08-24T17:16:17.902873Z",
        "is_accepted": true
      }
    ],
    "metapaging": {
      "total_data": 10,
      "total_page": 2,
      "current_page": 1,
      "page_size": 5
    }
  }
}
```

`description` is the prompt's `scenario` text. `categories` is an array (a prompt can belong to more than one). `deadline_at`/`submitted_at`/`is_accepted` reflect the caller's own submission for each prompt (from their `lms_accesses` row in this project) — all `null` if they have no submission yet, or if they have no access to this project at all. Sorted by `level_number` ascending, then `name` ascending. `metapaging` follows the same shape every future list endpoint in this API will use.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
