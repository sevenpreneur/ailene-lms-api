# Chapter surveys

A short reaction survey (Kirkpatrick level 1) at the end of each chapter: three 1–5 ratings (overall satisfaction, relevance to the caller's work, and how well the trainer delivered the session) plus an optional comment, stored in `lms_chapter_surveys`, one row per access per chapter. The question wording lives in the client; the API stores the three numbers. Each row records the `lms_chapter_sessions` sitting the caller attended (the group-specific one if there is one, else the all-groups one), so `trainer_rating` can be read per session and therefore per trainer without joining the identity service's `trainers` table here. The survey opens once an admin has unlocked that session (`is_locked = false`; see Chapter lock in `docs/api/learnings.md`). Its state also shows up in `POST /api/v1/learnings/task` as `survey` (see `docs/api/learnings.md`). There is no aggregate/report endpoint yet.

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
  "satisfaction": 5,
  "relevance": 4,
  "trainer_rating": 5,
  "comment": "Contoh kasusnya pas dengan kerjaan tim kami."
}
```

| Field | Type | Required |
|---|---|---|
| `chapter_id` | integer | yes |
| `satisfaction` | integer 1–5 | yes |
| `relevance` | integer 1–5 | yes |
| `trainer_rating` | integer 1–5 | yes |
| `comment` | string, max 2000 | no |

**Response** — `201 Created`

```json
{
  "success": true,
  "code": 201,
  "status": "CREATED",
  "message": "chapter survey submitted successfully",
  "data": {
    "satisfaction": 5,
    "relevance": 4,
    "trainer_rating": 5,
    "comment": "Contoh kasusnya pas dengan kerjaan tim kami.",
    "submitted_at": "2026-10-13T09:30:00.000Z"
  }
}
```

A blank `comment` is stored as `null`. Answers can't be edited after submitting. No XP is awarded.

**Errors**

Same auth, `403` chapter lock and `404` cases as `details` above, plus:

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `satisfaction: must be less than or equal to 5` | a rating outside 1–5 (same for `relevance`/`trainerRating`, and `must not be null` when missing) |
| 400 | `BAD_REQUEST` | `comment: size must be between 0 and 2000` | comment too long |
| 409 | `CONFLICT` | `You already submitted the survey for this chapter.` | the caller already answered this chapter |
