# Learnings

Endpoints for the program's curriculum structure and content: a project's levels and chapters, and the quizzes/videos/materials ("tasks") in one chapter — each annotated with the caller's own XP/completion/progress. Reading a single material/video's full detail, marking it complete, and listing "other modules in this level" lives in `docs/api/materials.md`/`docs/api/videos.md`; quiz-taking lives in `docs/api/quizzes.md`. The caller's own cross-project standing (XP, current level, pre-assessment status) lives in `docs/api/student.md` instead.

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
      "progress": "not_started"
    }
  ]
}
```

`total_tasks` is the number of active quizzes + videos + materials in that chapter. `done_tasks` counts the caller's own completed items among those (a quiz counts once it has any `is_completed` submission, videos/materials count via `lms_video_completions`/`lms_material_completions`). `progress` is derived: `not_started` when nothing's done, `completed` when `done_tasks >= total_tasks`, otherwise `in_progress`. Only chapters whose level belongs to `project_id` are returned; inactive chapters are excluded.

**Errors**

Same shape and cases as `levels` above (missing/invalid auth, expired session, blank `project_id`), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for this `project_id` |

### `POST {base_url}/api/v1/learnings/task`

Returns a chapter's active quizzes, videos, and materials, each ordered by `order_index` ascending.

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
        "completed": false
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
    ]
  }
}
```

`xp_reward` on a quiz is the sum of its questions' `xp_reward`; on a video/material it's that row's own column. `xp_earned` (quizzes and videos only — omitted for materials) comes from `lms_xp_earnings` for the caller's access (`0` if not earned yet). For quizzes, `best_score`/`attempts` are derived from the caller's completed `lms_quiz_submissions` (`best_score` is `null` with zero attempts). `active_attempt_started_at` is that quiz's non-finalized `lms_quiz_submissions.started_at` for the caller (`null` if there's no draft in progress — the client shows "Mulai Quiz"; a timestamp means one's in progress — the client shows "Lanjutkan Quiz" and derives the remaining time itself from `started_at` plus the quiz's 20-minute limit, same limit `POST /api/v1/quizzes/attempt` uses, see `docs/api/quizzes.md`). For videos/materials, `completed` reflects `lms_video_completions`/`lms_material_completions`. Materials omit `content`/`file_url`/`image_url` here — this is a list view, not the material's full body (see `docs/api/materials.md`). `project_id` isn't part of the request — it's resolved internally from `chapter_id` via the chapter's level.

**Errors**

Same shape and cases as `levels` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `chapterId: must not be null` | missing `chapter_id` field |
| 404 | `NOT_FOUND` | `Chapter not found` | no `lms_chapters` row matches `chapter_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the chapter's project |

Example error response (`404 Not Found`):

```json
{
  "success": false,
  "code": 404,
  "status": "NOT_FOUND",
  "message": "Chapter not found"
}
```
