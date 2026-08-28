# Pre-Assessment

Endpoints for a student's one-time onboarding questionnaire (`lms_pre_assessments`) and its generated report (`lms_pre_assessment_reports`). A student can only ever have one pre-assessment per project — `access_id` is unique on `lms_pre_assessments` — so `create` is a one-shot submission, not upsertable. The report has two independent parts: a deterministic pillar score computed on the fly from the raw answers (`score`), and a separate AI-generated recommendation list that's produced asynchronously after submission (`recommendations`).

## Endpoints

### `POST {base_url}/api/v1/pre-assessment/create`

Submits the student's pre-assessment questionnaire for a project, seeds its report row (`status: "pending"`), and schedules the AI recommendation generation on QStash. One-shot: a second call for the same access is rejected.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "ai_use_frequency": "weekly",
  "ai_tools_used": ["ChatGPT", "Copilot"],
  "ai_limitations": ["Kadang jawabannya kurang akurat"],
  "output_review": "sometimes",
  "use_cases": ["Menulis email", "Ringkasan rapat"],
  "team_adoption": "personal",
  "concrete_example": "Meringkas notulen rapat mingguan jadi poin aksi",
  "model_selection": "rarely",
  "multimodal_use": "never",
  "workflow_reuse": "sometimes",
  "prompt_comfort": "basic",
  "prompt_iteration": "sometimes",
  "refine_scenario": "manual",
  "professional_attitude": "supportive",
  "data_safety_check": "often",
  "publish_unchecked": "rarely",
  "biggest_challenge": "Sulit menyusun prompt yang detail untuk kasus yang kompleks",
  "training_expectation": "Ingin bisa membuat automation sederhana dengan AI",
  "motivation": "curious"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `project_id` | string | yes | |
| `ai_use_frequency` | string | yes | `never` / `tried` / `weekly` / `daily` / `intensive`. |
| `ai_tools_used` | array of string | yes | At least 1 item. |
| `ai_limitations` | array of string | yes | At least 1 item. |
| `output_review` | string | yes | `no_check` / `sometimes` / `always` / `cross_check` / `no_use`. |
| `use_cases` | array of string | yes | At least 1 item. |
| `team_adoption` | string | yes | `none` / `personal` / `pilot` / `policy` / `integrated`. |
| `concrete_example` | string | no | Max 255 chars. |
| `model_selection` | string | yes | `never` / `rarely` / `sometimes` / `often` / `always`. |
| `multimodal_use` | string | yes | Same 5 values as `model_selection`. |
| `workflow_reuse` | string | yes | Same 5 values as `model_selection`. |
| `prompt_comfort` | string | yes | `none` / `basic` / `decent` / `structured` / `expert`. |
| `prompt_iteration` | string | yes | Same 5 values as `model_selection`. |
| `refine_scenario` | string | yes | `targeted` / `switch_tool` / `manual` / `restart`. |
| `professional_attitude` | string | yes | `too_risky` / `cautious` / `neutral` / `supportive` / `essential`. |
| `data_safety_check` | string | yes | Same 5 values as `model_selection`. |
| `publish_unchecked` | string | yes | Same 5 values as `model_selection`. |
| `biggest_challenge` | string | yes | |
| `training_expectation` | string | yes | |
| `motivation` | string | yes | `mandatory` / `curious` / `tentative` / `ready` / `eager`. |

**Response** — `201 Created`

```json
{
  "success": true,
  "code": 201,
  "status": "CREATED",
  "message": "pre-assessment submitted successfully",
  "data": { "id": 4 }
}
```

`id` is the new `lms_pre_assessments.id` — pass it to nothing, since `score`/`recommendations` below are scoped by the caller's own access, not by this id. Scheduling the QStash job (`POST /api/v1/pre-assessment/report-callback` below) happens synchronously inside this call, but a publish failure doesn't fail the submission — if `QSTASH_TOKEN`/`APP_BASE_URL` aren't configured, or the publish call itself fails, `create` still succeeds and the report row is simply left `status: "pending"` with nothing queued.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 400 | `BAD_REQUEST` | `aiUseFrequency: must not be null` | missing `ai_use_frequency` field (same pattern for every other required field — Java property name + `must not be null`/`must not be blank`/`must not be empty`) |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |
| 400 | `BAD_REQUEST` | `Pre-assessment already submitted.` | the caller's access already has an `lms_pre_assessments` row |

### `POST {base_url}/api/v1/pre-assessment/score`

Returns the caller's raw pre-assessment answers plus a deterministic pillar score computed on the fly from those answers — 6 pillars (`ai_foundation`, `prompting`, `tool_fluency`, `use_case_diversity`, `ai_habit`, `agentic`), each scored 1–5. This is pure arithmetic over the stored answers, not AI — it's always available immediately after `create`, unlike `recommendations` below.

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

**Response** — `200 OK`, caller has submitted a pre-assessment:

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "pre-assessment score retrieved successfully",
  "data": {
    "pre_assessment": {
      "id": 4,
      "access_id": "N2rTk8pXQz1LBcVmoA9-K",
      "ai_use_frequency": "weekly",
      "ai_tools_used": ["ChatGPT", "Copilot"],
      "ai_limitations": ["Kadang jawabannya kurang akurat"],
      "output_review": "sometimes",
      "use_cases": ["Menulis email", "Ringkasan rapat"],
      "team_adoption": "personal",
      "concrete_example": "Meringkas notulen rapat mingguan jadi poin aksi",
      "model_selection": "rarely",
      "multimodal_use": "never",
      "workflow_reuse": "sometimes",
      "prompt_comfort": "basic",
      "prompt_iteration": "sometimes",
      "refine_scenario": "manual",
      "professional_attitude": "supportive",
      "data_safety_check": "often",
      "publish_unchecked": "rarely",
      "biggest_challenge": "Sulit menyusun prompt yang detail untuk kasus yang kompleks",
      "training_expectation": "Ingin bisa membuat automation sederhana dengan AI",
      "motivation": "curious",
      "created_at": "2026-08-28T10:00:00Z"
    },
    "report": {
      "pillars": [
        { "key": "ai_foundation", "label": "AI Foundation", "score": 2.7 },
        { "key": "prompting", "label": "Prompting", "score": 2.5 },
        { "key": "tool_fluency", "label": "Tool Fluency", "score": 1.8 },
        { "key": "use_case_diversity", "label": "Use Case", "score": 1.9 },
        { "key": "ai_habit", "label": "AI Habit", "score": 2.5 },
        { "key": "agentic", "label": "Agentic", "score": 1.8 }
      ],
      "avg": 2.2,
      "strongest": { "key": "ai_foundation", "label": "AI Foundation" },
      "weakest": { "key": "tool_fluency", "label": "Tool Fluency" },
      "quote": "Meringkas notulen rapat mingguan jadi poin aksi"
    }
  }
}
```

**Response** — `200 OK`, caller hasn't submitted a pre-assessment yet:

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "pre-assessment score retrieved successfully",
  "data": { "pre_assessment": null, "report": null }
}
```

`pre_assessment` is every column of the caller's `lms_pre_assessments` row verbatim (`access_id` is their own `lms_accesses.id` in this project). `report.strongest`/`weakest` are the pillar with the highest/lowest score (ties keep whichever pillar is listed first in `pillars`). `report.quote` is `concrete_example` if the student filled it in, else `biggest_challenge`, else a fixed fallback sentence. `report.avg` is the mean of all 6 pillar scores, rounded to 1 decimal — same rounding as every individual pillar score.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |

### `POST {base_url}/api/v1/pre-assessment/recommendations`

Returns the status and content of the AI-generated recommendation list for the caller's pre-assessment — separate from `score` above so the client can render the deterministic pillars immediately and poll only this endpoint while the AI generation finishes in the background.

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

**Response** — `200 OK`, still generating:

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "pre-assessment recommendations retrieved successfully",
  "data": {
    "status": "pending",
    "recommendations": null,
    "error_message": null,
    "generated_at": null
  }
}
```

**Response** — `200 OK`, done:

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "pre-assessment recommendations retrieved successfully",
  "data": {
    "status": "completed",
    "recommendations": {
      "time_saved_label": "~4,9 Jam/minggu",
      "items": [
        {
          "source": "Menyusun notulen rapat mingguan",
          "title": "Otomasi ringkasan rapat jadi poin aksi",
          "impact": "Tinggi",
          "speed": "~70% lebih cepat",
          "description": "Rekam rapat lalu minta AI merangkum jadi poin aksi terstruktur.",
          "lessons": ["Prompting Dasar", "Ringkasan & Ekstraksi"]
        }
      ]
    },
    "error_message": null,
    "generated_at": "2026-08-28T10:02:30Z"
  }
}
```

`status` is `pending` (queued, not started), `processing`, `completed`, or `failed`. `recommendations` is the raw JSON blob written by the AI worker — its shape (`time_saved_label` + `items[]` in the example above) isn't validated or reshaped by this endpoint, it's passed through verbatim from `lms_pre_assessment_reports.recommendations`. If the caller has no pre-assessment at all yet, this still returns `status: "pending"` (matching a freshly-seeded report row) rather than an error, so the client can render the same "waiting" state either way. `error_message` is only set when `status = "failed"`.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |

### `POST {base_url}/api/v1/pre-assessment/report-callback` (internal)

Not a student-facing endpoint. This is the callback `create` schedules via QStash (`QSTASH_TOKEN`/`APP_BASE_URL` env vars, same mechanism as `quizzes/auto-submit`) to actually generate the AI recommendations: flips the report to `status: "processing"`, calls OpenAI (`OPENAI_API_KEY` env var) with the deterministic pillar scores plus the raw answers as context, asks for 3–5 use-case recommendations constrained to the caller's project curriculum (`lms_chapters.name`, via Structured Outputs so the model's reply is guaranteed to match the stored shape), then writes the result back as `status: "completed"` with `recommendations` populated and `generated_at` set. Gated by the same static `SECRET_KEY` bearer token as `POST /api/v1/hello-world` (see `docs/api/auth.md`) rather than a student JWT — QStash is configured to forward that header on delivery.

Unlike `quizzes/auto-submit`, a failure here does **not** swallow the error into a 200 response: the report row is written to `status: "failed"` with `error_message` (truncated to 500 chars) *before* the exception is rethrown, so the HTTP response back to QStash is a genuine error status — QStash's own retry policy then reattempts delivery on its usual backoff, which re-flips the row through `processing` again on each attempt. There's no separate manual retry/regenerate endpoint — a stuck `failed` report only recovers via a fresh `create` (not possible, one-shot) or a future dedicated retry endpoint.

**Authorization:** `Bearer <SECRET_KEY>`.

**Request**

```json
{ "pre_assessment_id": 4 }
```

| Field | Type | Required |
|---|---|---|
| `pre_assessment_id` | integer | yes |

**Response** — `200 OK`, `data` is always `null`.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Bearer token is invalid` | wrong or missing bearer token |
| 400 | `BAD_REQUEST` | `preAssessmentId: must not be null` | missing `pre_assessment_id` field |
| 404 | `NOT_FOUND` | `Pre-assessment report not found` | no `lms_pre_assessment_reports` row matches `pre_assessment_id` (shouldn't happen — `create` always seeds one); this lookup happens before the row is flipped to `processing`, so nothing is written on this specific failure |
| 404 | `NOT_FOUND` | `Pre-assessment not found` / `Access not found` | the `lms_pre_assessments`/`lms_accesses` row disappeared after the report row was seeded (shouldn't happen in practice) — report is written to `status: "failed"` first |
| 500 | `INTERNAL_SERVER_ERROR` | `Unexpected error` | `OPENAI_API_KEY` missing, the OpenAI call failed, or its response didn't parse/validate — report is written to `status: "failed"` first, with the real reason in `error_message` |

## Known gaps

There's no endpoint yet to list a project's own curriculum chapters as a flat name list outside of this callback — `ChapterRepository.findActiveChapterNames` exists only to feed the AI prompt above, not as a public API. There's also no manual retry/regenerate endpoint for a report stuck in `failed` after QStash exhausts its own retries.
