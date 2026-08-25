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
        "reviewed_at": null,
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

`description` is the prompt's `scenario` text. `categories` is an array (a prompt can belong to more than one). `deadline_at`/`submitted_at`/`reviewed_at`/`is_accepted` reflect the caller's own submission for each prompt (from their `lms_accesses` row in this project) — all `null` if they have no submission yet, or if they have no access to this project at all. Sorted by `level_number` ascending, then `name` ascending. `metapaging` follows the same shape every future list endpoint in this API will use.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |

### `POST {base_url}/api/prompts/assigned`

Returns the prompts a champion has assigned to the caller specifically (`lms_prompt_submissions.assigned_by_access_id IS NOT NULL`) — not the general library, and not self-initiated practice.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "has_submitted": false,
  "is_accepted": false
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `project_id` | string | yes | |
| `has_submitted` | boolean | no | Omit for no filter. `true` → only prompts with `submitted_at` set. `false` → only prompts not yet submitted. |
| `is_accepted` | boolean | no | Omit for no filter. Independent `AND` with `has_submitted` — not an OR/complementary toggle. |

Common combos: `has_submitted: false` (+ optionally `is_accepted: false`, which is redundant but harmless since an unsubmitted prompt is never accepted) for a **to-do list**; `has_submitted: true` alone (omit `is_accepted`) for **submission history**, accepted or not.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "assigned prompts retrieved successfully",
  "data": [
    {
      "id": 2,
      "name": "Interview Question Generator",
      "description": "Anda akan mewawancarai kandidat untuk posisi Data Analyst...",
      "level_id": 3,
      "level_number": 2,
      "categories": [
        { "id": 87, "name": "Human Capital" }
      ],
      "xp_reward": 70,
      "is_accepted": false,
      "deadline_at": "2026-08-29T18:08:14.651505Z",
      "reviewed_at": null,
      "submitted_at": null,
      "assigned_by": {
        "id": "1df9f6b8-0911-4b65-acb6-3dc797bbe8e9",
        "name": "Akmal Luthfiansyah",
        "avatar": "https://lh3.googleusercontent.com/a/xxx"
      }
    }
  ]
}
```

Not paginated (this is always scoped to just the caller's own assignments). Sorted by `deadline_at` ascending. `xp_reward` is `lms_prompts.xp_reward` directly. `level_id`/`level_number` are the prompt's `lms_levels.id`/`level_number`. `assigned_by` is the champion who made the assignment (their `lms_users` row via `assigned_by_access_id`).

**Errors**

Same shape and cases as `POST /api/prompts` above (missing/invalid auth, expired session, blank `project_id`), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for this `project_id` |
