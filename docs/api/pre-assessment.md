# Pre-Assessment

Endpoints for a student's one-time CFTE AIPF pre-assessment. The questions live in the database as versioned item banks (`lms_pa_versions`/`lms_pa_items`/`lms_pa_item_options`). Each project is pointed at exactly one version through `lms_pa_project_versions`, and several projects can share a version, so their results stay comparable. Each access gets exactly one attempt (`lms_pa_attempts`, `UNIQUE (access_id)`) with one `lms_pa_answers` row per question. `submit` only stores answers. `score` grades the written answers with DeepSeek, computes the band the first time it's called, and stores the result. The older hardcoded questionnaire (`lms_pre_assessments`) has no endpoints left: `create`, `recommendations`, `recommendations/regenerate`, and `report-callback` were removed.

## Endpoints

### `POST {base_url}/api/v1/pre-assessment/questions`

Returns the pre-assessment version assigned to the caller's project, with every question in display order, plus the caller's own attempt if they already submitted one.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google` or `auth/login/password`.

**Request**

```json
{ "project_id": "V7rdgcYkq9PHQZkwvoA-F" }
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
  "message": "pre-assessment questions retrieved successfully",
  "data": {
    "version": { "id": 1, "code": "A", "version": 1, "name": "Pre-Assessment AI Proficiency (Form A)", "duration_minutes": 30 },
    "attempt": null,
    "items": [
      {
        "id": 15,
        "item_code": "R1",
        "section": "scenario",
        "type": "single_choice",
        "question": "AI menulis \"Menurut riset McKinsey 2024, 73% perusahaan sudah memakai AI\", lengkap dengan link. ...",
        "options": [
          { "id": 61, "text": "Buka sumbernya dan pastikan angka, tahun, dan konteks surveinya memang sesuai sebelum dipakai" },
          { "id": 62, "text": "Minta AI mengutip kalimat persis dari sumber itu, lalu tampilkan kutipan tersebut di slide" }
        ]
      },
      { "id": 25, "item_code": "C1", "section": "practice", "type": "text", "question": "Pilih satu tugas rutin ...", "options": [] }
    ]
  }
}
```

`section` is `profile` (context, not scored), `scenario` (multiple choice, scored), `practice` (a written prompt, graded by an LLM in `score`), or `behaviour` (a frequency scale plus one written example). A `text` item has an empty `options` list and takes a free-text answer. Only `scenario` options are shuffled, per caller and per question, so a reload shows the same order. `Saya tidak tahu` on knowledge questions always stays last, and scale and profile options keep their natural order. Nothing that gives the key away is returned: no credit, domain, CFTE level, or critical-item marker. `attempt` is `null` until the caller submits. After that it's `{ "id", "status", "submitted_at" }`, where `status` is `submitted` until the first `score` call turns it into `scored`.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |
| 404 | `NOT_FOUND` | `No pre-assessment is set for this project` | the project has no `lms_pa_project_versions` row |

```json
{
  "success": false,
  "code": 404,
  "status": "NOT_FOUND",
  "message": "No pre-assessment is set for this project"
}
```

### `POST {base_url}/api/v1/pre-assessment/submit`

Submits every answer in one call and creates the caller's attempt with `status: "submitted"`, against the version assigned to the project. One-shot: a second call is rejected.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "answers": [
    { "item_id": 15, "option_id": 61 },
    { "item_id": 25, "text": "Untuk direksi: ringkas notulen rapat ini jadi 5 poin keputusan ..." }
  ]
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `project_id` | string | yes | |
| `answers` | array | yes | Exactly one entry per question of the project's version (the `items` from `questions`). |
| `answers[].item_id` | integer | yes | |
| `answers[].option_id` | integer | for `single_choice` | Must be one of that question's own options. |
| `answers[].text` | string | for `text` | Not blank, max 4000 chars, stored trimmed. |

**Response** — `201 Created`

```json
{
  "success": true,
  "code": 201,
  "status": "CREATED",
  "message": "pre-assessment answers submitted successfully",
  "data": { "attempt_id": 1, "status": "submitted" }
}
```

Each multiple-choice answer stores the chosen option's credit at submit time. Choosing an option flagged `shadow_ai` (a personal AI account, question A2) sets the attempt's `is_shadow_ai`. Nothing is graded or scored here. That happens in `score`.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | same three messages as `questions` | session checks |
| 400 | `BAD_REQUEST` | `answers: must not be empty` / `answers[0].itemId: must not be null` / `answers[0].text: size must be between 0 and 4000` | request field validation |
| 400 | `BAD_REQUEST` | `answers: wrong type or missing` | `answers` isn't a JSON array |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |
| 404 | `NOT_FOUND` | `No pre-assessment is set for this project` | the project has no `lms_pa_project_versions` row |
| 400 | `BAD_REQUEST` | `Pre-assessment already submitted.` | the caller already has an attempt |
| 400 | `BAD_REQUEST` | `Question 999 is not part of this pre-assessment` | an `item_id` belongs to another version or doesn't exist |
| 400 | `BAD_REQUEST` | `Question F1 is answered more than once` | the same `item_id` appears twice |
| 400 | `BAD_REQUEST` | `Question F1 needs one of its own options` | a `single_choice` answer has no `option_id`, has an option of another question, or also carries `text` |
| 400 | `BAD_REQUEST` | `Question C1 needs a text answer` | a `text` answer is blank or carries an `option_id` |
| 400 | `BAD_REQUEST` | `Every question must be answered, missing: A1, C1` | one or more questions have no answer |

```json
{
  "success": false,
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "Every question must be answered, missing: A1, C1"
}
```

### `POST {base_url}/api/v1/pre-assessment/score`

Returns the caller's CFTE result, computing it first if the attempt is still `submitted`.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{ "project_id": "V7rdgcYkq9PHQZkwvoA-F" }
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
  "message": "pre-assessment score retrieved successfully",
  "data": {
    "attempt_id": 1,
    "status": "scored",
    "submitted_at": "2026-10-12T04:25:34.344741Z",
    "scored_at": "2026-10-12T04:25:36.52717Z",
    "band": { "band": 5, "name": "Strategic Mastery", "level": 3 },
    "raw_band": { "band": 5, "name": "Strategic Mastery", "level": 3 },
    "target_band": { "band": 2, "name": "Basic Proficiency", "level": 1 },
    "gap": 0,
    "meets_target": true,
    "dimensions": { "knowledge": 100, "skills": 93.75, "behaviours": 87.5 },
    "level_mastery": { "p1": 100, "p2": 100, "p3": 75 },
    "gates": { "security_passed": true, "behaviour_passed": true },
    "flags": { "overclaim": false, "shadow_ai": false, "sensitive_data": false },
    "clusters": [
      { "code": "understand", "name": "Memahami AI", "score": 100 },
      { "code": "use", "name": "Memakai AI", "score": 88.46 }
    ],
    "domains": [
      { "code": "foundations", "name": "AI Foundations", "cluster_code": "understand", "score": 100 },
      { "code": "tools_methods", "name": "AI Tools and Methods", "cluster_code": "use", "score": 82.14 }
    ],
    "feedback": [
      { "item_code": "C1", "text": "Anda sudah menulis prompt yang sangat lengkap: tujuan terukur, sumber input jelas, ..." },
      { "item_code": "B6", "text": "Anda memberikan contoh spesifik dengan tool ChatGPT, ..." }
    ]
  }
}
```

`data` is `null` when the caller hasn't submitted yet. The first call after `submit` sends the written answers (C1 prompt, B6 example, with the A3 routine tasks as context) to DeepSeek in one request, which takes a few seconds. It then runs the band engine, stores everything, and flips the attempt to `scored`. Every later call only reads the stored result.

How the numbers come out:
- **`level_mastery`**: the percentage of credit on CFTE L1, L2, and L3 items (`p1`/`p2`/`p3`). Scenario credit is 1, 0.5, or 0. C1 counts as two virtual items: L1 passes at a rubric total of 6/12, L2 at 9/12.
- **`dimensions`**: `knowledge` and `skills` average their items × 100. `behaviours` is half the B1–B5 average and half B6.
- **`raw_band`**: the highest band whose `min_p1`/`min_p2`/`min_p3` in `lms_pa_bands` are all met.
- **`band`**: the raw band after two gates. A wrong critical item (DA1, R1, E1) or real sensitive data in C1 caps it at 1. From band 3 up, `behaviours` below 50, or B1/B2 below "Kadang", caps it at 2.
- **Overclaim**: B1 or B2 answered "Sering"/"Selalu" while their paired skill item (R1, DA1) is wrong drops that behaviour to 0.25 and sets `flags.overclaim`.
- **`target_band`**: one flat target for everyone (`lms_pa_scoring_configs.target_band`, currently 2). `gap` is how many bands short the caller is, never negative.
- **`clusters`**: item-weighted averages of their domains.

Every threshold comes from the active `lms_pa_scoring_configs` row and its `lms_pa_bands`, not from code. The attempt keeps the `config_id` it was scored with.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | same three messages as `questions` | session checks |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |
| 404 | `NOT_FOUND` | `No active pre-assessment scoring config` | no `lms_pa_scoring_configs` row is active (shouldn't happen) |
| 503 | `SERVICE_UNAVAILABLE` | `AI draft generation is not available right now` | `DEEPSEEK_API_KEY` isn't set (the message is shared with the other DeepSeek callers) |
| 502 | `BAD_GATEWAY` | `The AI service failed to generate a draft, please try again` | DeepSeek returned an error or timed out |
| 502 | `BAD_GATEWAY` | `The AI service returned an unusable grading` | the reply wasn't JSON, missed an item, or had a score outside its rubric |

A 502/503 leaves the attempt `submitted` with nothing written, so calling `score` again retries the grading.

```json
{
  "success": false,
  "code": 502,
  "status": "BAD_GATEWAY",
  "message": "The AI service returned an unusable grading"
}
```

## Known gaps

AI use-case recommendations are gone with the legacy endpoints and have not been rebuilt on `lms_pa_attempt_reports` yet. The champion and sponsor baselines (`champion/pre-assessment-team`, `sponsor/pre-assessment-organization`) read these same attempts through `PreAssessmentBaselineReader`. The legacy `lms_pre_assessments` table is no longer read or written by anything.

There is no autosave: every answer is sent at once. Changing a project's version after someone submitted leaves their attempt on the old version, which is intended. Two simultaneous `submit` calls for the same access both pass the duplicate check, and the loser fails on `UNIQUE (access_id)` with a generic 500 instead of the 400 above. Two simultaneous first `score` calls can both pay for a DeepSeek grading. Only one result is stored, because the attempt row is locked while scoring is written.
