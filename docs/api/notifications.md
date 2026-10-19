# Notifications

Endpoints for the caller's in-app notifications (`lms_notifications`). Every row belongs to one `lms_accesses` row, so a user sees a separate inbox per project, and which notifications arrive depends on that access's `role`: students hear about their schedule, assignments and reviews, champions hear about submissions waiting for their review, and sponsors receive nothing. Event notifications are written by the action that causes them (a champion assigning, reviewing or sending a coaching note, a student submitting, an admin unlocking a session). Time-based reminders are written lazily: every call to `notifications` or `notifications/unread-count` first inserts any reminder that has come due in the next 24 hours, deduplicated so each event reminds an access only once. There is no scheduler and no push channel, so a reminder only appears once the client asks; poll `unread-count` for a badge.

## Notification types

| `type` | Who gets it | Created when | `ref_id` | `event_at` |
|---|---|---|---|---|
| `chapter_reminder` | student, champion | the session that applies to the caller's group starts within 24 hours | chapter id | session start |
| `chapter_unlocked` | student, champion | an admin sets that session's `is_locked` to `false` (`admin/chapter-sessions/lock`); a group with its own session is not notified about the all-groups one | chapter id | session start |
| `prompt_assigned` | student | a champion assigns a prompt (`champion/prompts/assign`, `create-assignment`); members who already held it are skipped | prompt id | deadline |
| `use_case_assigned` | student | same, for use cases | use case id | deadline |
| `prompt_deadline` | student | an assigned prompt with a deadline is still unsubmitted within 24 hours of it | prompt id | deadline |
| `use_case_deadline` | student | same, for use cases | use case id | deadline |
| `prompt_reviewed` | student | a champion accepts or sends back a prompt submission; `title` says which | prompt id | `null` |
| `use_case_reviewed` | student | same, for use cases | use case id | `null` |
| `coaching_note` | student | a champion sends a coaching note; `body` is the first 200 characters | coaching note id | `null` |
| `prompt_submitted` | champion | a student submits, resubmits or self-creates a prompt routed to this champion | prompt submission id | `null` |
| `use_case_submitted` | champion | same, for use cases | use case submission id | `null` |

`ref_id` is always a string and is the id the matching screen already takes: `prompts/details`/`use-cases/details` for the student types, `champion/prompts/submission-details`/`champion/use-cases/submission-details` for the champion types. `title` and `body` are stored in Indonesian at write time, so editing the copy later does not change rows already written.

## Endpoints

### `POST {base_url}/api/v1/notifications`

Returns one page of the caller's notifications in a project, newest first, after writing any reminders that have come due.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "unread_only": false,
  "page": 1,
  "page_size": 20
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `unread_only` | boolean | no (default `false`) |
| `page` | integer | no (default `1`) |
| `page_size` | integer | no (default `20`, max `100`) |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "notifications retrieved successfully",
  "data": {
    "list": [
      {
        "id": 318,
        "type": "prompt_assigned",
        "title": "Tugas prompt baru dari champion",
        "body": "Ringkas laporan mingguan tim",
        "ref_id": "42",
        "event_at": "2026-10-24T17:00:00Z",
        "read_at": null,
        "created_at": "2026-10-19T08:15:02.114Z"
      },
      {
        "id": 301,
        "type": "chapter_reminder",
        "title": "Sesi chapter dimulai dalam 24 jam",
        "body": "Prompt Engineering Dasar",
        "ref_id": "7",
        "event_at": "2026-10-20T02:00:00Z",
        "read_at": "2026-10-19T08:20:41.002Z",
        "created_at": "2026-10-19T03:01:10.553Z"
      }
    ],
    "metapaging": {
      "total_data": 2,
      "total_page": 1,
      "current_page": 1,
      "page_size": 20
    },
    "unread_count": 1
  }
}
```

`read_at` is `null` while unread. `unread_count` counts every unread notification for the caller in this project, not only the ones on this page, so one call can fill both the list and the badge.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |

```json
{
  "success": false,
  "code": 404,
  "status": "NOT_FOUND",
  "message": "No access found for this project"
}
```

### `POST {base_url}/api/v1/notifications/unread-count`

Returns how many of the caller's notifications in a project are unread, after writing any reminders that have come due. Meant for polling a badge.

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
  "message": "unread count retrieved successfully",
  "data": {
    "unread_count": 3
  }
}
```

**Errors**

Same as `notifications`.

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |

```json
{
  "success": false,
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "projectId: must not be blank"
}
```

### `POST {base_url}/api/v1/notifications/read`

Marks the given notifications as read.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "notification_ids": [318, 301]
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `notification_ids` | integer[] | yes, at least one |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "notifications marked as read",
  "data": {
    "updated": 1,
    "unread_count": 0
  }
}
```

`updated` counts only rows that actually changed: ids that belong to someone else, belong to another project, do not exist, or are already read are skipped without an error, so marking the same id twice is safe. `unread_count` is the caller's count after the update.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` |
| 400 | `BAD_REQUEST` | `notificationIds: must not be empty` | missing or empty `notification_ids` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |

```json
{
  "success": false,
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "notificationIds: must not be empty"
}
```

### `POST {base_url}/api/v1/notifications/read-all`

Marks every unread notification of the caller in a project as read.

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
  "message": "all notifications marked as read",
  "data": {
    "updated": 4,
    "unread_count": 0
  }
}
```

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |

```json
{
  "success": false,
  "code": 404,
  "status": "NOT_FOUND",
  "message": "No access found for this project"
}
```
