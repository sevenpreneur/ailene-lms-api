# Sponsor

Read-only dashboards for a project's **sponsor** — the executive view of the whole cohort rather than one learner's own standing (that's `docs/api/student.md`). Three groups of endpoints: `/api/v1/sponsor/*` for the organization dashboard, `/api/v1/sponsor/groups/*` for one department's detail page, and `/api/v1/sponsor/outcome/*` for the end-of-program outcome report. Every endpoint takes a `project_id` and is gated on the caller holding the `sponsor` role on that project — a `champion` or `student` access row gets `403`, not a filtered result.

Two conventions run through all of them: money figures use a fixed **Rp 250.000 per hour saved**, and "hours saved" for one use case submission is `hours_without_ai - hours_with_ai`, counted only when both are present and the difference is positive. Week and month boundaries are computed in **UTC**, and weeks start on Sunday.

## Organization dashboard

### `POST {base_url}/api/v1/sponsor/organization-stats`

Returns the headline counts for the project: how many people hold access and how many departments exist.

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

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "organization stats retrieved successfully",
  "data": {
    "member_count": 48,
    "group_count": 6
  }
}
```

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for this `project_id` |
| 403 | `FORBIDDEN` | `Only sponsors can access this resource` | the caller's access row on this project isn't `role = 'sponsor'` |

Example error response (`403 Forbidden`):

```json
{
  "success": false,
  "code": 403,
  "status": "FORBIDDEN",
  "message": "Only sponsors can access this resource"
}
```

### `POST {base_url}/api/v1/sponsor/executive-view`

Returns the six board-level metrics: average level, headcount, cumulative hours saved, rupiah ROI to date, and weekly active staff.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "executive view retrieved successfully",
  "data": {
    "metrics": {
      "avg_level": 1.8,
      "member_count": 48,
      "hours_saved_total": 312.5,
      "roi_cohort_to_date": 78125000,
      "staff_active_weekly_count": 31,
      "staff_active_weekly_percent": 65
    }
  }
}
```

`staff_active_weekly_*` counts people whose `lms_users.last_active_at` falls inside the last 7 days. `roi_cohort_to_date` is `hours_saved_total × 250000`, rounded to whole rupiah.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/headline`

Returns the sponsor hero KPIs — the share of staff who have produced a validated time saving, last week's hours saved, the annualized ROI those hours imply, and a 12-week sparkline.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "headline retrieved successfully",
  "data": {
    "productive_percent": 62,
    "productive_count": 29,
    "member_count": 47,
    "hours_saved_last_week": 24.5,
    "roi_annualized": 318500000,
    "trend": [
      { "label": "6 Jul", "hours": 8.5 },
      { "label": "13 Jul", "hours": 12.0 }
    ]
  }
}
```

Only **accepted** submissions count here, and sponsors themselves are excluded from `member_count`. `roi_annualized` extrapolates last week's saving across 52 weeks. `trend` always has 12 entries, oldest first, one per week bucket starting on a Sunday.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/program-health`

Returns four program-health percentages, each with an "X dari Y" caption ready to render.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "program health retrieved successfully",
  "data": {
    "metrics": [
      { "key": "pass_l1", "label": "Lulus L1", "name": "Capai Level 1+", "percent": 79, "detail": "38 dari 48 staff" },
      { "key": "pass_l2", "label": "Lulus L2", "name": "Capai Level 2+", "percent": 52, "detail": "25 dari 48 staff" },
      { "key": "accepted", "label": "Submission diterima", "name": "Hasil kerja di-ACC", "percent": 71, "detail": "56 dari 79 direview" },
      { "key": "participation", "label": "Partisipasi", "name": "Staff pernah submit", "percent": 83, "detail": "40 dari 48 staff" }
    ]
  }
}
```

`accepted` and `participation` pool prompt and use case submissions together. There are no targets or deltas yet — no baseline exists to compare against.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/recent-activity`

Returns the six newest events across the project — submissions, reviews, acceptances, and completed pre-assessments — merged into one feed.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "recent activity retrieved successfully",
  "data": {
    "activity": [
      {
        "type": "accepted",
        "actor": "Rani Puspita",
        "action": "terima use case",
        "meta": "Finance · Rekap invoice bulanan",
        "time": "2j",
        "at": "2026-09-14T03:12:44Z"
      }
    ]
  }
}
```

`type` is one of `submission`, `accepted`, `review`, `assessment`. `time` is a short Indonesian relative label (`baru saja`, `12 mnt`, `2j`, `3h`, then a `14 Sep`-style date past a week); `at` is the raw timestamp if the client would rather format it itself. `meta` joins the department and the subject with `·`, skipping either when absent.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/weekly-trends`

Returns 12 weekly buckets of hours saved and adoption rate, oldest first.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "weekly trends retrieved successfully",
  "data": {
    "weeks": [
      { "label": "22 Jun", "hours_saved": 6.5, "adoption_percent": 12, "highlight": false },
      { "label": "6 Sep", "hours_saved": 31.0, "adoption_percent": 44, "highlight": true }
    ]
  }
}
```

`adoption_percent` is the share of all project members who submitted at least one use case that week. `highlight` marks the current (last) week so the chart can emphasize it.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/proficiency-trends`

Returns the org-average level and average cumulative XP at the end of each program week.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "proficiency trends retrieved successfully",
  "data": {
    "weeks": [
      { "label": "M1", "avg_level": 0.12, "avg_xp": 45, "highlight": false },
      { "label": "M9", "avg_level": 1.84, "avg_xp": 1230, "highlight": true }
    ]
  }
}
```

Each week's figures are computed "as of" that week's end: a member counts only once their `lms_accesses` row existed, their level is the highest `lms_level_history` entry reached by the cutoff (falling back to their current level when they have no history rows at all), and XP is the sum of `lms_xp_earnings` earned by the cutoff. Sponsors are excluded. The program window is currently the fixed cohort range **2026-06-26 → 2026-08-26** — there is no per-project program window column yet, so this is the one endpoint that isn't fully project-scoped in its time axis.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/level-distribution`

Returns how the cohort is spread across levels 0–3, both org-wide and broken down per department, plus the departments that need intervention.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "level distribution retrieved successfully",
  "data": {
    "total": 48,
    "active_weekly": 31,
    "participation_percent": 65,
    "levels": [
      { "id": 1, "code": "L0", "label": "Level 0", "name": "AI Starter", "count": 6, "percent": 13 }
    ],
    "groups": [
      {
        "id": 3,
        "name": "Finance",
        "total": 9,
        "active_weekly": 6,
        "entry_level_count": 4,
        "entry_level_percent": 44,
        "levels": [
          { "level_id": 1, "code": "L0", "label": "Level 0", "name": "AI Starter", "count": 2, "percent": 22 }
        ]
      }
    ],
    "groups_needing_intervention": []
  }
}
```

Levels 0 through 3 are always rendered even when the project has no `lms_levels` row for one of them — a missing level gets a negative placeholder `id` and a `"Level N"` name, so the chart keeps four consistent bars. `entry_level_*` counts members still at level 0 or 1, and a department lands in `groups_needing_intervention` once that share reaches 35%. `groups` is sorted by headcount descending, then name.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/workforce-members`

Returns every non-sponsor member of the project with their level, curriculum progress, weekly hours saved, adoption segment, and a rendered status line.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "workforce members retrieved successfully",
  "data": {
    "total": 47,
    "departments": [{ "id": 3, "name": "Finance" }],
    "list": [
      {
        "access_id": "V7rdgcYkq9PHQZkwvoA-F",
        "user": {
          "id": "2f1c...",
          "full_name": "Rani Puspita",
          "email": "rani@example.com",
          "avatar": "https://..."
        },
        "department": { "id": 3, "name": "Finance" },
        "job_title": "Finance Analyst",
        "current_level": { "id": 3, "level_number": 2, "name": "AI Operator" },
        "score": 2.7,
        "progress_percent": 70,
        "segment": "Netral",
        "hours_saved_weekly": 4.5,
        "status": { "kind": "up", "label": "Naik L2 (3 hari lalu)" }
      }
    ]
  }
}
```

`score` blends level and curriculum progress as `min(level_number + progress_percent/100, 4.9)`, and the list is sorted by it descending, then by name. `progress_percent` is completed materials + videos + quizzes over every active task in the project. `segment` is `Promotor` at level 3+, `Netral` at level 2, `Resistor` below. `status.kind` is one of `champion`, `up` (levelled up within 14 days), `pass` (level 3+), `idle` (never active), or `stable`. `departments` lists only the departments actually represented in `list`, sorted by name.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/organization-leaderboard`

Returns the "Kinerja per Departemen" table: headcount, average level, most-submitted use case, submission volume, hours saved, and the week-over-week trend.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "organization leaderboard retrieved successfully",
  "data": {
    "max_score": 3,
    "list": [
      {
        "rank": 1,
        "id": 3,
        "name": "Finance",
        "member_count": 9,
        "avg_score": 2.3,
        "top_use_case": "Rekap invoice bulanan",
        "submission_count": 21,
        "hours": 88.5,
        "trend_percent": 18
      }
    ]
  }
}
```

`max_score` is the project's highest active level number, floored at 3, so the frontend can render `avg_score` against a consistent scale. `trend_percent` compares hours saved in the last 7 days against the 7 days before that, and is `null` when the earlier week had none (no meaningful base to grow from). `submission_count` pools prompts and use cases.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/pre-assessment-organization`

Returns the organization's competency baseline from the pre-assessment: six pillar scores averaged per department, then across departments, plus readiness tiers.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "organization pre-assessment retrieved successfully",
  "data": {
    "department_count": 5,
    "total_members": 44,
    "completed_count": 31,
    "measured_at": "2026-07-02T09:15:00Z",
    "target": 3.2,
    "org_avg": 2.1,
    "ready_count": 2,
    "gap_large_count": 1,
    "departments": [
      {
        "id": 3,
        "name": "Finance",
        "member_count": 9,
        "completed_count": 7,
        "completion_percent": 78,
        "pillars": [{ "key": "ai_foundation", "score": 2.4 }],
        "avg": 2.1
      }
    ],
    "org_pillars": [{ "key": "agentic", "score": 1.6 }],
    "readiness": { "ready": 2, "developing": 2, "basic": 1 }
  }
}
```

Pillar scores reuse the same deterministic scoring as `POST /api/v1/pre-assessment/score` (`PreAssessmentReportBuilder`), so a department's numbers are the plain mean of its members' pillar scores. Only departments with at least one submitted pre-assessment appear, sorted by `avg` ascending (weakest first), and `org_pillars` is sorted the same way so the biggest gap reads first. `target` is the 3.2 maturity line from the member-facing report. Readiness tiers key off each department's `avg`: `ready` ≥ 2.5, `developing` 1.5–2.5, `basic` < 1.5; `gap_large_count` counts departments below 2.0.

**Errors** — same shape and cases as `organization-stats` above.

## Department detail

### `POST {base_url}/api/v1/sponsor/groups/departments`

Returns every department in the project with its headcount — the picker that drives the rest of this section.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "departments retrieved successfully",
  "data": {
    "departments": [
      { "id": 3, "name": "Finance", "member_count": 9 }
    ]
  }
}
```

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/groups/overview`

Returns one department's champion and its ten headline metrics.

**Authorization:** `Bearer <jwt>`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "group_id": 3
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `group_id` | integer (positive) | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "group overview retrieved successfully",
  "data": {
    "group": {
      "id": 3,
      "name": "Finance",
      "champion": {
        "access_id": "V7rdgcYkq9PHQZkwvoA-F",
        "full_name": "Budi Santoso",
        "avatar": "https://...",
        "job_title": "Finance Manager"
      }
    },
    "metrics": {
      "total_members": 9,
      "active_members": 6,
      "active_percent": 67,
      "avg_level": 2.1,
      "beginner_count": 4,
      "beginner_percent": 44,
      "hours_saved_total": 88.5,
      "accepted_use_cases": 14,
      "accepted_use_cases_this_month": 3,
      "needs_intervention": true
    }
  }
}
```

`champion` is the earliest-created `champion` access row in that department, and is `null` when the department has none. `needs_intervention` fires when at least 35% of the department is still at level 0 or 1. `accepted_use_cases_this_month` counts against the current calendar month in UTC.

**Errors**

Same cases as `organization-stats`, plus:

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `groupId: must not be null` | missing `group_id` field |
| 404 | `NOT_FOUND` | `Group not found in this project` | `group_id` doesn't exist, or belongs to a different project |

### `POST {base_url}/api/v1/sponsor/groups/level-distribution`

Returns the department's members bucketed by the project's active levels.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `groups/overview`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "group level distribution retrieved successfully",
  "data": {
    "total_members": 9,
    "levels": [
      { "id": 1, "level_number": 0, "code": "L0", "name": "AI Starter", "count": 2, "percent": 22 }
    ]
  }
}
```

Unlike the org-wide `level-distribution`, this one lists exactly the project's active `lms_levels` rows — no placeholder levels.

**Errors** — same shape and cases as `groups/overview` above.

### `POST {base_url}/api/v1/sponsor/groups/top-use-cases`

Returns the department's five most frequently accepted use cases.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `groups/overview`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "group top use cases retrieved successfully",
  "data": {
    "total": 14,
    "use_cases": [
      {
        "id": 7,
        "name": "Rekap invoice bulanan",
        "level_code": "L2",
        "level_name": "AI Operator",
        "count": 5,
        "percent": 36
      }
    ]
  }
}
```

`total` is every accepted submission in the department, so the listed `percent` values won't add up to 100 when the tail is longer than five.

**Errors** — same shape and cases as `groups/overview` above.

### `POST {base_url}/api/v1/sponsor/groups/attention-members`

Returns the department's members ranked by how much coaching attention they need.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `groups/overview`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "group attention members retrieved successfully",
  "data": {
    "lagging_count": 4,
    "members": [
      {
        "access_id": "V7rdgcYkq9PHQZkwvoA-F",
        "full_name": "Sari Dewi",
        "avatar": null,
        "job_title": "Staff Finance",
        "level_number": 1,
        "level_name": "AI Explorer",
        "accepted_use_cases": 0,
        "inactive_days": 12,
        "status": "Pasif 12 hari",
        "needs_attention": true,
        "score": 7
      }
    ]
  }
}
```

The whole department is returned, not just the strugglers — `needs_attention` and `lagging_count` mark the subset. A member needs attention when they're at level 0/1, have no accepted use case, or haven't been active in 7 days; `score` weights those 3/2/2 respectively and drives the sort (flagged first, then highest score, then lowest level, then name). `inactive_days` is `null` for someone who has never been active, and `status` reads `Belum mulai` / `Pasif N hari` / `Aktif`.

**Errors** — same shape and cases as `groups/overview` above.

## Outcome report

### `POST {base_url}/api/v1/sponsor/outcome/overview`

Returns the end-of-program KPI tiles: cumulative hours saved, their FTE and rupiah equivalents, average level, and certification rate.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "outcome overview retrieved successfully",
  "data": {
    "member_count": 48,
    "department_count": 6,
    "hours_saved_total": 312.5,
    "fte_equivalent": 0.2,
    "roi_total": 78125000,
    "roi_rate_per_hour": 250000,
    "avg_level": 1.8,
    "max_level_number": 3,
    "certified_count": 38,
    "certified_percent": 79
  }
}
```

`fte_equivalent` expresses the saved hours as full-time staff-years at 1.760 working hours per year. `certified_*` counts everyone who has reached level 1 or above.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/outcome/level-distribution`

Returns the final spread of members across the project's active levels.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "outcome level distribution retrieved successfully",
  "data": {
    "total": 48,
    "distribution": [
      { "level_number": 0, "code": "L0", "name": "AI Starter", "count": 6, "percent": 13 }
    ]
  }
}
```

This buckets by **level number** rather than level id, so members sitting on an inactive level row still fall into the matching bucket.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/outcome/roi-trend`

Returns six months of ROI — four measured, two projected.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "roi trend retrieved successfully",
  "data": {
    "months": [
      { "label": "M1", "month": "Apr 2026", "roi_value": 12500000, "roi_billion": 0.0, "projected": false },
      { "label": "M6", "month": "Sep 2026", "roi_value": 96000000, "roi_billion": 0.1, "projected": true }
    ]
  }
}
```

The first four months are actual hours saved × Rp 250.000. The last two are extrapolated by the average monthly growth step observed across those four, and never dip below the previous month. `projected` marks them so the chart can dash that segment. `roi_billion` is the same figure in billions, rounded to one decimal, for axis labels.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/outcome/department-roi`

Returns each department's contribution to annualized ROI, ranked by last week's hours saved.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "department roi retrieved successfully",
  "data": {
    "total_roi_annualized": 546000000,
    "departments": [
      {
        "id": 3,
        "name": "Finance",
        "member_count": 9,
        "hours_saved_weekly": 8.5,
        "hours_saved_total": 88.5,
        "roi_annualized": 110500000,
        "contribution_percent": 20
      }
    ]
  }
}
```

`roi_annualized` projects the department's last 7 days across 52 weeks, which is also what `contribution_percent` is a share of. A department with no activity last week still appears, with zeroes.

**Errors** — same shape and cases as `organization-stats` above.

### `POST {base_url}/api/v1/sponsor/outcome/top-performers`

Returns every non-sponsor member ranked by a composite of XP, hours saved, use case count, and level.

**Authorization:** `Bearer <jwt>`.

**Request** — same body as `organization-stats`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "top performers retrieved successfully",
  "data": {
    "total": 47,
    "list": [
      {
        "rank": 1,
        "access_id": "V7rdgcYkq9PHQZkwvoA-F",
        "full_name": "Rani Puspita",
        "avatar": "https://...",
        "department": "Finance",
        "level_number": 3,
        "level_code": "L3",
        "level_name": "AI Builder",
        "xp": 2450,
        "use_case_count": 9,
        "hours": 42.5,
        "composite": 94
      }
    ]
  }
}
```

`composite` normalizes each of the four signals against the organization's own maximum, then weights them 40% XP, 25% hours saved, 20% use case count, 15% level — so the leader on a signal always scores full marks on it, and the number is only comparable within one project. The full ranked list is returned (not just a top N); `department` falls back to `"—"` for a member with no department.

**Errors** — same shape and cases as `organization-stats` above.
