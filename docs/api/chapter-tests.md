# Chapter tests

A chapter can carry one pre-test and one post-test (`lms_chapter_tests.kind`), each a list of multiple-choice and/or essay questions, so the program can measure learning (Kirkpatrick level 2) as the gap between the two scores. Each test is taken once per access. Only multiple-choice questions are scored; essays are stored as text for a person to read. The order is enforced here, not by the client: the post-test needs the pre-test submitted (when the chapter has one) and every active material in the chapter completed, and the pre-test closes once the post-test is in. Both tests show up in `POST /api/v1/learnings/task` as `pre_test`/`post_test` (see `docs/api/learnings.md`). There is no admin endpoint yet; tests are authored straight in Neon.

## Endpoints

### `POST {base_url}/api/v1/chapter-tests/details`

Returns one test with its questions for taking it, without the answer key.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "test_id": 12
}
```

| Field | Type | Required |
|---|---|---|
| `test_id` | integer | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "chapter test retrieved successfully",
  "data": {
    "id": 12,
    "kind": "post_test",
    "name": "Post-test — Fondasi AI",
    "description": "Ukur pemahaman setelah belajar chapter ini.",
    "chapter": { "id": 5, "name": "Paham AI & pakai dengan benar" },
    "question_count": 2,
    "choice_count": 1,
    "essay_count": 1,
    "submitted": false,
    "available": false,
    "locked_reason": "materials_incomplete",
    "questions": [
      {
        "id": 31,
        "type": "multiple_choice",
        "question": "Apa yang paling tepat menggambarkan cara kerja large language model?",
        "order_index": 1,
        "options": [
          { "id": 90, "option_code": "A", "text": "Mencari jawaban dari basis data fakta" },
          { "id": 91, "option_code": "B", "text": "Memprediksi kata berikutnya dari pola data latih" }
        ]
      },
      {
        "id": 32,
        "type": "essay",
        "question": "Ceritakan satu pekerjaanmu yang menurutmu bisa dibantu AI, dan kenapa.",
        "order_index": 2,
        "options": []
      }
    ]
  }
}
```

`available` is `false` exactly when `locked_reason` is set: `pre_test_required` (post-test, the chapter's pre-test isn't submitted yet), `materials_incomplete` (post-test, at least one active material in the chapter isn't completed) or `post_test_submitted` (pre-test, the post-test is already in so a pre-test now would measure nothing). A submitted test is always `available: true` with `locked_reason: null`, so the client can open its result. Essay questions have an empty `options` array.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `testId: must not be null` | missing `test_id` |
| 403 | `FORBIDDEN` | `This chapter isn't scheduled for your group.` | no active `lms_chapter_sessions` row applies to the caller's group (see Chapter lock in `docs/api/learnings.md`) |
| 403 | `FORBIDDEN` | `This chapter is locked until an admin opens it.` | the applicable session still has `is_locked = true` |
| 404 | `NOT_FOUND` | `Test not found` | no `lms_chapter_tests` row with that id, or it is `inactive` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the chapter's project |

Example error response (`404 Not Found`):

```json
{
  "success": false,
  "code": 404,
  "status": "NOT_FOUND",
  "message": "Test not found"
}
```

### `POST {base_url}/api/v1/chapter-tests/submit`

Grades and stores the caller's one attempt at a test.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "test_id": 12,
  "answers": {
    "31": "B",
    "32": "Saya pakai AI untuk merangkum notulen rapat mingguan."
  }
}
```

| Field | Type | Required |
|---|---|---|
| `test_id` | integer | yes |
| `answers` | object | yes — question id (as a string) to an `option_code` for multiple choice, or free text for an essay |

**Response** — `201 Created`

```json
{
  "success": true,
  "code": 201,
  "status": "CREATED",
  "message": "chapter test submitted successfully",
  "data": {
    "score": 100,
    "correct_count": 1,
    "choice_count": 1,
    "essay_count": 1,
    "submitted_at": "2026-10-13T09:12:44.120Z"
  }
}
```

`score` is the percentage of multiple-choice questions answered correctly, rounded, or `null` for a test with no multiple-choice questions. Every question must be answered; answers are trimmed before they are stored, and an essay may be up to 5000 characters. No XP is awarded for tests.

**Errors**

Same auth, `403` chapter lock and `404` cases as `details` above, plus:

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `answers: must not be null` | missing `answers` |
| 400 | `BAD_REQUEST` | `Answer every question before submitting.` | a question is missing or blank |
| 400 | `BAD_REQUEST` | `'Z' is not an option for question 31` | a multiple-choice answer isn't one of that question's `option_code`s |
| 400 | `BAD_REQUEST` | `Unknown question id: 99` | a key in `answers` isn't a question of this test |
| 400 | `BAD_REQUEST` | `The answer to question 32 is longer than 5000 characters.` | an essay over the limit |
| 403 | `FORBIDDEN` | `Finish this chapter's pre-test first.` | `locked_reason` is `pre_test_required` |
| 403 | `FORBIDDEN` | `Read every material in this chapter before the post-test.` | `locked_reason` is `materials_incomplete` |
| 403 | `FORBIDDEN` | `The pre-test closes once the post-test is submitted.` | `locked_reason` is `post_test_submitted` |
| 409 | `CONFLICT` | `You already submitted this test.` | the caller already has a submission for this test |

### `POST {base_url}/api/v1/chapter-tests/result`

Returns the caller's submitted attempt with each question and their answer.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "test_id": 12
}
```

| Field | Type | Required |
|---|---|---|
| `test_id` | integer | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "chapter test result retrieved successfully",
  "data": {
    "id": 12,
    "kind": "post_test",
    "name": "Post-test — Fondasi AI",
    "description": "Ukur pemahaman setelah belajar chapter ini.",
    "chapter": { "id": 5, "name": "Paham AI & pakai dengan benar" },
    "answers_revealed": true,
    "submission": {
      "score": 100,
      "correct_count": 1,
      "choice_count": 1,
      "submitted_at": "2026-10-13T09:12:44.120Z"
    },
    "questions": [
      {
        "id": 31,
        "type": "multiple_choice",
        "question": "Apa yang paling tepat menggambarkan cara kerja large language model?",
        "order_index": 1,
        "explanation": "LLM memprediksi token berikutnya berdasarkan pola dari data latihnya.",
        "options": [
          { "id": 90, "option_code": "A", "text": "Mencari jawaban dari basis data fakta", "is_correct": false },
          { "id": 91, "option_code": "B", "text": "Memprediksi kata berikutnya dari pola data latih", "is_correct": true }
        ],
        "answer": "B",
        "is_correct": true
      },
      {
        "id": 32,
        "type": "essay",
        "question": "Ceritakan satu pekerjaanmu yang menurutmu bisa dibantu AI, dan kenapa.",
        "order_index": 2,
        "explanation": null,
        "options": [],
        "answer": "Saya pakai AI untuk merangkum notulen rapat mingguan.",
        "is_correct": null
      }
    ],
    "pre_test_score": 40
  }
}
```

`answers_revealed` is `false` while the chapter still has an unsubmitted post-test, because the post-test usually reuses the pre-test's questions; in that state every `explanation`, option `is_correct` and question `is_correct` is `null` and only the caller's own `answer` and the overall `score` come back. A post-test result, or a pre-test in a chapter without a post-test, is always revealed. A question's `is_correct` is always `null` for essays. `pre_test_score` is the caller's pre-test score on a post-test result (`null` when there is no pre-test, it wasn't scored, or this is the pre-test itself), so the client can show the gain.

**Errors**

Same auth, `403` chapter lock and `404 Test not found`/`No access found for this project` cases as `details` above, plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Test submission not found` | the caller hasn't submitted this test |
