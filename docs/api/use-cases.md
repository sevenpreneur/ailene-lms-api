# Use Cases

Endpoints for the project's curated library, assignments, submissions, and guided adoption. The guided flow starts from a student's own work without requiring a library item, persists each modal step, and creates an L3 self-created use case only when submitted. Existing library endpoints remain available.

## Endpoints

## Guided adoption

All endpoints below are **POST**, use `Authorization: Bearer <active LMS JWT>`, and require the caller's student or champion access in that project. A champion uses the same learner flow from Mode Student; the guided session belongs to their own access row. Sessions are private to their owner; another user's session returns 404, and sponsors cannot create them. Responses use the usual `success`, `code`, `status`, `message`, `data` envelope. The six modal steps are intake, example, current process, workflow preview, execution, and outcome.

### `POST {base_url}/api/v1/use-cases/guided/details`

Resume by `{ "session_id": 1 }`, or look up the latest open personal draft with `{ "project_id": "<project-id>" }`. A lookup with no draft returns `data: null`. To resume a library item, send `use_case_id` and optionally `project_id`; a shared library item requires an explicit project. Completed personal submissions can still be retrieved by session id, while project-only lookup starts a new draft after submission.

The session shape returned by `details`, `save`, and `generate` is:

```json
{
  "id": 1,
  "use_case_id": null,
  "project_id": "<project-id>",
  "status": "steps_ready",
  "current_step": 4,
  "daily_task": "Saya merekap laporan penjualan mingguan dari beberapa cabang.",
  "frequency": "weekly",
  "example_text": "Cabang A: target 50 juta, realisasi 45 juta.",
  "example_file_url": null,
  "sensitive_data_confirmed": true,
  "minutes_without_ai": 90.00,
  "current_process": "Kumpulkan data, cek angka, lalu tulis ringkasan.",
  "current_tools": ["Excel"],
  "pain_point": "Menyusun ringkasan memakan waktu.",
  "steps": [
    {
      "id": 11,
      "order": 1,
      "title": "Siapkan data",
      "instruction": "Samarkan identitas pada data sumber.",
      "suggested_tool": "Excel",
      "prompt_template": "",
      "estimated_minutes": 10,
      "verification": "Tidak ada identitas pribadi.",
      "is_done": false,
      "skipped_reason": null,
      "note": null,
      "done_at": null
    }
  ],
  "summary": "Rekap laporan dengan pemeriksaan angka.",
  "estimated_minutes_with_ai": 35,
  "risks": ["Cocokkan angka dengan sumber."],
  "regenerate_remaining": 2,
  "is_generating": false,
  "created_at": "2026-10-20T10:00:00Z",
  "updated_at": "2026-10-20T10:01:00Z"
}
```

The example truncates `steps`; a generated workflow contains 3–7 steps. Statuses are `intake`, `steps_ready`, `in_progress`, `ready_to_submit`, and `submitted`. `is_generating` is true only while a generation lease is active (five minutes). `regenerate_remaining` counts all available generations, including the first, from a total allowance of three.

### `POST {base_url}/api/v1/use-cases/guided/save`

Upserts a draft and persists navigation. For a new personal draft, provide `project_id` without `use_case_id`. Subsequent calls should send `session_id`. Without it, the open draft for that student/project (or selected library item) is reused; concurrent creates are serialized. No AI call occurs.

```json
{
  "project_id": "<project-id>",
  "current_step": 4,
  "daily_task": "Saya merekap laporan penjualan mingguan dari beberapa cabang.",
  "frequency": "weekly",
  "example_text": "Cabang A: target 50 juta, realisasi 45 juta.",
  "sensitive_data_confirmed": true,
  "minutes_without_ai": 90,
  "current_process": "Kumpulkan data, cek angka, lalu tulis ringkasan.",
  "current_tools": ["Excel"],
  "pain_point": "Menyusun ringkasan memakan waktu."
}
```

`current_step` is always required (1–6). Other fields are partial updates: omitted or `null` leaves a value unchanged; an empty string clears optional text/file fields, and `[]` clears tools. Limits: `daily_task` 3000 characters, `example_text` 10000, file URL 500, `current_process` 5000, `pain_point` 2000, and up to 20 nonblank tools of 100 characters each. Minutes must be positive, at most 599999.40, with at most two decimals. Frequency uses the existing `daily`, `weekly`, `monthly` enum.

Navigation validates all preceding steps: step 2 needs a task of at least 30 trimmed characters and frequency; step 3 needs an example plus sensitive-data confirmation; step 4 needs baseline minutes and current process; step 5 needs a generated workflow; step 6 needs every step completed or explicitly skipped. Saving step 5 accepts the preview and enables the checklist.

Changing intake invalidates the generated workflow and checklist, retaining generation audit history and quota. Changing either example field resets confirmation unless `sensitive_data_confirmed: true` is sent in that request. When the generation allowance is exhausted, edits that would invalidate the existing workflow return 409, preserving the usable workflow. Submitted intake is immutable.

**Files:** `example_file_url` accepts HTTPS URLs whose paths end in PDF, DOCX, XLSX, CSV, PNG, JPG, or JPEG (query parameters are allowed). Upload uses the existing client/storage flow. This API never downloads the URL or extracts its contents. Before generation, also supply anonymized contents or a summary in `example_text`; a file-only example can be saved but cannot generate a workflow.

### `POST {base_url}/api/v1/use-cases/guided/generate`

Request: `{ "session_id": 1, "feedback": "Gunakan Copilot untuk langkah ringkasan." }`. Feedback is optional, max 2000 characters. Returns the updated session with a 3–7 step workflow in Indonesian, reusable prompts, verification checks, risks, and estimated duration. It returns to preview (step 4); accept with `save` step 5 before execution. Regeneration replaces the checklist and step ids, so clients must replace their cached steps.

The backend reuses DeepSeek's configured client. It reserves an attempt under a row lock, calls AI outside the database transaction, validates JSON/schema, retries malformed output once, and saves the workflow and audit record atomically. There are three successful generations per session. Provider or validation failures release the attempt and preserve the previous workflow. A process interruption leaves a five-minute lease; after expiry another request can retry, with that interrupted attempt still counted. Mutations during an active generation return 409. Do not automatically retry generation on a request timeout: first resume the session.

### `POST {base_url}/api/v1/use-cases/guided/check`

```json
{ "session_id": 1, "step_id": 11, "is_done": true, "note": "Angka sudah cocok." }
```

Returns the updated step only. `note` and `skipped_reason` are optional, max 2000 characters. To skip, send `is_done: false` with a nonblank `skipped_reason`. To undo a skip, send `is_done: false, skipped_reason: ""`. Completing a step clears its skip reason and assigns the server's `done_at`; unchecking clears that timestamp. Session status becomes `ready_to_submit` once every step is done/skipped. Read `details` to refresh the full session. Unknown or superseded step ids return 404.

### `POST {base_url}/api/v1/use-cases/guided/submit`

```json
{
  "session_id": 1,
  "name": "Rekap laporan mingguan",
  "category_ids": [87],
  "outcome_proof": "https://example.test/hasil.pdf",
  "hours_with_ai": 0.5,
  "description": "Ringkasan sudah dicocokkan dengan sumber.",
  "ai_tool": "ChatGPT",
  "type": "data_analysis",
  "testimonial": "AI membantu menyusun laporan lebih rapi. Saya tetap memeriksa semua angka sebelum membagikannya.",
  "helpfulness_rating": 4,
  "share_consent": false
}
```

Only `name` (max 255, defaults to task text) and `category_ids` (0–2 existing ids) are optional. Proof must be a valid HTTP/HTTPS link, max 500 characters. Actual hours must be positive, at most 9999.99, and have at most two decimals. Description is nonblank, max 5000; tool is nonblank, max 255; testimonial is 50–5000 trimmed characters; rating is 1–5; explicit boolean consent is required, with `false` valid. `type` uses the existing use-case enum below. Baseline hours and frequency are derived from the saved intake; clients cannot override them here.

Returns `data`:

```json
{
  "session": { "id": 1, "status": "submitted", "current_step": 6 },
  "submission_id": 12,
  "use_case_id": 7,
  "submission_status": "awaiting_review",
  "hours_saved": 1.00
}
```

`session` contains the full session shape (abbreviated above). New personal work creates an L3 self-created use case and routes its submission to the student's group champion in one transaction. Existing library work reuses its assignment. Missing group/champion prevents submission without partial writes. Savings are `max(0, minutes_without_ai / 60 - hours_with_ai)`, rounded to two decimals by the backend. Estimates generated by AI are never used as actual duration.

Retries are idempotent: pending/accepted submissions return the existing result without new rows or notifications. If a champion requests revision, the student can resubmit outcome fields on the same session/submission; intake and workflow remain fixed. `submission_status` is `awaiting_review`, `needs_revision`, or `accepted`. Champion details include the full guided session, testimonial, rating, consent, and savings; review and XP use existing endpoints/rules. Consent is stored for future sharing and does not publish anything.

**Existing submit compatibility:** `/api/v1/use-cases/submit` accepts optional `guided_session_id`, `testimonial`, `helpfulness_rating`, and `share_consent`. A matching completed session is mandatory when that id is supplied. For an already linked submission the guided path is enforced automatically, including on revision; omitted testimonial/rating/consent preserve saved values. Existing required legacy fields still apply, but guided frequency/baseline are taken from intake. The legacy response shape is unchanged. Ordinary submissions without a guided session retain existing behavior.

### Guided errors

| HTTP | Meaning |
|---|---|
| 400 | Invalid input, incomplete prior steps, missing anonymized text for generation, invalid URL, or sensitive-data pattern detected |
| 401 | Missing, expired, inactive, or invalid JWT |
| 403 | Project access is not a student |
| 404 | Session/access/use case not found, another student's session, or stale workflow step |
| 409 | Generation in progress, submitted intake edit, exhausted-quota intake edit, or conflicting session/submission |
| 429 | Three workflow generations already used (`TOO_MANY_REQUESTS`) |
| 502 | AI provider failed or returned unusable output after validation/retry |
| 503 | `DEEPSEEK_API_KEY` unavailable |

Text checks catch recognizable NIK/account/salary patterns and lists of emails/phone numbers. They are an extra guard, not automatic anonymization; the caller must still redact examples and explicitly confirm. Workflow generation does not execute tools, create automations, or publish the student's data.

### Verification

`GuidedRulesTest` and `GuidedWorkflowGeneratorTest` cover validation, savings, schema retries, and regeneration context. `GuidedUseCaseIntegrationTest` uses a real isolated PostgreSQL branch, mocked auth/AI, and transaction rollbacks to cover persistence, authorization, quota/audits, recovery, submission idempotency, Champion details, and revision through the legacy endpoint. Set `DB_URL` to the test branch before running `./mvnw test`; Spring-context tests apply Flyway migrations.

`GuidedUseCaseNativeSmokeTest` is opt-in. Start the native Docker image locally with the same isolated database and runtime secrets (including `DEEPSEEK_API_KEY`), then run `./mvnw test -Dtest=GuidedUseCaseNativeSmokeTest -Dguided.native.base-url=http://127.0.0.1:18080` with that branch's explicit `DB_URL`. It uses real JWTs and one real AI generation with synthetic data, checks concurrent checklist updates and idempotent submission, reads Champion details, then removes its own fixtures. Do not point the test or container at production.

For a provider-only check without a database, run `./mvnw test -Dtest=GuidedWorkflowLiveTest -Dguided.ai.live=true`; it reads the existing `DEEPSEEK_API_KEY` and sends synthetic office-work data. Both live tests are disabled by default.

For local native verification on an 8 GB Docker Desktop VM, the compiler was run from a temporary copy of the Dockerfile using `sh mvnw -B -Pnative -DskipTests "-DbuildArgs=-J-Xmx6g,-J-XX:NewRatio=5,-J-XX:ActiveProcessorCount=4,--parallelism=4" native:compile`. These are build-time resource settings; the application's runtime configuration is unchanged.

## Library and existing submissions

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

Same response shape as `POST /api/v1/use-cases/details`. `project_id` isn't part of the request — it's resolved internally from `use_case_id` via the use case's level, same as `details`. Unlike `self-create`, `submitted_at` stays `null` here — this endpoint only reserves the use case into the champion's queue, it doesn't submit the practice write-up. Call `POST /api/v1/use-cases/submit` below once it's ready.

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

### `POST {base_url}/api/v1/use-cases/submit`

Submits (or resubmits) the caller's practice write-up for a use case that's already routed to a champion — either via `self-assign` above or a champion-assigned task. Mirrors `POST /api/v1/prompts/submit` exactly. Overwrites whatever was submitted before and bumps `submitted_at` to now, so it can be called repeatedly while awaiting review. Once a champion has accepted the submission, further calls are rejected — the student has to be reassigned (or self-assign again) to get another shot.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "use_case_id": 1,
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
| `use_case_id` | integer | yes | |
| `outcome_proof` | string | yes | Max 500 chars. |
| `hours_with_ai` | number | yes | 0–9999.99. |
| `hours_without_ai` | number | yes | 0–9999.99. |
| `description` | string | yes | Max 5000 chars. |
| `ai_tool` | string | yes | Max 255 chars. |
| `frequency` | string | yes | `daily` / `weekly` / `monthly` / `occasionally`. |
| `type` | string | yes | `workflow_automation` / `content_creation` / `data_analysis` / `research` / `communication` / `decision_support` / `learning` / `other`. |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "use case submitted successfully",
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
    "submitted_at": "2026-08-28T12:05:00.000Z",
    "reviewed_at": null,
    "is_accepted": false
  }
}
```

Same response shape as `POST /api/v1/use-cases/details`. `project_id` isn't part of the request — it's resolved internally from `use_case_id` via the use case's level, same as `details`. None of `outcome_proof`/`hours_with_ai`/`hours_without_ai`/`ai_tool`/`frequency`/`type` you sent are part of this response, since `/details` doesn't surface submission content at all — only `submitted_at` moves. Note `description` here is `lms_use_cases.description` (the library item's description), not the practice write-up's `description` field — those are two different columns that happen to share a name.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `useCaseId: must not be null` | missing `use_case_id` field |
| 400 | `BAD_REQUEST` | `outcomeProof: must not be blank` | missing/empty `outcome_proof` field |
| 400 | `BAD_REQUEST` | `hoursWithAi: must not be null` | missing `hours_with_ai` field |
| 400 | `BAD_REQUEST` | `hoursWithoutAi: must not be null` | missing `hours_without_ai` field |
| 400 | `BAD_REQUEST` | `description: must not be blank` | missing/empty `description` field |
| 400 | `BAD_REQUEST` | `aiTool: must not be blank` | missing/empty `ai_tool` field |
| 400 | `BAD_REQUEST` | `frequency: must not be null` | missing `frequency` field |
| 400 | `BAD_REQUEST` | `type: must not be null` | missing `type` field |
| 404 | `NOT_FOUND` | `Use case not found` | no `lms_use_cases` row matches `use_case_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the use case's project |
| 404 | `NOT_FOUND` | `Assignment not found` | no `lms_use_case_submissions` row exists for `(caller, use_case_id)`, or it exists but `assigned_by_access_id` is still empty — call `self-assign` first, or wait for a champion to assign it |
| 400 | `BAD_REQUEST` | `Assignment already accepted, cannot resubmit.` | the submission's `is_accepted` is already `true` |
