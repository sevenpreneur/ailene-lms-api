# Quizzes

Endpoints for taking a quiz: viewing its question list, starting or resuming a timed attempt, autosaving answers to that attempt's draft, submitting for a final score, and reading back a past result. A quiz attempt has a 20-minute (1200s) time limit measured from `started_at`; once it elapses, the next call that touches that attempt (`attempt`, `update`, or the QStash-scheduled `auto-submit` callback below) finalizes it automatically using whatever answers were last saved, scores it, and awards XP — the student never needs to explicitly "run out the clock" client-side.

## Endpoints

### `POST {base_url}/api/v1/quizzes/details`

Returns one quiz's detail plus its full question list — for taking the quiz, not for reviewing a past attempt. Each question's `is_correct` and `explanation` are deliberately withheld here so the answer key can't be read off this endpoint; they only surface once a submission exists, via `POST /api/v1/quizzes/result` below.

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

`question_count`/`xp_reward` (the quiz's total, sum of its questions')/`attempts` are the same fields and derivation as the quiz entry in `POST /api/v1/learnings`, minus `xp_earned`/`best_score`. `questions` is ordered by `order_index` ascending, and each question's `options` by `option_code` ascending. `project_id` isn't part of the request — it's resolved internally from `quiz_id` via the quiz's chapter → level. The quiz's level must already be unlocked for the caller (see `Level not unlocked yet` below).

**Errors**

Same shape and cases as `POST /api/v1/learnings` (missing/invalid auth, expired session — see `docs/api/learnings.md`), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Quiz not found` | no `lms_quizzes` row matches `quiz_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the quiz's project |
| 403 | `FORBIDDEN` | `This level hasn't been unlocked yet.` | the quiz's `level_number` is above the caller's `lms_accesses.current_level_id` level number |

### `POST {base_url}/api/v1/quizzes/attempt`

Starts a new quiz attempt, or resumes the caller's existing in-progress draft for that quiz (there's at most one non-finalized `lms_quiz_submissions` row per `(access, quiz)` at a time). If the resumed draft has already run past its 20-minute limit, it's finalized on the spot instead of resumed. Starting a brand-new attempt also schedules a QStash job (see `auto-submit` below) that finalizes it automatically after 20 minutes even if the client never calls back.

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

**Response** — `200 OK`, new or resumed attempt still active:

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "quiz attempt started",
  "data": {
    "status": "active",
    "submission_id": 501,
    "started_at": "2026-08-26T09:00:00Z",
    "server_now": "2026-08-26T09:05:00Z",
    "seconds_left": 900,
    "answers": { "101": "B" }
  }
}
```

**Response** — `200 OK`, the resumed draft had already timed out (auto-finalized by this call):

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "quiz attempt started",
  "data": {
    "status": "finalized",
    "submission_id": null,
    "started_at": null,
    "server_now": null,
    "seconds_left": null,
    "answers": null
  }
}
```

`status` is `"active"` for a new or still-in-time attempt, `"finalized"` when the existing draft had already run out the clock and was just scored instead (call `POST /api/v1/quizzes/result` to read the outcome). On a new attempt, `answers` starts as `{}`; on a resumed one it's whatever was last saved via `update` below, keyed by `question_id` (string) → `option_code`. `attempt_number` isn't returned here — it's only surfaced by `submit`/`result`. Same level-unlock requirement as `details` above.

**Errors**

Same shape and cases as `POST /api/v1/learnings` (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Quiz not found` | no `lms_quizzes` row matches `quiz_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the quiz's project |
| 403 | `FORBIDDEN` | `This level hasn't been unlocked yet.` | the quiz's `level_number` is above the caller's `lms_accesses.current_level_id` level number |

### `POST {base_url}/api/v1/quizzes/update`

Autosaves answers to the caller's active (not yet completed) draft for a quiz — meant to be called repeatedly as the student answers questions, well before they hit "submit". Unlike `attempt`/`submit`, this endpoint does **not** re-check whether the quiz's level is still unlocked (an attempt already in progress is allowed to run to completion), it only requires an active draft to exist.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "quiz_id": "aae34047a42dddbd82cf84f8",
  "answers": { "101": "B", "102": null }
}
```

| Field | Type | Required |
|---|---|---|
| `quiz_id` | string | yes |
| `answers` | object (`question_id` string → `option_code` string or `null`) | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "quiz draft saved",
  "data": { "status": "active" }
}
```

`answers` fully replaces the draft's stored answers (not a merge). `status` is `"active"` on a normal save, or `"finalized"` if the draft had already run past its 20-minute limit — in which case the submitted `answers` in this call are discarded and the draft is scored using whatever was last saved instead (call `POST /api/v1/quizzes/result` to read the outcome).

**Errors**

Same shape and cases as `POST /api/v1/learnings` (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Quiz not found` | no `lms_quizzes` row matches `quiz_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the quiz's project |
| 400 | `BAD_REQUEST` | `No active attempt. Start an attempt first.` | no non-finalized `lms_quiz_submissions` row exists for this `(access, quiz)` — call `attempt` first |

### `POST {base_url}/api/v1/quizzes/submit`

Finalizes a quiz attempt: scores the given answers against the answer key, marks the submission `is_completed = true`, and awards XP. Works whether or not an active draft already exists — if the caller never called `attempt` first, one is created and immediately finalized with the given answers. If the caller's active draft already timed out, its `started_at`-derived deadline is irrelevant here — `submit` always scores exactly the `answers` passed in this call (unlike `attempt`/`update`'s auto-finalize path, which reuses the draft's last-saved answers).

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "quiz_id": "aae34047a42dddbd82cf84f8",
  "answers": { "101": "B", "102": "A" }
}
```

| Field | Type | Required |
|---|---|---|
| `quiz_id` | string | yes |
| `answers` | object (`question_id` string → `option_code` string or `null`) | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "quiz submitted successfully",
  "data": { "score": 83, "xp_awarded": 45, "attempt_number": 1 }
}
```

`score` is `round(correct_answers / question_count * 100)`, `0` if the quiz has no questions. XP uses a best-attempt-wins rule scoped to `(access, quiz)`: the sum of `xp_reward` from correctly-answered questions on *this* attempt is compared against whatever XP was already earned for this quiz (if any), and `lms_xp_earnings` is only raised, never lowered — `xp_awarded` is the delta actually added this call (`0` if this attempt didn't beat the existing best). Calling `submit` again for an already-completed attempt is a no-op: it returns the stored `score` with `xp_awarded: 0` rather than re-scoring. Same level-unlock requirement as `details` above.

**Errors**

Same shape and cases as `POST /api/v1/learnings` (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Quiz not found` | no `lms_quizzes` row matches `quiz_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the quiz's project |
| 403 | `FORBIDDEN` | `This level hasn't been unlocked yet.` | the quiz's `level_number` is above the caller's `lms_accesses.current_level_id` level number |

### `POST {base_url}/api/v1/quizzes/result`

Returns the caller's most recent **completed** attempt for a quiz (highest `attempt_number` among finalized submissions), together with the full question list — this time including `explanation` and each option's `is_correct`, since a submission already exists.

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
  "message": "quiz result retrieved successfully",
  "data": {
    "id": "aae34047a42dddbd82cf84f8",
    "name": "Quiz F1 — Fondasi AI",
    "description": "Fondasi AI",
    "chapter": { "id": 5, "name": "Paham AI & pakai dengan benar" },
    "questions": [
      {
        "id": 101,
        "question": "Pada dasarnya, sebuah LLM (Large Language Model) seperti Claude bekerja dengan cara apa saat menghasilkan jawaban?",
        "order_index": 1,
        "xp_reward": 5,
        "explanation": "LLM memprediksi token berikutnya secara statistik, bukan mengambil fakta dari basis data.",
        "options": [
          { "id": 401, "option_code": "A", "text": "Mencari jawaban langsung dari basis data fakta yang selalu mutakhir", "is_correct": false },
          { "id": 402, "option_code": "B", "text": "Menebak token (kata) berikutnya yang paling mungkin berdasarkan pola teks sebelumnya", "is_correct": true }
        ]
      }
    ],
    "submission": {
      "attempt_number": 1,
      "score": 83,
      "answers": { "101": "B" },
      "submitted_at": "2026-08-26T09:20:00Z"
    },
    "xp_earned": 45
  }
}
```

`xp_earned` is the caller's current `lms_xp_earnings` value for this quiz (the best-attempt-wins total from `submit`, not necessarily this particular attempt's own XP). `project_id` isn't part of the request — resolved internally from `quiz_id` via the quiz's chapter → level, same as `details`.

**Errors**

Same shape and cases as `POST /api/v1/learnings` (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Quiz not found` | no `lms_quizzes` row matches `quiz_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the quiz's project |
| 404 | `NOT_FOUND` | `Quiz submission not found` | the caller has no completed `lms_quiz_submissions` row for this quiz yet |

### `POST {base_url}/api/v1/quizzes/auto-submit` (internal)

Not a student-facing endpoint. This is the callback `attempt` schedules via QStash (`QSTASH_TOKEN`/`APP_BASE_URL` env vars) to fire ~20 minutes after a new attempt starts, so a submission still gets finalized and scored even if the client never calls back. Gated by the same static `SECRET_KEY` bearer token as `POST /api/v1/hello-world` (see `docs/auth.md`) rather than a student JWT — QStash is configured to forward that header on delivery. Idempotent: finalizing an already-completed submission is a silent no-op.

**Authorization:** `Bearer <SECRET_KEY>`.

**Request**

```json
{ "submission_id": 501 }
```

| Field | Type | Required |
|---|---|---|
| `submission_id` | integer | yes |

**Response** — `200 OK`, `data` is always `null`.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Bearer token is invalid` | wrong or missing bearer token |
| 404 | `NOT_FOUND` | `Quiz submission not found` | no `lms_quiz_submissions` row matches `submission_id` |
