# Chapter surveys

A reaction survey (Kirkpatrick level 1) at the end of each chapter, the same question set as Sevenpreneur's cohort check-out rating: eight 1–5 ratings, four about the trainer (`trainer_clarity`, `trainer_mastery`, `trainer_responsiveness`, `trainer_engagement`) and four about the material (`material_relevance`, `material_flow`, `material_depth`, `learning_value`), plus four optional open answers (`favorite_material`, `missing_topics`, `disliked_material`, `improvement_suggestion`), stored in `lms_chapter_surveys`, one row per access per chapter. The question wording lives in the client; the API stores the answers. Each row records the `lms_chapter_sessions` sitting the caller attended (the group-specific one if there is one, else the all-groups one), so the `trainer_*` ratings can be read per session and therefore per trainer without joining the identity service's `trainers` table here. The survey opens once an admin has unlocked that session (`is_locked = false`; see Chapter lock in `docs/api/learnings.md`). Its state also shows up in `POST /api/v1/learnings/task` as `survey` (see `docs/api/learnings.md`). There is no aggregate/report endpoint yet.

## Endpoints

### `POST {base_url}/api/v1/chapter-surveys/details`

Returns whether the caller can answer a chapter's survey yet, and their answer if they already did.

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
  "message": "chapter survey retrieved successfully",
  "data": {
    "chapter": { "id": 5, "name": "Paham AI & pakai dengan benar" },
    "session_date": "2026-06-26T02:00:00Z",
    "available": true,
    "response": null
  }
}
```

`available` is always `true` here, because a locked chapter answers `403` instead; it is kept so the shape matches `survey` in `learnings/task`, where it is `false` while the chapter is locked. `response` is `null` until the caller submits, then has the same shape as the `submit` response below.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `chapterId: must not be null` | missing `chapter_id` |
| 403 | `FORBIDDEN` | `This chapter isn't scheduled for your group.` | no active `lms_chapter_sessions` row applies to the caller's group (see Chapter lock in `docs/api/learnings.md`) |
| 403 | `FORBIDDEN` | `This chapter is locked until an admin opens it.` | the applicable session still has `is_locked = true` |
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

### `POST {base_url}/api/v1/chapter-surveys/submit`

Stores the caller's one survey answer for a chapter.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "chapter_id": 5,
  "trainer_clarity": 5,
  "trainer_mastery": 5,
  "trainer_responsiveness": 4,
  "trainer_engagement": 5,
  "material_relevance": 4,
  "material_flow": 4,
  "material_depth": 3,
  "learning_value": 5,
  "favorite_material": "Contoh kasusnya pas dengan kerjaan tim kami.",
  "missing_topics": "Lebih banyak latihan praktik.",
  "disliked_material": null,
  "improvement_suggestion": ""
}
```

| Field | Type | Required |
|---|---|---|
| `chapter_id` | integer | yes |
| `trainer_clarity` | integer 1–5 | yes |
| `trainer_mastery` | integer 1–5 | yes |
| `trainer_responsiveness` | integer 1–5 | yes |
| `trainer_engagement` | integer 1–5 | yes |
| `material_relevance` | integer 1–5 | yes |
| `material_flow` | integer 1–5 | yes |
| `material_depth` | integer 1–5 | yes |
| `learning_value` | integer 1–5 | yes |
| `favorite_material` | string, max 2000 | no |
| `missing_topics` | string, max 2000 | no |
| `disliked_material` | string, max 2000 | no |
| `improvement_suggestion` | string, max 2000 | no |

**Response** — `201 Created`

```json
{
  "success": true,
  "code": 201,
  "status": "CREATED",
  "message": "chapter survey submitted successfully",
  "data": {
    "trainer_clarity": 5,
    "trainer_mastery": 5,
    "trainer_responsiveness": 4,
    "trainer_engagement": 5,
    "material_relevance": 4,
    "material_flow": 4,
    "material_depth": 3,
    "learning_value": 5,
    "missing_topics": "Lebih banyak latihan praktik.",
    "favorite_material": "Contoh kasusnya pas dengan kerjaan tim kami.",
    "disliked_material": null,
    "improvement_suggestion": null,
    "submitted_at": "2026-10-13T09:30:00.000Z"
  }
}
```

A missing or blank open answer is stored as `null`, and the rest are trimmed. Answers can't be edited after submitting. No XP is awarded.

**Errors**

Same auth, `403` chapter lock and `404` cases as `details` above, plus:

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `trainerClarity: must be less than or equal to 5` | a rating outside 1–5 (same for every rating, and `must not be null` when one is missing) |
| 400 | `BAD_REQUEST` | `missingTopics: size must be between 0 and 2000` | an open answer longer than 2000 characters (same for the other three) |
| 409 | `CONFLICT` | `You already submitted the survey for this chapter.` | the caller already answered this chapter |
