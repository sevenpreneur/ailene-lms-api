# Admin insights

Read-only monitoring for the OS project's **Insights** tab: pre-assessment results, guided use cases, session (chapter) reviews, pre/post-test results and quiz submissions. Everything is computed from existing LMS tables; no migration is required and nothing is written. Every endpoint takes the same body and can be narrowed to one group.

Every endpoint below requires `Authorization: Bearer <identity-service administrator token>` and shares the errors in [Admin](admin.md), plus `404 Group not found` when `group_id` is not a group of the project. All routes use POST and snake_case JSON.

**Request** (every endpoint)

```json
{ "project_id": "V7rdgcYkq9PHQZkwvoA-F", "group_id": null }
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `group_id` | integer or null | no, null reads the whole project |

A `learner` object everywhere is `{ "access_id", "role", "full_name", "email", "avatar", "group_id", "group_name" }`, and a `chapter` is `{ "id", "name", "level_id" }` (chapters are shown as sessions in the UI).

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | `project_id` missing |
| 404 | `NOT_FOUND` | `Group not found` | `group_id` is not a group of the project |

```json
{ "success": false, "code": 404, "status": "NOT_FOUND", "message": "Group not found" }
```

## POST `/api/v1/admin/insights/overview`

Returns the summary block of every section below in one call, for the tab's KPI header.

**Authorization:** identity-service administrator bearer token.

```json
{
  "success": true, "code": 200, "status": "OK", "message": "insights overview retrieved successfully",
  "data": {
    "learner_count": 7,
    "pre_assessment": { "total": 7, "submitted": 3, "scored": 2, "meets_target": 1, "average_score": 54.2, "flagged_security": 0, "flagged_overclaim": 1, "flagged_shadow_ai": 0 },
    "guided_use_cases": { "total": 2, "in_progress": 1, "awaiting_review": 1, "needs_revision": 0, "accepted": 0, "minutes_saved_per_run": 25.0, "hours_saved": 3.5, "average_helpfulness": 4.0 },
    "session_reviews": { "learner_count": 7, "responses": 4, "trainer_average": 4.4, "material_average": 4.1, "learning_value_average": 4.5, "overall_average": 4.3 },
    "chapter_tests": { "pre_submissions": 5, "post_submissions": 3, "pre_average": 52.0, "post_average": 78.3, "average_uplift": 24.0, "improved_count": 3, "paired_count": 3 },
    "quizzes": { "submissions": 6, "participants": 4, "average_score": 71.7, "average_best_score": 80.0, "perfect_scores": 1 }
  }
}
```

`learner_count` counts every non-sponsor access. `pre_assessment` is `null` when no active scoring config exists. The other summaries are documented with their own endpoint.

## POST `/api/v1/admin/insights/pre-assessment`

Pre-assessment progress and scores per member, read through the same baseline as `champion/pre-assessment-team`.

**Authorization:** identity-service administrator bearer token.

```json
{
  "success": true, "code": 200, "status": "OK", "message": "pre-assessment insights retrieved successfully",
  "data": {
    "summary": { "total": 7, "submitted": 3, "scored": 2, "meets_target": 1, "average_score": 54.2, "flagged_security": 0, "flagged_overclaim": 1, "flagged_shadow_ai": 0 },
    "target_band": { "band": 2, "name": "Practitioner", "level": 1 },
    "bands": [{ "band": 0, "name": "Unaware", "level": 0, "count": 0 }],
    "clusters": [{ "code": "C1", "name": "Foundations", "score": 61.5 }],
    "groups": [{ "group_id": 1, "group_name": "Finance", "total": 4, "scored": 2, "meets_target": 1, "average_score": 54.2 }],
    "members": [{
      "access_id": "...", "full_name": "Rina", "avatar": null, "group_id": 1, "group_name": "Finance",
      "status": "scored", "band": { "band": 2, "name": "Practitioner", "level": 1 }, "average_score": 61.0,
      "meets_target": true, "submitted_at": "2026-10-01T03:00:00Z",
      "clusters": [{ "code": "C1", "name": "Foundations", "score": 70.0 }],
      "flags": { "knowledge_score": 66.0, "skills_score": 58.0, "behaviours_score": 60.0, "security_gate_passed": true, "behaviour_gate_passed": true, "is_overclaim": false, "is_shadow_ai": false }
    }]
  }
}
```

`status` is `not_started`, `submitted` or `scored`. `flags` is `null` until the attempt is scored. `bands` lists every configured band, empty ones included.

## POST `/api/v1/admin/insights/guided-use-cases`

Every guided use case session in the project with its step progress and review state, most recently updated first.

**Authorization:** identity-service administrator bearer token.

```json
{
  "success": true, "code": 200, "status": "OK", "message": "guided use case insights retrieved successfully",
  "data": {
    "summary": { "total": 2, "in_progress": 1, "awaiting_review": 1, "needs_revision": 0, "accepted": 0, "minutes_saved_per_run": 25.0, "hours_saved": 3.5, "average_helpfulness": 4.0 },
    "funnel": [{ "status": "intake", "count": 0 }, { "status": "steps_ready", "count": 1 }, { "status": "in_progress", "count": 0 }, { "status": "ready_to_submit", "count": 0 }, { "status": "submitted", "count": 1 }],
    "sessions": [{
      "id": 3, "learner": { "access_id": "..." }, "status": "submitted", "current_step": 6, "review_status": "awaiting_review",
      "use_case_name": "Weekly report draft", "daily_task": "Compile weekly sales report", "frequency": "weekly",
      "pain_point": "Manual copy-paste", "summary": "...", "minutes_without_ai": 60.0, "estimated_minutes_with_ai": 35,
      "step_count": 5, "steps_done": 5, "hours_saved": 3.5, "helpfulness_rating": 4, "testimonial": "...", "review_comment": null,
      "submitted_at": "2026-10-10T03:00:00Z", "reviewed_at": null, "created_at": "...", "updated_at": "..."
    }]
  }
}
```

`review_status` is `in_progress` (not submitted yet), `awaiting_review`, `needs_revision` (a champion reviewed it without accepting and the learner has not resubmitted since) or `accepted`. `minutes_saved_per_run` averages `minutes_without_ai - estimated_minutes_with_ai` over sessions that have both; `hours_saved` sums what learners reported on submit.

## POST `/api/v1/admin/insights/session-reviews`

End-of-session survey results: averages per chapter and every response with its open answers, newest first.

**Authorization:** identity-service administrator bearer token.

```json
{
  "success": true, "code": 200, "status": "OK", "message": "session review insights retrieved successfully",
  "data": {
    "summary": { "learner_count": 7, "responses": 4, "trainer_average": 4.4, "material_average": 4.1, "learning_value_average": 4.5, "overall_average": 4.3 },
    "chapters": [{
      "chapter": { "id": 1, "name": "Introduction to AI", "level_id": 1 }, "responses": 4,
      "ratings": { "trainer_clarity": 4.5, "trainer_mastery": 4.3, "trainer_responsiveness": 4.5, "trainer_engagement": 4.3, "material_relevance": 4.0, "material_flow": 4.3, "material_depth": 4.0, "learning_value": 4.5 },
      "trainer_average": 4.4, "material_average": 4.1, "overall_average": 4.3
    }],
    "responses": [{
      "id": 9, "learner": { "access_id": "..." }, "chapter": { "id": 1, "name": "Introduction to AI", "level_id": 1 },
      "ratings": { "trainer_clarity": 5.0 }, "overall_average": 4.6, "missing_topics": null, "favorite_material": "Prompt patterns",
      "disliked_material": null, "improvement_suggestion": "More hands-on time", "submitted_at": "2026-10-02T09:00:00Z"
    }]
  }
}
```

Every chapter of the project is listed, with `responses: 0` and null averages when nobody has answered. Ratings are 1–5; trainer is the mean of the four trainer ratings, material of the three material ratings, overall of all eight.

## POST `/api/v1/admin/insights/chapter-tests`

Pre-test and post-test results per chapter and per learner.

**Authorization:** identity-service administrator bearer token.

```json
{
  "success": true, "code": 200, "status": "OK", "message": "chapter test insights retrieved successfully",
  "data": {
    "summary": { "pre_submissions": 5, "post_submissions": 3, "pre_average": 52.0, "post_average": 78.3, "average_uplift": 24.0, "improved_count": 3, "paired_count": 3 },
    "chapters": [{ "chapter": { "id": 1, "name": "Introduction to AI", "level_id": 1 }, "has_pre_test": true, "has_post_test": true, "pre_submissions": 5, "post_submissions": 3, "pre_average": 52.0, "post_average": 78.3, "average_uplift": 24.0 }],
    "results": [{
      "learner": { "access_id": "..." }, "chapter": { "id": 1, "name": "Introduction to AI", "level_id": 1 },
      "pre_submitted": true, "pre_score": 40, "pre_submitted_at": "...", "post_submitted": true, "post_score": 80, "post_submitted_at": "...", "uplift": 40
    }]
  }
}
```

Scores are 0–100 and `null` for an essay-only test. `uplift` is `post_score - pre_score` and only exists when both scores do; `average_uplift`, `improved_count` and `paired_count` only count those learners. `chapters` lists chapters that have an active test or any submission.

## POST `/api/v1/admin/insights/quizzes`

Completed quiz submissions, newest first, with per-quiz and overall statistics.

**Authorization:** identity-service administrator bearer token.

```json
{
  "success": true, "code": 200, "status": "OK", "message": "quiz insights retrieved successfully",
  "data": {
    "summary": { "submissions": 6, "participants": 4, "average_score": 71.7, "average_best_score": 80.0, "perfect_scores": 1 },
    "distribution": [{ "label": "0-49", "min": 0, "max": 49, "count": 0 }, { "label": "50-69", "min": 50, "max": 69, "count": 1 }, { "label": "70-84", "min": 70, "max": 84, "count": 2 }, { "label": "85-100", "min": 85, "max": 100, "count": 1 }],
    "quizzes": [{ "id": "quiz-1", "name": "AI basics quiz", "chapter": { "id": 1, "name": "Introduction to AI", "level_id": 1 }, "question_count": 5, "attempts": 6, "participants": 4, "average_score": 71.7, "average_best_score": 80.0, "highest_score": 100 }],
    "submissions": [{ "id": 12, "learner": { "access_id": "..." }, "quiz_id": "quiz-1", "quiz_name": "AI basics quiz", "chapter": { "id": 1, "name": "Introduction to AI", "level_id": 1 }, "attempt_number": 2, "score": 100, "started_at": "...", "submitted_at": "..." }]
  }
}
```

Unfinished drafts are left out. `average_best_score`, `perfect_scores` and `distribution` use each learner's best attempt per quiz, so retries do not pull them down; `average_score` averages every attempt.
