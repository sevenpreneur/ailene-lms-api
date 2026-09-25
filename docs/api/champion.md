# Champion

Endpoints for a **champion** — the person who coaches one department's learners. Three groups: `/api/v1/champion/*` for the team roster, one member's profile, the team competency baseline and the periodic report; `/api/v1/champion/assignments/*` for AI-drafted library items; `/api/v1/champion/prompts/*` and `/api/v1/champion/use-cases/*` for assigning work and reviewing what comes back. Library browsing (`/api/v1/prompts`, `/api/v1/use-cases`), the category list (`/api/v1/categories`) and coaching notes (`/api/v1/coaching-notes/*`) already live in their own docs and are shared with other roles.

Every endpoint takes a `project_id` and requires the caller's `lms_accesses` row on that project to have `role = 'champion'` — anyone else gets `403`.

**One structural difference from the old app:** `lms_accesses` is unique on `(project_id, user_id)`, so a champion leads **exactly one group per project** (the `group_id` on their own access row) rather than owning a list of groups. A champion with no `group_id` gets `400` from every endpoint here. "Team" throughout means every access row in that group **except the champion's own**.

## Team

### `POST {base_url}/api/v1/champion/members`

Returns the champion's team roster with per-member progress, plus team-level scorecard stats.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "group_id": 1
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `group_id` | integer | no |

`group_id` is an optional assertion, not a filter: it must match the group the champion actually leads, otherwise the call is rejected with `403`. Omit it to just get your own group.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "team members retrieved successfully",
  "data": {
    "stats": {
      "total": 9,
      "on_track": 5,
      "at_risk": 2,
      "behind": 2,
      "active_this_week": 6,
      "submissions_sent": 21,
      "members_submitted": 7,
      "hours_saved": 88
    },
    "list": [
      {
        "access_id": "V7rdgcYkq9PHQZkwvoA-F",
        "user": {
          "id": "2f1c...",
          "full_name": "Rani Puspita",
          "email": "rani@example.com",
          "avatar": "https://..."
        },
        "current_level": { "id": 2, "level_number": 2, "name": "Operator Dasar & Lanjut" },
        "total_xp": 1240,
        "progress_percent": 62,
        "use_case_count": 4,
        "last_active_at": "2026-09-12T08:14:00Z",
        "status": "on_track"
      }
    ]
  }
}
```

`status` is `on_track` (active within 7 days), `at_risk` (7–14 days), or `behind` (over 14 days, or never active). `progress_percent` is completed materials + videos + quizzes over every active task in the project. `use_case_count` counts **accepted** use cases, while the team-level `submissions_sent` / `members_submitted` / `hours_saved` count everything submitted. The list is sorted by level descending, then XP, then name.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for this `project_id` |
| 403 | `FORBIDDEN` | `Only champions can access this resource` | the caller's access row isn't `role = 'champion'` |
| 400 | `BAD_REQUEST` | `You don't lead a group in this project yet` | the champion's access row has a null `group_id` |
| 403 | `FORBIDDEN` | `You can only view the group you lead` | `group_id` was sent and doesn't match the champion's group |

Example error response (`403 Forbidden`):

```json
{
  "success": false,
  "code": 403,
  "status": "FORBIDDEN",
  "message": "Only champions can access this resource"
}
```

### `POST {base_url}/api/v1/champion/member-details`

Returns one team member's full coaching profile: headline metrics, a competency radar, their level gate, recent activity and coaching notes.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "member_access_id": "V7rdgcYkq9PHQZkwvoA-F"
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `member_access_id` | string | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "member details retrieved successfully",
  "data": {
    "member": {
      "access_id": "V7rdgcYkq9PHQZkwvoA-F",
      "full_name": "Rani Puspita",
      "email": "rani@example.com",
      "avatar": "https://...",
      "job_title": "Finance Analyst",
      "group": { "id": 1, "name": "Human Capital" },
      "current_level": { "id": 2, "level_number": 2, "name": "Operator Dasar & Lanjut" },
      "joined_at": "2026-06-26T00:00:00Z",
      "last_active_at": "2026-09-12T08:14:00Z"
    },
    "metrics": { "gate_percent": 75, "streak_days": 4, "submission_total": 6, "avg_quiz": 82 },
    "radar": {
      "total_submissions": 6,
      "dimensions": [{ "key": "specificity", "label": "Specificity", "score": 4.1 }]
    },
    "gate": {
      "from_level": 2,
      "to_level": 3,
      "next_level_id": 3,
      "done": 6,
      "total": 8,
      "percent": 75,
      "ready": false,
      "requirements": [{ "label": "6 / 8 modul level 2 selesai", "completed": false }]
    },
    "activities": [
      {
        "id": "use-case-12",
        "type": "use_case",
        "title": "Use case: Rekap invoice bulanan",
        "subtitle": "Diterima",
        "status": "accepted",
        "occurred_at": "2026-09-10T04:00:00Z"
      }
    ],
    "notes": [
      { "id": 3, "text": "Fokus ke iterasi prompt minggu depan.", "created_at": "2026-09-09T02:00:00Z", "champion_name": "Budi Santoso" }
    ]
  }
}
```

The six radar dimensions are each scored 0–5 and derived from this member's own record: `specificity` from their average best quiz score, `context` and `verification` from prompt / use case acceptance rates, `iteration` and `workflow` from submission volume, and `tool` from how many distinct AI tools they've reported. `gate` describes progress out of the member's **current** level — `total` counts the active quizzes and materials at that level, and `percent` is 100 when the level has no gated content at all. `streak_days` counts consecutive UTC days with any completion, starting from today (or yesterday if nothing happened yet today). `activities` is capped at the 8 most recent events, `notes` at 20. Prompt and use case activity is limited to items **this** champion assigned; quizzes and notes are not.

**Errors**

Same cases as `members`, except that the group assertion doesn't apply and instead:

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `memberAccessId: must not be blank` | missing/empty `member_access_id` |
| 403 | `FORBIDDEN` | `You can only access members in the group you lead` | the access id isn't on the champion's team (also covers ids that don't exist) |

### `POST {base_url}/api/v1/champion/pre-assessment-team`

Returns the team's competency baseline from the pre-assessment, per member and aggregated.

**Authorization:** `Bearer <jwt>`.

**Request** — `{ "project_id": "..." }`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "team pre-assessment retrieved successfully",
  "data": {
    "department_count": 1,
    "total_members": 9,
    "completed_count": 7,
    "measured_at": "2026-07-02T09:15:00Z",
    "target": 3.2,
    "team_avg": 2.1,
    "ready_count": 2,
    "gap_large_count": 3,
    "departments": [
      {
        "id": 1,
        "name": "Human Capital",
        "member_count": 9,
        "completed_count": 7,
        "completion_percent": 78,
        "avg": 2.1,
        "pillars": [{ "key": "ai_foundation", "score": 2.4 }],
        "members": [
          {
            "access_id": "V7rdgcYkq9PHQZkwvoA-F",
            "name": "Sari Dewi",
            "avatar": null,
            "avg": 1.4,
            "pillars": [{ "key": "ai_foundation", "score": 1.6 }],
            "weakest_key": "agentic",
            "is_me": false
          }
        ]
      }
    ],
    "team_pillars": [{ "key": "agentic", "score": 1.6 }],
    "readiness": { "ready": 2, "developing": 3, "basic": 2 }
  }
}
```

Pillar scores reuse the same deterministic scoring as `POST /api/v1/pre-assessment/score`. Unlike the sponsor's org view, this one is **member-grained** — a champion coaches people, not departments — so `members` is sorted weakest-first and every aggregate (`team_avg`, `team_pillars`, `readiness`) is a mean over members rather than over departments. `departments` always holds exactly one entry, the champion's own group, so the array shape matches the sponsor endpoint. Unlike the rest of this doc, where "team" excludes the caller, the baseline covers **everyone in the group except sponsors**: the caller and any other champions count toward `total_members`/`member_count` (the `completion_percent` denominator) and appear in `members` once they have submitted. `is_me` marks the caller's own row. Sponsors are left out of both the count and the list, because they are org-wide observers who only sit in a group because `group_id` is required. `team_pillars` is sorted ascending so the biggest gap reads first. Readiness tiers key off each member's average: `ready` ≥ 2.5, `developing` 1.5–2.5, `basic` < 1.5; `gap_large_count` counts members below 2.0. With no submitted pre-assessments you get the same shape with zeroes and an empty `departments`.

**Errors** — same shape and cases as `members` above (minus the `group_id` assertion).

### `POST {base_url}/api/v1/champion/report`

Returns an auto-generated weekly or monthly report draft for the champion's team, ready to send to the sponsor.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "period": "weekly"
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `period` | `weekly` \| `monthly` | no (default `weekly`) |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "champion report retrieved successfully",
  "data": {
    "period": "weekly",
    "generated_at": "2026-09-14T02:00:00Z",
    "report": {
      "title": "Laporan Mingguan - 13 Sep - 14 Sep 2026",
      "team_name": "Human Capital",
      "champion_name": "Champion",
      "status": "draft_auto_generated"
    },
    "recipient": { "access_id": "V7rd...", "full_name": "Dewi Anggraini", "avatar": null, "job_title": "COO" },
    "metrics": {
      "active_members": 6,
      "total_members": 9,
      "active_percent": 67,
      "accepted_submissions": 5,
      "hours_saved": 18.5,
      "level_ups": 2
    },
    "level_movements": [{ "from": "L1", "to": "L2", "count": 2, "note": "Rani Puspita, Sari Dewi" }],
    "narrative": "Momentum tim Human Capital minggu ini berjalan positif. ...",
    "sent_reports": [
      { "id": "weekly-1", "title": "Laporan Mingguan - 06 Sep 2026", "sent_at": "2026-09-06T00:00:00Z", "recipient": "Dewi Anggraini" }
    ],
    "previous_period_start": "2026-09-06T00:00:00Z"
  }
}
```

Everything is computed against the current period — the week (starting Sunday, UTC) or calendar month containing today. `level_movements` reads `lms_level_history` for level changes reached inside the period, and `note` lists up to three names plus a `+N` overflow. `recipient` is the project's sponsor, or `null` when the project has none. `sent_reports` is a **synthetic** back-catalogue of the previous three periods for the UI's history list — the app has no report archive table yet, so these are generated labels, not real sent records. `status` is always `draft_auto_generated` for the same reason. `champion_name` is currently a fixed placeholder rather than the caller's own name.

**Errors** — same shape and cases as `members` above (minus the `group_id` assertion).

## AI assignment drafts

A champion can ask DeepSeek for several draft library items at once, look through them, and turn one into a real prompt or use case. The drafts from one `generate` call form a **batch** that shares a `batch_id`. Every draft is saved to `lms_assignment_drafts` and is private to the champion who generated it: drafts are not library rows, and students never see them. A draft only becomes a library item through `prompts/create-assignment` or `use-cases/create-assignment`, with its `draft_id`, after the champion has edited it.

### `POST {base_url}/api/v1/champion/assignments/generate`

Turns one instruction into 1–5 distinct draft variants, with the model choosing both the number and each variant's kind, and saves them as a new batch.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "instruction": "bikin latihan prompt untuk tim HR menulis job description"
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `instruction` | string, max 2000 | yes |
| `kind` | `PROMPT` \| `USE_CASE` | no |
| `count` | integer, 1–5 | no |

Send only `project_id` and `instruction` to let the model decide everything. Before drafting, it writes a short plan: what the instruction actually asks for, which kind fits each variant, how many variants are genuinely worth offering, and what sets each one apart. `PROMPT` (level 2) is practice writing one prompt from a scenario. `USE_CASE` (level 3) is applying AI to a real recurring workflow. One batch may mix the two. A narrow instruction usually yields 1–2 variants and a broad one up to 5, and the server caps the batch at 5. The plan is only logged and is never returned. `kind` and `count` are optional overrides: `kind` forces every variant to that kind, and `count` asks for exactly that many. The model also gets the champion's group name as context.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "assignment drafts generated successfully",
  "data": {
    "batch_id": "46nvsuVvbe7fODMoEO0Pd",
    "instruction": "bikin latihan prompt untuk tim HR menulis job description",
    "requested_kind": null,
    "created_at": "2026-09-25T03:25:03.500731Z",
    "drafts": [
      {
        "id": 12,
        "kind": "PROMPT",
        "angle": "Untuk pemula",
        "name": "Prompt Job Description Posisi Entry-Level",
        "description": "Anda adalah HR Generalist di perusahaan ritel ...",
        "expected_output": "Job description lengkap untuk posisi Kasir ...",
        "category_ids": [6, 87],
        "used_at": null,
        "used_prompt_id": null,
        "used_use_case_id": null
      }
    ]
  }
}
```

`drafts` is in generation order and holds 1–5 entries, or at most `count` when you send it. When a variant comes back without a `name`, a `description`, or an `expected_output` on a `PROMPT`, the server drops it rather than failing the whole batch, so a batch can occasionally be shorter than planned. It is never empty. `angle` is a short label for what sets the variant apart (for example `Untuk pemula` or `Tantangan lanjutan`). When the model leaves it out, it falls back to `Varian N`. `description` is the scenario for a `PROMPT` and the task description for a `USE_CASE`. `expected_output` is always a string for `PROMPT` and always `null` for `USE_CASE`. `category_ids` holds up to 2 ids that all exist in `lms_categories`. Ids the model made up are dropped, so it can be `[]`, and the champion then picks categories by hand. `requested_kind` echoes the request's `kind`. `used_at`, `used_prompt_id` and `used_use_case_id` start out `null` (see `draft_id` on `create-assignment`). Generation takes about 4–5 seconds for 2–4 variants, and the server times out after 90 seconds.

**Errors**

Same auth/role/group cases as `members`, plus:

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `instruction: must not be blank` | `instruction` missing or blank (over 2000 characters reads `size must be between 0 and 2000`) |
| 400 | `BAD_REQUEST` | `count: must be less than or equal to 5` | `count` above 5 (below 1 reads `must be greater than or equal to 1`) |
| 400 | `BAD_REQUEST` | `kind: 'X' is not one of [PROMPT, USE_CASE]` | `kind` isn't one of the two values |
| 503 | `SERVICE_UNAVAILABLE` | `AI draft generation is not available right now` | `DEEPSEEK_API_KEY` isn't set on the server |
| 502 | `BAD_GATEWAY` | `The AI service failed to generate a draft, please try again` | DeepSeek returned an error, or the call failed or timed out |
| 502 | `BAD_GATEWAY` | `The AI service returned an empty draft, please try again` | DeepSeek answered with no content |
| 502 | `BAD_GATEWAY` | `The AI service returned an unusable draft, please try again` | the content wasn't JSON, or no variant was usable |

Nothing is saved when any of these errors occurs.

```json
{
  "success": false,
  "code": 502,
  "status": "BAD_GATEWAY",
  "message": "The AI service returned an unusable draft, please try again",
  "data": null
}
```

### `POST {base_url}/api/v1/champion/assignments/drafts`

Lists the caller's own draft batches, newest first.

**Authorization:** `Bearer <jwt>`.

**Request** — `{ "project_id": "..." }`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "assignment drafts retrieved successfully",
  "data": {
    "batches": [
      {
        "batch_id": "46nvsuVvbe7fODMoEO0Pd",
        "instruction": "bikin latihan prompt untuk tim HR menulis job description",
        "requested_kind": null,
        "created_at": "2026-09-25T03:25:03.500731Z",
        "drafts": [ { "id": 12, "kind": "PROMPT", "angle": "Untuk pemula", "...": "same draft shape as generate" } ]
      }
    ]
  }
}
```

Each batch has the same shape as the `generate` response. The response holds only the 30 most recent batches. Drafts inside a batch are in generation order. A batch whose drafts were all deleted no longer appears. Only the caller's drafts are returned, never another champion's, even in the same group.

**Errors** — same auth/role/group cases as `members`.

### `POST {base_url}/api/v1/champion/assignments/drafts/delete`

Deletes one of the caller's own drafts.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{ "project_id": "V7rdgcYkq9PHQZkwvoA-F", "draft_id": 12 }
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `draft_id` | integer | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "assignment draft deleted successfully",
  "data": { "deleted": true }
}
```

Deleting a draft that was already turned into a library item leaves that prompt or use case untouched.

**Errors**

Same auth/role/group cases as `members`, plus:

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `draftId: must not be null` | `draft_id` missing |
| 404 | `NOT_FOUND` | `Draft not found` | no such draft, or it belongs to someone else |

## Prompt assignments and review

### `POST {base_url}/api/v1/champion/prompts/assign`

Assigns an existing library prompt to team members, creating one submission row each.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "library_id": 1,
  "target_type": "MEMBER",
  "target_access_ids": ["V7rdgcYkq9PHQZkwvoA-F"],
  "deadline": "2026-09-30T23:59:59Z",
  "message": "Fokus ke struktur output."
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `library_id` | integer | yes |
| `target_type` | `MEMBER` \| `GROUP` | yes |
| `target_access_ids` | string[] | when `target_type` is `MEMBER` |
| `target_group_ids` | integer[] | when `target_type` is `GROUP` |
| `deadline` | ISO-8601 timestamp, must be future | yes |
| `message` | string, max 500 | no |

`MEMBER` targets specific access ids, all of which must be on the champion's team. `GROUP` expands to the champion's whole team — and since a champion leads only one group, any id in `target_group_ids` other than their own is rejected. Either way the champion is never assigned to themselves.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "prompt assigned successfully",
  "data": { "assigned_count": 5, "target_total": 6, "skipped": 1 }
}
```

Assignment is idempotent per `(student_access_id, prompt_id)`: a member who already has that prompt is counted in `skipped` rather than duplicated or erroring, so re-sending the same assignment is safe.

**Errors**

Same auth/role/group cases as `members`, plus:

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `Deadline must be in the future` | `deadline` is now or in the past |
| 404 | `NOT_FOUND` | `Prompt not found` | no active `lms_prompts` row with that `library_id` |
| 400 | `BAD_REQUEST` | `target_access_ids is required when target_type is MEMBER` | `MEMBER` with no access ids |
| 400 | `BAD_REQUEST` | `target_group_ids is required when target_type is GROUP` | `GROUP` with no group ids |
| 403 | `FORBIDDEN` | `Some members are not in the group you lead` | an access id isn't on the champion's team |
| 403 | `FORBIDDEN` | `Some groups are not yours` | a group id isn't the champion's group |
| 400 | `BAD_REQUEST` | `No target members` | the resolved target set is empty (an empty team, for instance) |

### `POST {base_url}/api/v1/champion/prompts/create-assignment`

Creates a brand-new library prompt and, optionally, assigns it in the same call.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "name": "Draft Job Description",
  "description": "Anda adalah HR Generalist ...",
  "expected_output": "Job description lengkap ...",
  "category_ids": [87],
  "assignment": {
    "target_type": "GROUP",
    "target_group_ids": [1],
    "deadline": "2026-09-30T23:59:59Z",
    "message": null
  }
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `name` | string, max 255 | yes |
| `description` | string | yes |
| `expected_output` | string | yes |
| `category_ids` | short[], 1–2 entries | yes |
| `assignment` | object (same target/deadline/message fields as `assign`) | no |
| `draft_id` | integer, one of the caller's `assignments/generate` drafts | no |

When `draft_id` is sent, the draft is stamped once the prompt is inserted, in the same transaction: `used_at` becomes now and `used_prompt_id` the new prompt's id. The draft's content is not re-read, so the request fields are what gets saved, edits included. A draft can be used more than once, and each use points `used_*` at the newest prompt. The prompt is created at **level 2** with `is_self_created = false` — that's what separates a champion-authored library item from a student's self-created practice. Omit `assignment` to add the prompt to the library without sending it to anyone.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "prompt assignment created successfully",
  "data": { "prompt_id": 12, "assigned_count": 6, "target_total": 6, "skipped": 0 }
}
```

**Errors**

Same cases as `prompts/assign`, except `Prompt not found` is replaced by:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Some categories were not found` | a `category_ids` entry has no `lms_categories` row |
| 404 | `NOT_FOUND` | `Prompt level (L2) not found` | the project has no level with `level_number = 2` |
| 404 | `NOT_FOUND` | `Draft not found` | `draft_id` is not one of the caller's drafts |
| 400 | `BAD_REQUEST` | `Draft is not a prompt draft` | `draft_id` names a `USE_CASE` draft |

### `POST {base_url}/api/v1/champion/prompts/submissions`

Returns the champion's prompt review queue — everything they've assigned, unreviewed first.

**Authorization:** `Bearer <jwt>`.

**Request** — `{ "project_id": "..." }`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "prompt submissions retrieved successfully",
  "data": {
    "list": [
      {
        "id": 6,
        "subject": {
          "id": 1,
          "name": "Draft Job Description",
          "text": "Anda adalah HR Generalist ...",
          "level": { "id": 2, "level_number": 2, "name": "Operator Dasar & Lanjut" }
        },
        "member": { "access_id": "V7rd...", "full_name": "Akmal Luthfiansyah", "avatar": "https://..." },
        "deadline": null,
        "submitted_at": null,
        "reviewed_at": null,
        "is_accepted": false,
        "hours_with_ai": null,
        "ai_tool": null,
        "categories": [{ "id": 87, "name": "Human Capital" }]
      }
    ]
  }
}
```

Sorted unaccepted-first, then newest submission. `subject.text` is the prompt's scenario. `hours_with_ai` and `ai_tool` are always `null` here — they exist so the prompt and use case queues share one response shape. Each item also carries `evaluation`, the same object as in `submission-details` (so the list can show AI status and `average`), `null` until the student submits. It is always `null` in the use case queue.

**Errors** — same shape and cases as `members` above (minus the `group_id` assertion).

### `POST {base_url}/api/v1/champion/prompts/submission-details`

Returns one prompt submission in full, including the student's work and the AI's rubric evaluation of it.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "submission_id": 6
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `submission_id` | integer | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "prompt submission details retrieved successfully",
  "data": {
    "id": 6,
    "prompt": {
      "id": 1,
      "name": "Draft Job Description",
      "scenario": "Anda adalah HR Generalist ...",
      "expected_output": "Job description lengkap ...",
      "level": { "id": 2, "level_number": 2, "name": "Operator Dasar & Lanjut" }
    },
    "member": { "access_id": "V7rd...", "full_name": "Akmal Luthfiansyah", "email": "akmal@example.com", "avatar": "https://..." },
    "reviewed_by": null,
    "deadline": "2026-09-30T23:59:59Z",
    "message": "Fokus ke struktur output.",
    "input": "Buatkan job description ...",
    "output": "## Marketing Manager ...",
    "submitted_at": "2026-09-12T03:00:00Z",
    "reviewed_at": null,
    "comment": null,
    "is_accepted": false,
    "evaluation": {
      "ai_status": "completed",
      "ai_feedback": "Anda sudah menjelaskan peran dan tugas dengan jelas. Tambahkan format output yang diinginkan ...",
      "ai_evaluated_at": "2026-09-12T03:00:04Z",
      "specificity": 4,
      "context": 5,
      "constraints": 2,
      "examples": 1,
      "iteration": 3,
      "average": 3.0
    },
    "categories": [{ "id": 87, "name": "Human Capital" }]
  }
}
```

`evaluation` is the rubric, and it is `null` until the student has submitted. The five scores (1–5) live in one set of columns that DeepSeek fills first and the champion may then overwrite through `review`. Every submit or resubmit clears them and sets `ai_status` to `pending`, and an async job then grades the prompt the student wrote against the scenario and target output, usually within a few seconds. A champion override replaces the AI's number for that dimension, so the response carries only the current score, not the AI's original one. If the champion scores a dimension before the AI finishes, the AI leaves it alone.

`ai_status` is `pending`, `completed`, or `failed` (DeepSeek couldn't produce a usable evaluation even after retries). It is `null` for submissions made before AI grading existed, whose scores, if any, were entered by hand. `ai_feedback` is the model's 2–4 sentence feedback addressed to the student. `average` is the mean of the five scores, rounded to 1 decimal, and `null` unless all five are present. The scores decide nothing by themselves: acceptance is still the champion's `review`.

A submission the champion didn't assign comes back as `404`, not `403` — the queue is scoped to them, so from their side it simply doesn't exist.

**Errors**

Same auth/role/group cases as `members`, plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Submission not found` | no such submission, or it wasn't assigned by this champion |

### `POST {base_url}/api/v1/champion/prompts/review`

Accepts or returns a prompt submission for revision, awarding XP once on acceptance.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "submission_id": 6,
  "is_accepted": true,
  "comment": "Struktur sudah rapi.",
  "rubric_context": 5
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `submission_id` | integer | yes |
| `is_accepted` | boolean | yes |
| `comment` | string, max 2000 | required when `is_accepted` is `false` |
| `rubric_specificity` … `rubric_iteration` | short, 1–5 | no |

DeepSeek scores the rubric when the student submits (see `submission-details`), so the champion doesn't have to. A `rubric_*` field sent here overwrites that one dimension's score. A field left out (or `null`) keeps the current score, whether the AI's or an earlier override.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "review saved successfully",
  "data": { "xp_awarded": 70 }
}
```

`xp_awarded` is the prompt's `xp_reward` when accepted and `0` otherwise. The XP write is idempotent on `(student_access_id, learning_type, learning_id)`, so a member never earns twice for the same prompt — but note the response still reports the reward value even in the rare case the row already existed.

**Errors**

Same auth/role/group cases as `members`, plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Submission not found` | no such submission, or it wasn't assigned by this champion |
| 400 | `BAD_REQUEST` | `Student has not submitted yet` | `submitted_at` is still null |
| 400 | `BAD_REQUEST` | `Submission already accepted` | the submission was already accepted; acceptance is final |
| 400 | `BAD_REQUEST` | `Comment required when sending back for revision` | `is_accepted` is `false` with a blank `comment` |

## Use case assignments and review

These mirror the prompt endpoints exactly, with three differences: new use cases are created at **level 3**, there's no `expected_output` field, and there is no rubric or AI evaluation.

### `POST {base_url}/api/v1/champion/use-cases/assign`

Assigns an existing library use case to team members. Same request, response and errors as `prompts/assign`, except the not-found message is `Use case not found` and the success message is `use case assigned successfully`.

### `POST {base_url}/api/v1/champion/use-cases/create-assignment`

Creates a new library use case and optionally assigns it. Same as `prompts/create-assignment` minus `expected_output`. The response carries `use_case_id` instead of `prompt_id`, and the level error reads `Use case level (L3) not found`. `draft_id` works the same way, except that it stamps `used_use_case_id` and requires a `USE_CASE` draft (`Draft is not a use case draft` otherwise).

### `POST {base_url}/api/v1/champion/use-cases/submissions`

The use case review queue. Same shape as `prompts/submissions`, but `subject.text` is the use case description and `hours_with_ai` / `ai_tool` carry real values once a student has submitted.

### `POST {base_url}/api/v1/champion/use-cases/submission-details`

One use case submission in full.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "use case submission details retrieved successfully",
  "data": {
    "id": 12,
    "use_case": {
      "id": 7,
      "name": "Rekap invoice bulanan",
      "description": "Merangkum invoice ...",
      "level": { "id": 3, "level_number": 3, "name": "Capstone" }
    },
    "member": { "access_id": "V7rd...", "full_name": "Rani Puspita", "email": "rani@example.com", "avatar": null },
    "reviewed_by": { "access_id": "V7rd...", "full_name": "Budi Santoso", "avatar": null },
    "deadline": "2026-09-30T23:59:59Z",
    "message": null,
    "outcome_proof": "https://drive.example.com/...",
    "hours_with_ai": 1.5,
    "hours_without_ai": 6.0,
    "description": "Saya pakai AI untuk ...",
    "ai_tool": "ChatGPT, Claude",
    "frequency": "monthly",
    "type": "workflow_automation",
    "submitted_at": "2026-09-10T02:00:00Z",
    "reviewed_at": "2026-09-10T06:00:00Z",
    "comment": "Bagus, lanjutkan.",
    "is_accepted": true,
    "categories": [{ "id": 87, "name": "Human Capital" }]
  }
}
```

`frequency` and `type` come back as the raw Postgres enum labels (`monthly`, `workflow_automation`). Hours saved isn't precomputed here — it's `hours_without_ai - hours_with_ai`, and the aggregate endpoints only count it when both are present and the difference is positive.

**Errors** — same as `prompts/submission-details`.

### `POST {base_url}/api/v1/champion/use-cases/review`

Accepts or returns a use case submission. Same request as `prompts/review`; `xp_awarded` is the use case's `xp_reward`.
