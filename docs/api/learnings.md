# Learnings

Endpoints for the program's curriculum structure and content: a project's chapters and levels, the quizzes/videos/materials ("tasks") in one chapter, and every material in a level (the "other modules in this level" sidebar) — each annotated with the caller's own XP/completion/progress. Reading a single material/video's full detail and marking it complete lives in `docs/api/materials.md`/`docs/api/videos.md`; quiz-taking lives in `docs/api/quizzes.md`. The caller's own cross-project standing (XP, current level, pre-assessment status) lives in `docs/api/student.md` instead.

## Endpoints

### `POST {base_url}/api/v1/learnings`

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
        "attempts": 0
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

`xp_reward` on a quiz is the sum of its questions' `xp_reward`; on a video/material it's that row's own column. `xp_earned` (quizzes and videos only — omitted for materials) comes from `lms_xp_earnings` for the caller's access (`0` if not earned yet). For quizzes, `best_score`/`attempts` are derived from the caller's completed `lms_quiz_submissions` (`best_score` is `null` with zero attempts). For videos/materials, `completed` reflects `lms_video_completions`/`lms_material_completions`. Materials omit `content`/`file_url`/`image_url` here — this is a list view, not the material's full body (see `docs/api/materials.md`). `project_id` isn't part of the request — it's resolved internally from `chapter_id` via the chapter's level.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
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

Same shape and cases as `POST /api/v1/learnings` above (missing/invalid auth, expired session, blank `project_id`, or no access for the project).

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

Same shape and cases as `chapters` above (missing/invalid auth, expired session, or blank `project_id` — no `404`, since this endpoint doesn't require an `lms_accesses` row for the caller).

### `POST {base_url}/api/v1/learnings/materials`

Returns every active material in the same level as the given material — the "other modules in this level" sidebar — each numbered sequentially across chapters and annotated with the caller's completion and lock status.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "material_id": "430bb86dc0edc244188fd9eb"
}
```

| Field | Type | Required |
|---|---|---|
| `material_id` | string | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "level materials retrieved successfully",
  "data": {
    "level_number": 1,
    "materials": [
      {
        "id": "430bb86dc0edc244188fd9eb",
        "title": "Materi 1.1 — Orientasi & tetapkan target",
        "index": 1,
        "completed": true,
        "locked": false,
        "is_current": true
      },
      {
        "id": "b8e2f1a4c0edc244188fd9ec",
        "title": "Materi 1.2 — Praktik",
        "index": 2,
        "completed": false,
        "locked": true,
        "is_current": false
      }
    ]
  }
}
```

`material_id`'s chapter → level determines the level being listed; `level_number` is that level's number. `materials` covers every active material across every active chapter of that level, ordered by the chapter's `session_date` ascending then the material's `order_index` ascending, with `index` numbered sequentially across that whole ordering (not reset per chapter). `completed` comes from the caller's `lms_material_completions` rows. `locked` is `true` unless both: (1) the level is unlocked — the level's `level_number` is at or below the caller's `lms_accesses.current_level_id` level number — and (2) that material's chapter `session_date` has already started. `is_current` flags the material matching the request's `material_id`. `project_id` isn't part of the request — it's resolved internally from `material_id` via the material's chapter → level.

**Errors**

Same shape and cases as `POST /api/v1/learnings` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Material not found` | no `lms_materials` row matches `material_id` |
| 404 | `NOT_FOUND` | `Level not found` | the material's chapter references a level that no longer exists |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the material's project |
