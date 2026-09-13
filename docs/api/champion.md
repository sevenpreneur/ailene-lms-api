# Champion

Endpoints for a **champion** — the person who coaches one department's learners. Three groups: `/api/v1/champion/*` for the team roster, one member's profile, the team competency baseline and the periodic report; `/api/v1/champion/prompts/*` and `/api/v1/champion/use-cases/*` for assigning work and reviewing what comes back. Library browsing (`/api/v1/prompts`, `/api/v1/use-cases`), the category list (`/api/v1/categories`) and coaching notes (`/api/v1/coaching-notes/*`) already live in their own docs and are shared with other roles.

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
            "weakest_key": "agentic"
          }
        ]
      }
    ],
    "team_pillars": [{ "key": "agentic", "score": 1.6 }],
    "readiness": { "ready": 2, "developing": 3, "basic": 2 }
  }
}
```

Pillar scores reuse the same deterministic scoring as `POST /api/v1/pre-assessment/score`. Unlike the sponsor's org view, this one is **member-grained** — a champion coaches people, not departments — so `members` is sorted weakest-first and every aggregate (`team_avg`, `team_pillars`, `readiness`) is a mean over members rather than over departments. `departments` always holds exactly one entry, the champion's own group, so the array shape matches the sponsor endpoint. `team_pillars` is sorted ascending so the biggest gap reads first. Readiness tiers key off each member's average: `ready` ≥ 2.5, `developing` 1.5–2.5, `basic` < 1.5; `gap_large_count` counts members below 2.0. With no submitted pre-assessments you get the same shape with zeroes and an empty `departments`.

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

The prompt is created at **level 2** with `is_self_created = false` — that's what separates a champion-authored library item from a student's self-created practice. Omit `assignment` to add the prompt to the library without sending it to anyone.

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

Sorted unaccepted-first, then newest submission. `subject.text` is the prompt's scenario. `hours_with_ai` and `ai_tool` are always `null` here — they exist so the prompt and use case queues share one response shape.

**Errors** — same shape and cases as `members` above (minus the `group_id` assertion).

### `POST {base_url}/api/v1/champion/prompts/submission-details`

Returns one prompt submission in full, including the student's work and any rubric already recorded.

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
    "rubric_specificity": null,
    "rubric_context": null,
    "rubric_constraints": null,
    "rubric_examples": null,
    "rubric_iteration": null,
    "categories": [{ "id": 87, "name": "Human Capital" }]
  }
}
```

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
  "rubric_specificity": 4,
  "rubric_context": 4,
  "rubric_constraints": 3,
  "rubric_examples": 3,
  "rubric_iteration": 4
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `submission_id` | integer | yes |
| `is_accepted` | boolean | yes |
| `comment` | string, max 2000 | required when `is_accepted` is `false` |
| `rubric_specificity` … `rubric_iteration` | short, 1–5 | no |

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

These mirror the prompt endpoints exactly, with three differences: new use cases are created at **level 3**, there's no `expected_output` field, and the review has no rubric.

### `POST {base_url}/api/v1/champion/use-cases/assign`

Assigns an existing library use case to team members. Same request, response and errors as `prompts/assign`, except the not-found message is `Use case not found` and the success message is `use case assigned successfully`.

### `POST {base_url}/api/v1/champion/use-cases/create-assignment`

Creates a new library use case and optionally assigns it. Same as `prompts/create-assignment` minus `expected_output`; the response carries `use_case_id` instead of `prompt_id`, and the level error reads `Use case level (L3) not found`.

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

Accepts or returns a use case submission. Same as `prompts/review` without the five rubric fields; `xp_awarded` is the use case's `xp_reward`.
