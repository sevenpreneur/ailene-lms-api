# Prompts

Endpoint for browsing a project's prompt library (curated prompts only — `is_self_created = false`), with search, pagination, and the caller's own submission status per prompt.

## Endpoints

### `POST {base_url}/api/v1/prompts`

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

### `POST {base_url}/api/v1/prompts/assigned`

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

Same shape and cases as `POST /api/v1/prompts` above (missing/invalid auth, expired session, blank `project_id`), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for this `project_id` |

### `POST {base_url}/api/v1/prompts/details`

Returns one prompt's full detail by id — unlike the list/assigned endpoints above, this doesn't filter by `status`/`is_self_created`, so it also works for a self-created or inactive prompt as long as the id exists and the caller has access to its project.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "id": 1
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
  "message": "prompt retrieved successfully",
  "data": {
    "id": 1,
    "name": "Draft Job Description",
    "scenario": "Anda adalah HR Generalist yang baru menerima permintaan rekrutmen...",
    "expected_output": "Draft job description lengkap dengan judul, tanggung jawab, dan kualifikasi...",
    "level_id": 3,
    "level_number": 2,
    "categories": [
      { "id": 87, "name": "Human Capital" }
    ],
    "xp_reward": 70,
    "is_self_created": false,
    "deadline_at": "2026-08-27T17:16:17.902873Z",
    "submitted_at": "2026-08-24T17:16:17.902873Z",
    "reviewed_at": null,
    "is_accepted": true
  }
}
```

`scenario`/`expected_output` are `lms_prompts.scenario`/`expected_output` directly. `level_id`/`level_number` are resolved from `lms_prompts.level_id` — `project_id` isn't part of the request, it's derived from there to check the caller's access. `deadline_at`/`submitted_at`/`reviewed_at`/`is_accepted` reflect the caller's own submission for this prompt (from their `lms_accesses` row in the prompt's project), same semantics as the list endpoint above — all `null` if they have no submission yet.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `id: must not be null` | missing `id` field |
| 404 | `NOT_FOUND` | `Prompt not found` | no `lms_prompts` row matches `id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the prompt's project |

### `POST {base_url}/api/v1/prompts/self-create`

Lets a student create their own prompt practice from scratch — not from the curated library, and not assigned by a champion — and immediately submits it for review, in one call. The level is hardcoded to `level_number = 2` (there is no champion-defined target for self-practice, so it can't be scoped to whatever level the student happens to be on), and `expected_output` is set to a fixed placeholder text since the champion reviews the actual input/output pair directly instead. The submission is routed to the champion of the caller's own group (`lms_accesses.group_id`) so it lands in their review queue, the same way a champion-assigned prompt would.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "name": "Draft Email Follow-Up",
  "scenario": "Saya butuh menulis email follow-up ke calon klien yang belum membalas proposal selama seminggu.",
  "input": "Tulis prompt untuk membuat draft email follow-up yang sopan dan tidak terkesan mendesak.",
  "output": "Subject: Menindaklanjuti Proposal Kami\n\nHalo [Nama], ingin menindaklanjuti proposal yang kami kirim minggu lalu...",
  "category_ids": [87]
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `project_id` | string | yes | |
| `name` | string | yes | Max 255 chars. |
| `scenario` | string | yes | The practice scenario the student wrote for themselves. |
| `input` | string | yes | The prompt the student actually wrote. Max 5000 chars. |
| `output` | string | yes | The AI's actual output for that prompt. Max 10000 chars. |
| `category_ids` | array of integer | yes | 1–2 `lms_categories.id` values. |

**Response** — `201 Created`

```json
{
  "success": true,
  "code": 201,
  "status": "CREATED",
  "message": "self-created prompt submitted successfully",
  "data": {
    "id": 15,
    "name": "Draft Email Follow-Up",
    "scenario": "Saya butuh menulis email follow-up ke calon klien yang belum membalas proposal selama seminggu.",
    "expected_output": "(Latihan mandiri — tanpa target output)",
    "level_id": 2,
    "level_number": 2,
    "categories": [
      { "id": 87, "name": "Human Capital" }
    ],
    "xp_reward": 70,
    "is_self_created": true,
    "deadline_at": null,
    "submitted_at": "2026-08-28T10:12:00.000Z",
    "reviewed_at": null,
    "is_accepted": false
  }
}
```

Same response shape as `POST /api/v1/prompts/details` — this endpoint creates the prompt and its submission, then returns the freshly-created prompt's detail. `deadline_at` is always `null` (self-practice has no deadline), `submitted_at` is set to the moment of the call, and `is_accepted` starts `false` until a champion reviews it (there's no review endpoint yet — see `Known gaps` in `AGENTS.md`). The `input`/`output` you sent, and which champion it was routed to (`assigned_by_access_id`), are stored on `lms_prompt_submissions` but aren't part of this response, since `/details` doesn't surface submission content at all.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 400 | `BAD_REQUEST` | `name: must not be blank` | missing/empty `name` field |
| 400 | `BAD_REQUEST` | `scenario: must not be blank` | missing/empty `scenario` field |
| 400 | `BAD_REQUEST` | `input: must not be blank` | missing/empty `input` field |
| 400 | `BAD_REQUEST` | `output: must not be blank` | missing/empty `output` field |
| 400 | `BAD_REQUEST` | `categoryIds: must not be empty` | missing/empty `category_ids` field |
| 400 | `BAD_REQUEST` | `categoryIds: size must be between 0 and 2` | more than 2 `category_ids` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |
| 400 | `BAD_REQUEST` | `You're not part of a group yet, so self-created practice isn't available.` | the caller's access has no `group_id` |
| 404 | `NOT_FOUND` | `No champion found for this group` | no `lms_accesses` row has `role = 'champion'` for that `project_id` + `group_id` |
| 404 | `NOT_FOUND` | `Some categories were not found` | one or more `category_ids` don't match an `lms_categories` row |
| 404 | `NOT_FOUND` | `Prompt level (L2) not found` | no `lms_levels` row has `level_number = 2` anywhere in the database — see the caveat below |

`level_number` is `UNIQUE` across the whole `lms_levels` table, not per project (matches the source this was ported from) — so in a deployment with more than one project, `level_number = 2` resolves to exactly one level system-wide, which may not belong to `project_id`. Single-project deployments aren't affected.

### `POST {base_url}/api/v1/prompts/self-assign`

Lets a student pick an existing prompt from the curated library themselves and put it into their own group champion's review queue — as if it had been assigned to them — without actually submitting an `input`/`output` yet. Idempotent: calling it again for the same prompt is a no-op if it's already routed to a champion (e.g. it was actually assigned by a champion in the meantime); it only fills in `assigned_by_access_id` when that's still empty. Unlike `self-create`, this doesn't create a new `lms_prompts` row — `prompt_id` must already exist and be `status = 'active'` (works for both curated and self-created prompts, as long as they're active).

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "prompt_id": 3
}
```

| Field | Type | Required |
|---|---|---|
| `prompt_id` | integer | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "prompt self-assigned successfully",
  "data": {
    "id": 3,
    "name": "Interview Question Generator",
    "scenario": "Anda akan mewawancarai kandidat untuk posisi Data Analyst...",
    "expected_output": "Daftar 10 pertanyaan wawancara yang relevan...",
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

Same response shape as `POST /api/v1/prompts/details`. `project_id` isn't part of the request — it's resolved internally from `prompt_id` via the prompt's level, same as `details`. Unlike `self-create`, `submitted_at` stays `null` here — this endpoint only reserves the prompt into the champion's queue, it doesn't submit `input`/`output` (there's no submit endpoint for this flow yet — see `Known gaps` in `AGENTS.md`).

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `promptId: must not be null` | missing `prompt_id` field |
| 404 | `NOT_FOUND` | `Prompt not found` | no `lms_prompts` row matches `prompt_id`, or it isn't `status = 'active'` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the prompt's project |
| 400 | `BAD_REQUEST` | `You're not part of a group yet, so self-assigned practice isn't available.` | the caller's access has no `group_id` |
| 404 | `NOT_FOUND` | `No champion found for this group` | no `lms_accesses` row has `role = 'champion'` for that project + `group_id` |
