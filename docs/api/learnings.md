# Learnings

Endpoint for listing the quizzes, videos, and materials ("tasks") in one chapter, each annotated with the caller's own XP and completion status.

## Endpoints

### `POST {base_url}/api/learnings`

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

`xp_reward` on a quiz is the sum of its questions' `xp_reward`; on a video/material it's that row's own column. `xp_earned` (quizzes and videos only — omitted for materials) comes from `lms_xp_earnings` for the caller's access (`0` if not earned yet). For quizzes, `best_score`/`attempts` are derived from the caller's completed `lms_quiz_submissions` (`best_score` is `null` with zero attempts). For videos/materials, `completed` reflects `lms_video_completions`/`lms_material_completions`. Materials omit `content`/`file_url`/`image_url` here — this is a list view, not the material's full body. `project_id` isn't part of the request — it's resolved internally from `chapter_id` via the chapter's level.

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
