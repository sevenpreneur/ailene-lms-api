# Student

Endpoints for a logged-in learner's own standing and performance within a specific project — XP, current level and progress, badges/achievements, a competency radar, and their group's leaderboard, and (later) things like a "today's focus" recommendation. Curriculum structure and content (chapters, levels, tasks) lives in `docs/api/learnings.md` instead.

## Endpoints

### `POST {base_url}/api/v1/student/status`

Returns the caller's XP, current level, and pre-assessment status for one project.

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
  "message": "student status retrieved successfully",
  "data": {
    "xp_count": 350,
    "current_level_number": 2,
    "has_pre_assessment": true
  }
}
```

`xp_count` is the sum of `lms_xp_earnings.xp_earned` for the caller's access in this project (`0` if none earned yet). `current_level_number` is `lms_levels.level_number` for the access's `current_level_id`, or `null` if no level has been set. `has_pre_assessment` is whether an `lms_pre_assessments` row exists for this access.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for this `project_id` |

Example error response (`404 Not Found`):

```json
{
  "success": false,
  "code": 404,
  "status": "NOT_FOUND",
  "message": "No access found for this project"
}
```

### `POST {base_url}/api/v1/student/level-progress`

Returns the caller's progress within their *current* level (modules done/required, whether the next level is unlockable) plus their all-time achievement badges (approved use cases/prompts, hours saved, tools mastered).

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
  "message": "level progress retrieved successfully",
  "data": {
    "total_xp": 215,
    "current_level_number": 1,
    "current_level_name": "Foundation",
    "tasks_required": 8,
    "tasks_done": 6,
    "next_level_unlockable": false,
    "use_case_approved_count": 3,
    "prompt_approved_count": 5,
    "hours_saved_total": 12.5,
    "tools_mastered": ["ChatGPT", "Claude", "Gemini"]
  }
}
```

`total_xp` is the sum of `lms_xp_earnings.xp_earned` for the caller's access. `current_level_number`/`current_level_name` come from the access's `current_level_id` (both `null` if no level has been set yet, and `tasks_required`/`tasks_done` are then `0`). `tasks_required` is the number of active quizzes + materials (videos don't count) across every active chapter of the *current* level; `tasks_done` counts the caller's own completions among those (a quiz counts once it has any `is_completed` submission). `next_level_unlockable` is `tasks_done >= tasks_required` — it doesn't itself unlock anything, it's just a hint for the client. `use_case_approved_count`/`prompt_approved_count` are the caller's `is_accepted = true` `lms_use_case_submissions`/`lms_prompt_submissions` rows for this project. `hours_saved_total` sums `hours_without_ai - hours_with_ai` (only positive deltas) across the caller's *approved* use case submissions, rounded to 1 decimal. `tools_mastered` is the deduplicated (case-insensitive) list of `ai_tool` values across those same approved use case submissions, comma-split per submission, first-seen casing preserved.

**Errors**

Same shape and cases as `status` above (missing/invalid auth, expired session, blank `project_id`, or no access for the project).

### `POST {base_url}/api/v1/student/competency`

Returns the caller's competency profile: a score (0–5) per dimension for a radar chart, an overall average, and a maturity tier.

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
  "message": "competency profile retrieved successfully",
  "data": {
    "dimensions": [
      { "key": "ai_foundation", "name": "AI Foundation", "score": 3.5 },
      { "key": "prompting_quality", "name": "Prompting Quality", "score": 2.8 },
      { "key": "tool_fluency", "name": "Tool Fluency", "score": 4.0 },
      { "key": "use_case_diversity", "name": "Use Case Diversity", "score": 2.1 },
      { "key": "ai_habit", "name": "AI Habit", "score": 3.2 },
      { "key": "agentic_capabilities", "name": "Agentic Capabilities", "score": 0.0 }
    ],
    "avg": 2.6,
    "current_level_number": 1,
    "tier_number": 2,
    "tier_name": "Operator",
    "next_tier": { "number": 3, "name": "Builder", "avg_needed": 1.4 }
  }
}
```

Each dimension is a `0`–`5` score, rounded to 1 decimal:
- `ai_foundation` — average of the caller's best score per quiz (across every quiz in the project, from completed `lms_quiz_submissions`), scaled from `0`–`100` down to `0`–`5`.
- `prompting_quality` — mean of the 5-part rubric (specificity/context/constraints/examples/iteration, as scored by the AI or overridden by the champion) across the caller's evaluated `lms_prompt_submissions` in the last 90 days, dated by `ai_evaluated_at` (or `reviewed_at` for older, champion-scored rows); if fewer than 3 fall in that window, falls back to the 3 most recently evaluated overall (submissions missing any rubric value are skipped entirely).
- `tool_fluency` — `0.4 * tool breadth + 0.4 * artifact rate + 0.2 * multi-model bonus`, derived from every *submitted* `lms_use_case_submissions` row (not just approved ones): distinct `ai_tool` values, the share of submissions with a non-blank `outcome_proof`, and a bonus for using 2+ (partial) or 3+ (full) distinct tools.
- `use_case_diversity` — distinct `type` values among submitted use cases (capped contribution), plus a small bonus for `is_accepted = true` ones.
- `ai_habit` — `0.6 * hours-saved score + 0.4 * consistency`, where hours-saved comes from all submitted use cases' `hours_without_ai - hours_with_ai` (positive deltas only), and consistency is the caller's count of distinct active days (quiz completions + video completions + material completions) in the last 90 days, out of 90.
- `agentic_capabilities` — always `0` for now; no agentic/portfolio feature exists yet.

`avg` is the mean of all 6 dimension scores. `tier_name`/`tier_number` come from `avg`: `< 1` Beginner (`0`), `< 2` Explorer (`1`), `< 3` Operator (`2`), `< 4` Builder (`3`), else Master (`4`). `next_tier` is `null` at the Master tier; otherwise `avg_needed` is how much more `avg` needs to reach the next tier's threshold.

**Errors**

Same shape and cases as `status` above (missing/invalid auth, expired session, blank `project_id`, or no access for the project).

### `POST {base_url}/api/v1/student/leaderboard`

Returns the caller's group ranked by total XP.

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

**Response** — `200 OK`, caller belongs to a group:

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "leaderboard retrieved successfully",
  "data": {
    "group": { "id": 1, "name": "Human Capital" },
    "my_rank": 2,
    "total": 3,
    "leaderboard": [
      { "rank": 1, "access_id": "abc123", "full_name": "Top Scorer", "avatar": null, "total_xp": 500, "is_me": false },
      { "rank": 2, "access_id": "-2xph_GzlulJA0SlZy1GP", "full_name": "Akmal Luthfiansyah", "avatar": "https://...", "total_xp": 215, "is_me": true },
      { "rank": 3, "access_id": "xyz789", "full_name": "Newcomer", "avatar": null, "total_xp": 0, "is_me": false }
    ]
  }
}
```

**Response** — `200 OK`, caller has no group (`lms_accesses.group_id` is `null`):

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "leaderboard retrieved successfully",
  "data": { "group": null, "my_rank": 0, "total": 0, "leaderboard": [] }
}
```

`leaderboard` covers every access in the caller's group (same `project_id` + `group_id`, any role), ordered by `total_xp` descending (sum of `lms_xp_earnings.xp_earned`, `0` if none earned). `my_rank` is the caller's 1-indexed position; `is_me` flags their own row.

**Errors**

Same shape and cases as `status` above (missing/invalid auth, expired session, blank `project_id`, or no access for the project).
