# Admin curriculum

The OS project's **Content** tab manages chapters (shown as sessions in the UI), their group schedules, and their learning content. These endpoints use the existing LMS tables; no migration is required. A chapter has at most one material, video, quiz, pre-test, and post-test. Content belongs to the chapter and is shared by all its schedules. Dates never unlock content automatically.

Every endpoint below requires `Authorization: Bearer <identity-service administrator token>`, validates the project, and scopes the chapter to that project. All routes use POST and snake_case JSON. Mutations are transactional and hold a chapter row lock; learner write transactions take a compatible shared chapter lock through `ChapterAccessGuard`, so scoring and curriculum replacement cannot race. Deletes refuse content with participant completions or attempts; schedule/session deletes also refuse referenced survey responses. Assessment updates are refused once an attempt exists, including unfinished quiz drafts, to preserve question IDs and past results. Material/video updates retain their IDs and completion history.

## POST `/api/v1/admin/curriculum`

Returns learning levels and the project's complete editable curriculum, including inactive content and locked schedules.

**Authorization:** identity-service administrator bearer token.

```json
{ "project_id": "project-nanoid-21chars" }
```

| Field | Type | Required |
|---|---|---|
| project_id | string | yes |

```json
{
  "success": true, "code": 200, "status": "OK", "message": "curriculum retrieved successfully",
  "data": {
    "levels": [{ "id": 1, "name": "Foundation" }],
    "chapters": [{
      "id": 1, "level_id": 1, "name": "Introduction to AI", "description": "Learn the basics", "status": "active",
      "sessions": [{
        "id": 1, "only_group_id": null, "session_date": "2026-10-21T02:00:00Z", "duration_minutes": 90,
        "location_name": "Zoom", "location_url": "https://example.com/meeting", "method": "online",
        "trainer_id": null, "status": "active", "is_locked": true
      }],
      "contents": [{
        "id": "1", "kind": "pre_test", "title": "Before the session", "description": null, "status": "active",
        "content": null, "file_url": null, "image_url": null, "video_url": null, "xp_reward": 0,
        "questions": [{ "id": 1, "type": "multiple_choice", "question": "Choose the correct answer", "explanation": "Why A is correct",
          "xp_reward": 0, "options": [{ "option_code": "A", "text": "Correct", "is_correct": true }, { "option_code": "B", "text": "Incorrect", "is_correct": false }]
        }], "timestamps": [], "has_progress": false
      }]
    }]
  }
}
```

`contents[].kind` is `pre_test`, `post_test`, `quiz`, `material`, or `video`. IDs are returned as strings for every content type. Only this administrator response includes answer keys. `questions` is empty for materials/videos; `timestamps` is empty except for videos, where entries contain `start_seconds` and `title`. `has_progress` includes unfinished quiz attempts. Levels are global lookup data ordered by level number; chapters are ordered by level ID and chapter ID, schedules by date and ID, questions by their saved array order, options by code, and timestamps by start time.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 401 | UNAUTHORIZED | Session not found or already ended / Invalid or expired token | absent, expired, or inactive identity session |
| 403 | FORBIDDEN | Only administrators can access this resource | caller is not an active administrator |
| 404 | NOT_FOUND | Project not found | unknown project |

```json
{ "success": false, "code": 404, "status": "NOT_FOUND", "message": "Project not found" }
```

## POST `/api/v1/admin/curriculum/chapters/create`

Creates a curriculum session without automatically creating a schedule or content.

**Authorization:** identity-service administrator bearer token.

```json
{ "project_id": "project-nanoid-21chars", "level_id": 1, "name": "Introduction to AI", "description": "Learn the basics", "status": "active" }
```

| Field | Type | Required |
|---|---|---|
| project_id | string | yes |
| level_id | positive integer, existing global level | yes |
| name | nonblank string, max 255 | yes |
| description | string/null, max 10000 | no |
| status | active / inactive | yes |

```json
{ "success": true, "code": 201, "status": "CREATED", "message": "session created successfully", "data": { "id": 1 } }
```

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 400 | BAD_REQUEST | Unknown learning level / field validation message | invalid level or fields |
| 401/403/404 | UNAUTHORIZED/FORBIDDEN/NOT_FOUND | shared authorization/project errors above | authorization/project failure |

```json
{ "success": false, "code": 400, "status": "BAD_REQUEST", "message": "Unknown learning level" }
```

## POST `/api/v1/admin/curriculum/chapters/update`

Updates the session's name, description, level, and status without changing its content or schedules.

**Authorization:** identity-service administrator bearer token.

```json
{ "project_id": "project-nanoid-21chars", "chapter_id": 1, "level_id": 1, "name": "AI fundamentals", "description": null, "status": "active" }
```

| Field | Type | Required |
|---|---|---|
| project_id | string | yes |
| chapter_id | positive integer in project | yes |
| level_id | positive integer, existing global level | yes |
| name | nonblank string, max 255 | yes |
| description | string/null, max 10000 | no |
| status | active / inactive | yes |

```json
{ "success": true, "code": 200, "status": "OK", "message": "session updated successfully", "data": { "id": 1 } }
```

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 400 | BAD_REQUEST | chapter_id is required / Unknown learning level / field validation message | missing ID, invalid level, or fields |
| 404 | NOT_FOUND | Session not found in this project | unknown/cross-project chapter |
| 401/403/404 | UNAUTHORIZED/FORBIDDEN/NOT_FOUND | shared authorization/project errors | authorization/project failure |

```json
{ "success": false, "code": 404, "status": "NOT_FOUND", "message": "Session not found in this project" }
```

## POST `/api/v1/admin/curriculum/chapters/delete`

Deletes an unused curriculum session and its content, questions, options, video markers, and schedules atomically.

**Authorization:** identity-service administrator bearer token.

```json
{ "project_id": "project-nanoid-21chars", "chapter_id": 1 }
```

| Field | Type | Required |
|---|---|---|
| project_id | string | yes |
| chapter_id | positive integer in project | yes |

```json
{ "success": true, "code": 200, "status": "OK", "message": "session deleted successfully", "data": { "deleted": true } }
```

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 409 | CONFLICT | This content or schedule already has learner progress or survey responses and cannot be deleted | any referenced progress/survey; the whole deletion rolls back |
| 400/401/403/404 | BAD_REQUEST/UNAUTHORIZED/FORBIDDEN/NOT_FOUND | validation or shared authorization/project/session errors | invalid input/scope |

```json
{ "success": false, "code": 409, "status": "CONFLICT", "message": "This content or schedule already has learner progress or survey responses and cannot be deleted" }
```

## POST `/api/v1/admin/curriculum/schedules/create`

Schedules a chapter for all groups or one group, with explicit content access control.

**Authorization:** identity-service administrator bearer token.

```json
{ "project_id": "project-nanoid-21chars", "chapter_id": 1, "only_group_id": null, "session_date": "2026-10-21T09:00:00+07:00", "duration_minutes": 90, "location_name": "Zoom", "location_url": "https://example.com/meeting", "method": "online", "trainer_id": null, "status": "active", "is_locked": true }
```

| Field | Type | Required |
|---|---|---|
| project_id / chapter_id | string / positive integer in project | yes |
| only_group_id | positive integer in project / null (all groups) | no |
| session_date | ISO-8601 timestamp with offset | yes |
| duration_minutes | integer, 1–1440 | yes |
| location_name | nonblank string, max 255 | yes |
| location_url | absolute HTTP(S) URL / blank / null, max 2000 | no |
| method | online / offline | yes |
| trainer_id | existing external trainer's 21-character ID / null | no |
| status | active / inactive | yes |
| is_locked | boolean; UI defaults true | yes |

```json
{ "success": true, "code": 201, "status": "CREATED", "message": "schedule created successfully", "data": { "id": 1 } }
```

At most one schedule per audience is created by this API, including the all-groups audience. A group-specific schedule takes precedence. Opening an active schedule triggers the existing deduplicated chapter-unlocked notification.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 400 | BAD_REQUEST | Group does not belong to this project / location_url must be an absolute HTTP or HTTPS URL / field validation message | invalid audience, URL, or fields |
| 409 | CONFLICT | A schedule already exists for this audience; edit that schedule instead | duplicate audience |
| 409 | CONFLICT | Schedule could not be saved; check its audience and trainer | foreign-key/uniqueness race |
| 401/403/404 | UNAUTHORIZED/FORBIDDEN/NOT_FOUND | shared authorization/project/session errors | invalid scope |

```json
{ "success": false, "code": 409, "status": "CONFLICT", "message": "A schedule already exists for this audience; edit that schedule instead" }
```

## POST `/api/v1/admin/curriculum/schedules/update`

Replaces one schedule's editable fields while preserving its ID and survey references.

**Authorization:** identity-service administrator bearer token.

```json
{ "project_id": "project-nanoid-21chars", "chapter_id": 1, "session_id": 1, "only_group_id": null, "session_date": "2026-10-22T09:00:00+07:00", "duration_minutes": 120, "location_name": "Zoom", "location_url": "", "method": "online", "trainer_id": null, "status": "active", "is_locked": false }
```

| Field | Type | Required |
|---|---|---|
| session_id | positive integer in chapter/project | yes |
| all schedule-create fields | same types/limits as create | same requirements as create |

```json
{ "success": true, "code": 200, "status": "OK", "message": "schedule updated successfully", "data": { "id": 1 } }
```

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 400 | BAD_REQUEST | session_id is required / same validation errors as create | missing ID/invalid fields |
| 404 | NOT_FOUND | Schedule not found in this session | unknown/cross-chapter ID |
| 401/403/404/409 | UNAUTHORIZED/FORBIDDEN/NOT_FOUND/CONFLICT | same errors as schedule create | invalid scope or duplicate audience |

```json
{ "success": false, "code": 404, "status": "NOT_FOUND", "message": "Schedule not found in this session" }
```

## POST `/api/v1/admin/curriculum/schedules/delete`

Deletes one schedule without deleting the shared chapter or its learning content.

**Authorization:** identity-service administrator bearer token.

```json
{ "project_id": "project-nanoid-21chars", "chapter_id": 1, "session_id": 1 }
```

| Field | Type | Required |
|---|---|---|
| project_id / chapter_id / session_id | string / positive integer / positive integer | yes |

```json
{ "success": true, "code": 200, "status": "OK", "message": "schedule deleted successfully", "data": { "deleted": true } }
```

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 409 | CONFLICT | This content or schedule already has learner progress or survey responses and cannot be deleted | survey references schedule |
| 404 | NOT_FOUND | Schedule not found in this session | unknown/cross-chapter schedule |
| 400/401/403/404 | BAD_REQUEST/UNAUTHORIZED/FORBIDDEN/NOT_FOUND | validation or shared authorization/project/session errors | invalid fields/scope |

```json
{ "success": false, "code": 409, "status": "CONFLICT", "message": "This content or schedule already has learner progress or survey responses and cannot be deleted" }
```

## POST `/api/v1/admin/curriculum/contents/create`

Creates one learning content slot and atomically saves its questions/options or video chapter markers.

**Authorization:** identity-service administrator bearer token.

```json
{
  "project_id": "project-nanoid-21chars", "chapter_id": 1, "kind": "quiz", "title": "Knowledge check", "description": null,
  "status": "active", "content": null, "file_url": null, "image_url": null, "video_url": null, "xp_reward": 0,
  "questions": [{ "type": "multiple_choice", "question": "Choose the correct answer", "explanation": "A is correct", "xp_reward": 10,
    "options": [{ "option_code": "A", "text": "Correct", "is_correct": true }, { "option_code": "B", "text": "Incorrect", "is_correct": false }]
  }], "timestamps": []
}
```

| Field | Type | Required |
|---|---|---|
| project_id / chapter_id | string / positive integer | yes |
| kind | pre_test / post_test / quiz / material / video | yes |
| title | nonblank string, max 255 | yes |
| description | string/null, max 10000 | no |
| status | active / inactive | yes |
| content | material HTML string/null, max 500000 | no |
| file_url / image_url | absolute HTTP(S) URL / blank / null, max 2000 | no |
| video_url | absolute HTTP(S) URL, max 2000 | yes for video |
| xp_reward | integer, 0–32767; material/video completion XP | yes |
| questions | array, max 200; nonempty for assessments, empty for material/video | yes |
| questions[].type | multiple_choice / essay; quiz supports only multiple_choice | yes |
| questions[].question | nonblank string, max 20000 | yes |
| questions[].explanation | string/null, max 20000 | no |
| questions[].xp_reward | integer, 0–32767; used only for quiz correct answers | yes |
| questions[].options | 2–26 entries for multiple choice, exactly one correct; empty for essay | yes |
| options[].option_code | unique uppercase letter A–Z within question | yes |
| options[].text / is_correct | nonblank string max 10000 / boolean | yes |
| timestamps | array, max 200; empty unless video | yes |
| timestamps[].start_seconds / title | unique integer >= 0 / nonblank string max 255 | yes |

```json
{ "success": true, "code": 201, "status": "CREATED", "message": "content created successfully", "data": { "saved": true } }
```

Question array order becomes `order_index`. Chapter-test essays are saved for review; no automatic essay scoring is introduced. Material and video completion XP is independent of quiz per-question XP. Timestamps are returned sorted by time. This endpoint never overwrites an occupied slot.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 400 | BAD_REQUEST | Add at least one question / Each multiple-choice question needs at least two options and exactly one correct answer / Answer option codes must be unique | invalid questions |
| 400 | BAD_REQUEST | Quiz questions must be multiple choice / Essay questions cannot have answer options | unsupported question type/options |
| 400 | BAD_REQUEST | Only assessments can contain questions / Only videos can contain timestamps / Video timestamps must have unique start times | invalid content children |
| 400 | BAD_REQUEST | [field] must be an absolute HTTP or HTTPS URL / video_url is required / field validation message | invalid fields |
| 409 | CONFLICT | This session already has this content type; edit it instead | occupied slot |
| 409 | CONFLICT | Content could not be saved; refresh and try again | concurrent uniqueness/reference change |
| 401/403/404 | UNAUTHORIZED/FORBIDDEN/NOT_FOUND | shared authorization/project/session errors | invalid scope |

```json
{ "success": false, "code": 409, "status": "CONFLICT", "message": "This session already has this content type; edit it instead" }
```

## POST `/api/v1/admin/curriculum/contents/update`

Updates an existing slot in place, replacing its ordered questions/options or video markers atomically.

**Authorization:** identity-service administrator bearer token.

```json
{ "project_id": "project-nanoid-21chars", "chapter_id": 1, "kind": "material", "title": "AI fundamentals", "description": null, "status": "active", "content": "<p>Lesson text</p>", "file_url": "https://example.com/lesson.pdf", "image_url": null, "video_url": null, "xp_reward": 10, "questions": [], "timestamps": [] }
```

| Field | Type | Required |
|---|---|---|
| all content-create fields | same types/limits as create; chapter_id + kind identify existing content | same requirements as create |

```json
{ "success": true, "code": 200, "status": "OK", "message": "content updated successfully", "data": { "saved": true } }
```

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 404 | NOT_FOUND | Content not found in this session | slot does not exist |
| 409 | CONFLICT | This assessment already has attempts and can no longer be edited | any quiz/chapter-test attempt exists |
| 400/401/403/404/409 | BAD_REQUEST/UNAUTHORIZED/FORBIDDEN/NOT_FOUND/CONFLICT | same validation/scope/save errors as create | invalid fields/scope |

```json
{ "success": false, "code": 409, "status": "CONFLICT", "message": "This assessment already has attempts and can no longer be edited" }
```

## POST `/api/v1/admin/curriculum/contents/delete`

Deletes an unused content slot and its questions/options or video markers.

**Authorization:** identity-service administrator bearer token.

```json
{ "project_id": "project-nanoid-21chars", "chapter_id": 1, "kind": "quiz" }
```

| Field | Type | Required |
|---|---|---|
| project_id / chapter_id | string / positive integer | yes |
| kind | pre_test / post_test / quiz / material / video | yes |

```json
{ "success": true, "code": 200, "status": "OK", "message": "content deleted successfully", "data": { "deleted": true } }
```

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 404 | NOT_FOUND | Content not found in this session | slot does not exist |
| 409 | CONFLICT | This content or schedule already has learner progress or survey responses and cannot be deleted | completion/attempt references content |
| 400/401/403/404 | BAD_REQUEST/UNAUTHORIZED/FORBIDDEN/NOT_FOUND | validation or shared authorization/project/session errors | invalid fields/scope |

```json
{ "success": false, "code": 409, "status": "CONFLICT", "message": "This content or schedule already has learner progress or survey responses and cannot be deleted" }
```

## Local tests

`AdminCurriculumServiceTest` uses Mockito and never loads Spring or `.env`. `AdminCurriculumRepositoryTest` opts in only when `LMS_CURRICULUM_TEST_DB_URL` exactly equals `jdbc:postgresql://127.0.0.1:55437/lms_curriculum_test`, with the disposable local container's `postgres` / `local-curriculum-test` credentials. It uses baseline DDL directly, rolls each test back, and never runs Flyway or connects to Neon. Run with `./mvnw -Dtest=AdminCurriculumServiceTest,AdminCurriculumRepositoryTest test` after setting that variable. Without it, repository tests are skipped.
