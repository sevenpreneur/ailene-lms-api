# Learnings

Endpoints for the program's curriculum structure and content: a project's levels and chapters, and the quizzes/videos/materials ("tasks") in one chapter — each annotated with the caller's own XP/completion/progress. Reading a single material/video's full detail, marking it complete, and listing "other modules in this level" lives in `docs/api/materials.md`/`docs/api/videos.md`; quiz-taking lives in `docs/api/quizzes.md`. The caller's own cross-project standing (XP, current level, pre-assessment status) lives in `docs/api/student.md` instead.

## Chapter lock

A chapter is locked for the caller unless its applicable session is open. The applicable session is the active `lms_chapter_sessions` row for the chapter that targets the caller's group (`only_group_id = lms_accesses.group_id`), or, if there is none, the active all-groups row (`only_group_id IS NULL`); the lowest `id` breaks a tie. The chapter is locked when no session applies or while that session has `is_locked = true`, which is the default for a new session; an admin opens it with `POST /api/v1/admin/chapter-sessions/lock` (see `docs/api/admin.md`). `session_date` is informational only and never locks or opens a chapter. Because the group's own session wins, locking it never locks the chapter for other groups, and locking the all-groups session does not lock a group that has its own session. The caller's current level plays no part: level-based locking is the client's job.

`chapters` reports the lock per chapter and `today-focus` skips locked chapters. `task` and every material, video, quiz, chapter test and chapter survey endpoint that reads or writes a chapter's content answer a locked chapter with one of these, whatever the caller's role:

| Code | Status | Message | When |
|---|---|---|---|
| 403 | `FORBIDDEN` | `This chapter isn't scheduled for your group.` | no active `lms_chapter_sessions` row applies to the caller's group (see Chapter lock in `docs/api/learnings.md`) |
| 403 | `FORBIDDEN` | `This chapter is locked until an admin opens it.` | the applicable session still has `is_locked = true` |

## Endpoints

### `POST {base_url}/api/v1/learnings/levels`

Returns a project's active `lms_levels` rows, ordered by `level_number` ascending.

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
  "message": "levels retrieved successfully",
  "data": [
    {
      "id": 1,
      "level_number": 1,
      "name": "Foundation"
    },
    {
      "id": 2,
      "level_number": 2,
      "name": "Operator Dasar & Lanjut"
    }
  ]
}
```

Levels with `status = 'inactive'` are excluded, and levels belonging to other projects never appear regardless of `project_id`. An empty array means the project has no active levels.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |

This endpoint doesn't require an `lms_accesses` row for the caller, so there's no `404` case.

### `POST {base_url}/api/v1/learnings/chapters`

Returns a project's active chapters, ordered by `session_date` ascending, each annotated with the caller's own completion progress.

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
  "message": "chapters retrieved successfully",
  "data": [
    {
      "id": 5,
      "name": "Paham AI & pakai dengan benar",
      "description": "Paham cara kerja AI tanpa jargon...",
      "session_date": "2026-06-26T02:00:00Z",
      "duration_minutes": 120,
      "location_name": "Kantor Pusat Hutama Karya",
      "location_url": "https://maps.app.goo.gl/sEBqLrLxZWiLmXGp8",
      "method": "offline",
      "level": {
        "id": 1,
        "level_number": 1,
        "name": "Foundation"
      },
      "done_tasks": 0,
      "total_tasks": 5,
      "progress": "not_started",
      "locked": true,
      "locked_reason": "admin_locked"
    }
  ]
}
```

`total_tasks` is the number of active quizzes + videos + materials in that chapter. `done_tasks` counts the caller's own completed items among those (a quiz counts once it has any `is_completed` submission, videos/materials count via `lms_video_completions`/`lms_material_completions`). `progress` is derived: `not_started` when nothing's done, `completed` when `done_tasks >= total_tasks`, otherwise `in_progress`. Only chapters whose level belongs to `project_id` are returned; inactive chapters are excluded, and so are chapters with no session for the caller's group. `session_date`, `duration_minutes`, `location_*` and `method` come from the applicable session (see Chapter lock above). `locked` is that rule's result for the caller, and `locked_reason` is `admin_locked` while the session has `is_locked = true`, `null` when the chapter is open.

**Errors**

Same shape and cases as `levels` above (missing/invalid auth, expired session, blank `project_id`), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for this `project_id` |

### `POST {base_url}/api/v1/learnings/task`

Returns a chapter's active quizzes, videos, and materials, each ordered by `order_index` ascending, plus the chapter's pre-test, post-test and survey state.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "chapter_id": 5
}
```

| Field | Type | Required |
|---|---|---|
| `chapter_id` | integer | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "learnings retrieved successfully",
  "data": {
    "quizzes": [
      {
        "id": "aae34047a42dddbd82cf84f8",
        "name": "Quiz F1 — Fondasi AI",
        "description": "Fondasi AI",
        "order_index": 4,
        "question_count": 12,
        "xp_reward": 60,
        "xp_earned": 0,
        "best_score": null,
        "attempts": 0,
        "active_attempt_started_at": null
      }
    ],
    "videos": [
      {
        "id": 5,
        "title": "Sesi 1 — Fondasi: Cara Kerja AI & Pakai dengan Aman",
        "description": null,
        "video_url": "#",
        "xp_reward": 15,
        "order_index": 5,
        "xp_earned": 0,
        "completed": false,
        "timestamps": [
          { "start_seconds": 0, "title": "Chapter 0 - Sebenarnya, separah apa sih kebakaran ini?" },
          { "start_seconds": 212, "title": "Chapter 1 - Apakah Kapasitas Pemerintah Benar-Benar Cukup Buat Nanganin Ini Sendiri?" }
        ]
      }
    ],
    "materials": [
      {
        "id": "430bb86dc0edc244188fd9eb",
        "title": "Materi 1.1 — Orientasi & tetapkan target",
        "description": null,
        "xp_reward": 30,
        "order_index": 1,
        "completed": false
      }
    ],
    "pre_test": {
      "id": 11,
      "kind": "pre_test",
      "name": "Pre-test — Fondasi AI",
      "description": null,
      "question_count": 5,
      "submitted": true,
      "score": 40,
      "submitted_at": "2026-06-26T01:40:00Z",
      "available": true,
      "locked_reason": null
    },
    "post_test": {
      "id": 12,
      "kind": "post_test",
      "name": "Post-test — Fondasi AI",
      "description": null,
      "question_count": 5,
      "submitted": false,
      "score": null,
      "submitted_at": null,
      "available": false,
      "locked_reason": "materials_incomplete"
    },
    "survey": {
      "available": true,
      "submitted": false,
      "submitted_at": null
    }
  }
}
```

`xp_reward` on a quiz is the sum of its questions' `xp_reward`; on a video/material it's that row's own column. `xp_earned` (quizzes and videos only — omitted for materials) comes from `lms_xp_earnings` for the caller's access (`0` if not earned yet). For quizzes, `best_score`/`attempts` are derived from the caller's completed `lms_quiz_submissions` (`best_score` is `null` with zero attempts). `active_attempt_started_at` is that quiz's non-finalized `lms_quiz_submissions.started_at` for the caller (`null` if there's no draft in progress — the client shows "Mulai Quiz"; a timestamp means one's in progress — the client shows "Lanjutkan Quiz" and derives the remaining time itself from `started_at` plus the quiz's 20-minute limit, same limit `POST /api/v1/quizzes/attempt` uses, see `docs/api/quizzes.md`). For videos/materials, `completed` reflects `lms_video_completions`/`lms_material_completions`. A video's `timestamps` are its `lms_video_timestamps` rows ordered by `start_seconds` ascending, the chapter markers the client offers as "jump to minute" shortcuts; it is `[]` when the video has none. Materials omit `content`/`file_url`/`image_url` here — this is a list view, not the material's full body (see `docs/api/materials.md`). `project_id` isn't part of the request — it's resolved internally from `chapter_id` via the chapter's level.

`pre_test`/`post_test` are `null` when the chapter has no active test of that kind; otherwise `available`/`locked_reason` follow the same rules as `POST /api/v1/chapter-tests/details` (see `docs/api/chapter-tests.md`), and `score` is `null` until submitted or for an essay-only test. `survey.available` is `true` once the chapter is unlocked for the caller (see `docs/api/chapter-surveys.md`). None of the three count toward `total_tasks`/`done_tasks` in `chapters`, nor toward any champion/sponsor completion figure.

**Errors**

Same shape and cases as `levels` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `chapterId: must not be null` | missing `chapter_id` field |
| 404 | `NOT_FOUND` | `Chapter not found` | no `lms_chapters` row matches `chapter_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the chapter's project |
| 403 | `FORBIDDEN` | `This chapter isn't scheduled for your group.` | no active `lms_chapter_sessions` row applies to the caller's group (see Chapter lock in `docs/api/learnings.md`) |
| 403 | `FORBIDDEN` | `This chapter is locked until an admin opens it.` | the applicable session still has `is_locked = true` |

Example error response (`404 Not Found`):

```json
{
  "success": false,
  "code": 404,
  "status": "NOT_FOUND",
  "message": "Chapter not found"
}
```

### `POST {base_url}/api/v1/learnings/today-focus`

Returns the single next thing the caller should do in the project — the "today focus" widget on the student dashboard.

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
  "message": "today focus retrieved successfully",
  "data": {
    "focus": {
      "kind": "material",
      "task_id": "430bb86dc0edc244188fd9eb",
      "task_title": "Materi 1.1 — Orientasi & tetapkan target",
      "chapter_id": 5,
      "chapter_name": "Paham AI & pakai dengan benar",
      "level_id": 1,
      "level_number": 1,
      "category": null,
      "assigned_by": null,
      "deadline": null
    }
  }
}
```

`data.focus` is `null` when there's nothing left to do (every unlocked chapter's tasks are complete and no assignment is pending). Otherwise `kind` is one of `material` / `quiz` / `video` / `prompt_practice` / `use_case_practice`, and `task_id` is always a string regardless of the underlying table's id type.

Selection walks the project's active chapters in `session_date` order, skipping any chapter whose level is above the caller's current level and any chapter that is locked for the caller (`is_locked = true` on its applicable session; see Chapter lock above). The level check here only picks a sensible next step; it is not an access lock. For the first eligible chapter, it picks — in this priority — the chapter's first incomplete material, else its first incomplete quiz, else the caller's nearest-deadline pending assignment (an `lms_prompt_submissions`/`lms_use_case_submissions` row with an assigner and a deadline but no submission yet, across the whole project, not just this chapter), else the chapter's first incomplete video, then stops. If no chapter yields anything, the nearest-deadline pending assignment (if any) is returned instead. `chapter_id`/`chapter_name`/`level_id`/`level_number` are `null` for `prompt_practice`/`use_case_practice` foci (those aren't chapter-scoped); `category` is that prompt/use case's first assigned category (`null` if it has none), and `assigned_by` is `null` for `material`/`quiz`/`video`.

**Errors**

Same shape and cases as `levels` above (missing/invalid auth, expired session, blank `project_id`), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for this `project_id` |
