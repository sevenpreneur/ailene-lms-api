# Learnings

Endpoints for listing the quizzes, videos, and materials ("tasks") in one chapter, for reading a single one's full detail, and for marking a material/video complete — each annotated with the caller's own XP and completion status.

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

### `POST {base_url}/api/learnings/material-details`

Returns one material's full body (`content`/`file_url`/`image_url` included — this is the detail view the list endpoint above deliberately omits them from), plus the caller's own completion status.

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
  "message": "material retrieved successfully",
  "data": {
    "id": "430bb86dc0edc244188fd9eb",
    "title": "Materi 1.1 — Orientasi & tetapkan target",
    "description": null,
    "content": "# F1.1 Orientasi dan Tetapkan Target\n\nSelamat datang di program adopsi AI...",
    "file_url": null,
    "image_url": null,
    "xp_reward": 30,
    "order_index": 1,
    "chapter": { "id": 5, "name": "Paham AI & pakai dengan benar" },
    "completed": false,
    "completed_at": null,
    "created_at": "2026-06-17T09:38:15.117Z",
    "updated_at": "2026-06-20T08:22:22.098Z"
  }
}
```

`completed`/`completed_at` come from the caller's `lms_material_completions` row (`false`/`null` if not completed yet). Materials don't track `xp_earned` separately (same as the list endpoint) — their `xp_reward` is granted in full on completion. `project_id` isn't part of the request — it's resolved internally from `material_id` via the material's chapter → level.

**Errors**

Same shape and cases as `POST /api/learnings` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Material not found` | no `lms_materials` row matches `material_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the material's project |

### `POST {base_url}/api/learnings/video-details`

Returns one video's full detail plus the caller's own completion status.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "video_id": 5
}
```

| Field | Type | Required |
|---|---|---|
| `video_id` | integer | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "video retrieved successfully",
  "data": {
    "id": 5,
    "title": "Sesi 1 — Fondasi: Cara Kerja AI & Pakai dengan Aman",
    "description": null,
    "video_url": "#",
    "xp_reward": 15,
    "order_index": 5,
    "chapter": { "id": 5, "name": "Paham AI & pakai dengan benar" },
    "completed": false,
    "completed_at": null,
    "created_at": "2026-06-17T09:38:15.117Z",
    "updated_at": "2026-06-17T09:38:15.117Z"
  }
}
```

`completed`/`completed_at` come from `lms_video_completions`. `project_id` isn't part of the request — it's resolved internally from `video_id` via the video's chapter → level.

**Errors**

Same shape and cases as `POST /api/learnings` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Video not found` | no `lms_videos` row matches `video_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the video's project |

### `POST {base_url}/api/learnings/quiz-details`

Returns one quiz's detail plus its full question list — for taking the quiz, not for reviewing a past attempt. Each question's `is_correct` and `explanation` are deliberately withheld here so the answer key can't be read off this endpoint; they only surface once a submission exists (a future `quiz-submit`/`quiz-result`-style endpoint, not this one).

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "quiz_id": "aae34047a42dddbd82cf84f8"
}
```

| Field | Type | Required |
|---|---|---|
| `quiz_id` | string | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "quiz retrieved successfully",
  "data": {
    "id": "aae34047a42dddbd82cf84f8",
    "name": "Quiz F1 — Fondasi AI",
    "description": "Fondasi AI",
    "order_index": 4,
    "chapter": { "id": 5, "name": "Paham AI & pakai dengan benar" },
    "question_count": 12,
    "xp_reward": 60,
    "attempts": 0,
    "questions": [
      {
        "id": 101,
        "question": "Pada dasarnya, sebuah LLM (Large Language Model) seperti Claude bekerja dengan cara apa saat menghasilkan jawaban?",
        "order_index": 1,
        "xp_reward": 5,
        "options": [
          { "id": 401, "option_code": "A", "text": "Mencari jawaban langsung dari basis data fakta yang selalu mutakhir" },
          { "id": 402, "option_code": "B", "text": "Menebak token (kata) berikutnya yang paling mungkin berdasarkan pola teks sebelumnya" },
          { "id": 403, "option_code": "C", "text": "Menghubungi internet secara langsung untuk setiap pertanyaan" },
          { "id": 404, "option_code": "D", "text": "Menyalin kalimat utuh dari dokumen yang pernah dibacanya" }
        ]
      }
    ]
  }
}
```

`question_count`/`xp_reward` (the quiz's total, sum of its questions')/`attempts` are the same fields and derivation as the quiz entry in `POST /api/learnings`, minus `xp_earned`/`best_score`. `questions` is ordered by `order_index` ascending, and each question's `options` by `option_code` ascending. `project_id` isn't part of the request — it's resolved internally from `quiz_id` via the quiz's chapter → level.

**Errors**

Same shape and cases as `POST /api/learnings` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Quiz not found` | no `lms_quizzes` row matches `quiz_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the quiz's project |

### `POST {base_url}/api/learnings/materials`

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

Same shape and cases as `POST /api/learnings` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Material not found` | no `lms_materials` row matches `material_id` |
| 404 | `NOT_FOUND` | `Level not found` | the material's chapter references a level that no longer exists |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the material's project |

### `POST {base_url}/api/learnings/material-completion`

Marks a material complete for the caller and awards its XP. Idempotent — calling it again for an already-completed material still returns `completed: true` with the original `completed_at`, but `xp_awarded` is `0` since the XP was already granted.

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
  "message": "material completed successfully",
  "data": {
    "material_id": "430bb86dc0edc244188fd9eb",
    "completed": true,
    "completed_at": "2026-08-26T09:38:15.117Z",
    "xp_awarded": 30
  }
}
```

`completed` is always `true` on success. `completed_at` is the timestamp of the *first* completion — replaying this call doesn't move it. `xp_awarded` is the material's `xp_reward` on first completion, `0` on every call after that (XP is granted once per `(access, material)`, backed by `lms_xp_earnings`'s unique constraint).

**Errors**

Same shape and cases as `POST /api/learnings` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Material not found` | no `lms_materials` row matches `material_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the material's project |

### `POST {base_url}/api/learnings/video-completion`

Marks a video complete for the caller and awards its XP. Same idempotency behavior as `material-completion` above.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "video_id": 5
}
```

| Field | Type | Required |
|---|---|---|
| `video_id` | integer | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "video completed successfully",
  "data": {
    "video_id": 5,
    "completed": true,
    "completed_at": "2026-08-26T09:38:15.117Z",
    "xp_awarded": 15
  }
}
```

`completed`/`completed_at`/`xp_awarded` follow the same rules as `material-completion` above.

**Errors**

Same shape and cases as `POST /api/learnings` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Video not found` | no `lms_videos` row matches `video_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the video's project |
