# Use Cases

Endpoint for browsing a project's use case library (curated use cases only — `is_self_created = false`), with search, pagination, and the caller's own submission status per use case. Mirrors `docs/api/prompts.md` exactly, just backed by `lms_use_cases`/`lms_use_case_submissions`.

## Endpoints

### `POST {base_url}/api/v1/use-cases`

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
        "reviewed_at": null,
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

`description` is `lms_use_cases.description` directly. `categories` is an array (a use case can belong to more than one). `deadline_at`/`submitted_at`/`reviewed_at`/`is_accepted` reflect the caller's own submission for each use case (from their `lms_accesses` row in this project) — all `null` if they have no submission yet, or if they have no access to this project at all. Sorted by `level_number` ascending, then `name` ascending. `metapaging` follows the same shape as every other paginated list endpoint in this API.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |

### `POST {base_url}/api/v1/use-cases/assigned`

Returns the use cases a champion has assigned to the caller specifically (`lms_use_case_submissions.assigned_by_access_id IS NOT NULL`) — not the general library, and not self-initiated practice. Mirrors `POST /api/v1/prompts/assigned` exactly.

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
| `has_submitted` | boolean | no | Omit for no filter. `true` → only use cases with `submitted_at` set. `false` → only use cases not yet submitted. |
| `is_accepted` | boolean | no | Omit for no filter. Independent `AND` with `has_submitted` — not an OR/complementary toggle. |

Common combos: `has_submitted: false` (+ optionally `is_accepted: false`, which is redundant but harmless since an unsubmitted use case is never accepted) for a **to-do list**; `has_submitted: true` alone (omit `is_accepted`) for **submission history**, accepted or not.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "assigned use cases retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Otomasi Screening CV Massal",
      "description": "Tim recruiter menerima ratusan CV untuk satu lowongan...",
      "level_id": 3,
      "level_number": 2,
      "categories": [
        { "id": 87, "name": "Human Capital" }
      ],
      "xp_reward": 70,
      "is_accepted": false,
      "deadline_at": "2026-08-28T18:09:15.615951Z",
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

Not paginated (this is always scoped to just the caller's own assignments). Sorted by `deadline_at` ascending. `xp_reward` is `lms_use_cases.xp_reward` directly. `level_id`/`level_number` are the use case's `lms_levels.id`/`level_number`. `assigned_by` is the champion who made the assignment (their `lms_users` row via `assigned_by_access_id`).

**Errors**

Same shape and cases as `POST /api/v1/use-cases` above (missing/invalid auth, expired session, blank `project_id`), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for this `project_id` |

### `POST {base_url}/api/v1/use-cases/details`

Returns one use case's full detail by id — mirrors `POST /api/v1/prompts/details` exactly, just backed by `lms_use_cases`/`lms_use_case_submissions`. Unlike the list/assigned endpoints above, this doesn't filter by `status`/`is_self_created`, so it also works for a self-created or inactive use case as long as the id exists and the caller has access to its project.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "id": 2
}
```

| Field | Type | Required |
|---|---|---|
| `id` | integer | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "use case retrieved successfully",
  "data": {
    "id": 2,
    "name": "Generate Job Description Multi-Platform",
    "description": "HR perlu menulis ulang job description untuk berbagai kanal...",
    "level_id": 3,
    "level_number": 2,
    "categories": [
      { "id": 87, "name": "Human Capital" }
    ],
    "xp_reward": 70,
    "is_self_created": false,
    "deadline_at": "2026-08-29T17:28:16.541534Z",
    "submitted_at": "2026-08-24T17:28:16.541534Z",
    "reviewed_at": null,
    "is_accepted": false
  }
}
```

`description` is `lms_use_cases.description` directly. `level_id`/`level_number` are resolved from `lms_use_cases.level_id` — `project_id` isn't part of the request, it's derived from there to check the caller's access. `deadline_at`/`submitted_at`/`reviewed_at`/`is_accepted` reflect the caller's own submission for this use case (from their `lms_accesses` row in the use case's project), same semantics as the list endpoint above — all `null` if they have no submission yet.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `id: must not be null` | missing `id` field |
| 404 | `NOT_FOUND` | `Use case not found` | no `lms_use_cases` row matches `id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the use case's project |

### `POST {base_url}/api/v1/use-cases/self-create`

Lets a student log their own AI use case from scratch — not from the curated library, and not assigned by a champion — and immediately submits it for review, in one call. Mirrors `POST /api/v1/prompts/self-create` exactly, just backed by `lms_use_cases`/`lms_use_case_submissions`. The level is hardcoded to `level_number = 3`. The submission is routed to the champion of the caller's own group (`lms_accesses.group_id`) so it lands in their review queue, the same way a champion-assigned use case would.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "name": "Otomasi Balasan Email Customer Support",
  "category_ids": [87],
  "outcome_proof": "Screenshot draft balasan email otomatis: https://example.com/proof.png",
  "hours_with_ai": 1.5,
  "hours_without_ai": 4,
  "description": "Menggunakan AI untuk menyusun draft balasan email customer support dari tiket yang masuk.",
  "ai_tool": "ChatGPT",
  "frequency": "weekly",
  "type": "workflow_automation"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `project_id` | string | yes | |
| `name` | string | yes | Max 255 chars. |
| `category_ids` | array of integer | yes | 1–2 `lms_categories.id` values. |
| `outcome_proof` | string | yes | Max 500 chars. |
| `hours_with_ai` | number | yes | 0–9999.99. |
| `hours_without_ai` | number | yes | 0–9999.99. |
| `description` | string | yes | Max 5000 chars. Also becomes `lms_use_cases.description`. |
| `ai_tool` | string | yes | Max 255 chars. |
| `frequency` | string | yes | `daily` / `weekly` / `monthly` / `occasionally`. |
| `type` | string | yes | `workflow_automation` / `content_creation` / `data_analysis` / `research` / `communication` / `decision_support` / `learning` / `other`. |

**Response** — `201 Created`

```json
{
  "success": true,
  "code": 201,
  "status": "CREATED",
  "message": "self-created use case submitted successfully",
  "data": {
    "id": 8,
    "name": "Otomasi Balasan Email Customer Support",
    "description": "Menggunakan AI untuk menyusun draft balasan email customer support dari tiket yang masuk.",
    "level_id": 5,
    "level_number": 3,
    "categories": [
      { "id": 87, "name": "Human Capital" }
    ],
    "xp_reward": 70,
    "is_self_created": true,
    "deadline_at": null,
    "submitted_at": "2026-08-28T10:15:00.000Z",
    "reviewed_at": null,
    "is_accepted": false
  }
}
```

Same response shape as `POST /api/v1/use-cases/details` — this endpoint creates the use case and its submission, then returns the freshly-created use case's detail. `deadline_at` is always `null` (self-practice has no deadline), `submitted_at` is set to the moment of the call, and `is_accepted` starts `false` until a champion reviews it (there's no review endpoint yet — see `Known gaps` in `AGENTS.md`). `outcome_proof`/`hours_with_ai`/`hours_without_ai`/`ai_tool`/`frequency`/`type`, and which champion it was routed to (`assigned_by_access_id`), are stored on `lms_use_case_submissions` but aren't part of this response, since `/details` doesn't surface submission content at all.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 400 | `BAD_REQUEST` | `name: must not be blank` | missing/empty `name` field |
| 400 | `BAD_REQUEST` | `categoryIds: must not be empty` | missing/empty `category_ids` field |
| 400 | `BAD_REQUEST` | `categoryIds: size must be between 0 and 2` | more than 2 `category_ids` |
| 400 | `BAD_REQUEST` | `outcomeProof: must not be blank` | missing/empty `outcome_proof` field |
| 400 | `BAD_REQUEST` | `hoursWithAi: must not be null` | missing `hours_with_ai` field |
| 400 | `BAD_REQUEST` | `hoursWithoutAi: must not be null` | missing `hours_without_ai` field |
| 400 | `BAD_REQUEST` | `description: must not be blank` | missing/empty `description` field |
| 400 | `BAD_REQUEST` | `aiTool: must not be blank` | missing/empty `ai_tool` field |
| 400 | `BAD_REQUEST` | `frequency: must not be null` | missing `frequency` field |
| 400 | `BAD_REQUEST` | `type: must not be null` | missing `type` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |
| 400 | `BAD_REQUEST` | `You're not part of a group yet, so self-created practice isn't available.` | the caller's access has no `group_id` |
| 404 | `NOT_FOUND` | `No champion found for this group` | no `lms_accesses` row has `role = 'champion'` for that `project_id` + `group_id` |
| 404 | `NOT_FOUND` | `Some categories were not found` | one or more `category_ids` don't match an `lms_categories` row |
| 404 | `NOT_FOUND` | `Use case level (L3) not found` | no `lms_levels` row has `level_number = 3` anywhere in the database — see the caveat on `POST /api/v1/prompts/self-create` |

### `POST {base_url}/api/v1/use-cases/self-assign`

Lets a student pick an existing use case from the curated library themselves and put it into their own group champion's review queue — as if it had been assigned to them — without actually submitting the practice write-up yet. Mirrors `POST /api/v1/prompts/self-assign` exactly. Idempotent: calling it again for the same use case is a no-op if it's already routed to a champion; it only fills in `assigned_by_access_id` when that's still empty. Unlike `self-create`, this doesn't create a new `lms_use_cases` row — `use_case_id` must already exist and be `status = 'active'` (works for both curated and self-created use cases, as long as they're active).

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "use_case_id": 1
}
```

| Field | Type | Required |
|---|---|---|
| `use_case_id` | integer | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "use case self-assigned successfully",
  "data": {
    "id": 1,
    "name": "Otomasi Screening CV Massal",
    "description": "Tim recruiter menerima ratusan CV untuk satu lowongan...",
    "level_id": 3,
    "level_number": 2,
    "categories": [
      { "id": 87, "name": "Human Capital" }
    ],
    "xp_reward": 70,
    "is_self_created": false,
    "deadline_at": null,
    "submitted_at": null,
    "reviewed_at": null,
    "is_accepted": false
  }
}
```

Same response shape as `POST /api/v1/use-cases/details`. `project_id` isn't part of the request — it's resolved internally from `use_case_id` via the use case's level, same as `details`. Unlike `self-create`, `submitted_at` stays `null` here — this endpoint only reserves the use case into the champion's queue, it doesn't submit the practice write-up (there's no submit endpoint for this flow yet — see `Known gaps` in `AGENTS.md`).

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `useCaseId: must not be null` | missing `use_case_id` field |
| 404 | `NOT_FOUND` | `Use case not found` | no `lms_use_cases` row matches `use_case_id`, or it isn't `status = 'active'` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the use case's project |
| 400 | `BAD_REQUEST` | `You're not part of a group yet, so self-assigned practice isn't available.` | the caller's access has no `group_id` |
| 404 | `NOT_FOUND` | `No champion found for this group` | no `lms_accesses` row has `role = 'champion'` for that project + `group_id` |
