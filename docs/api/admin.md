# Admin

Endpoints for Ailene administrators to manage a project's groups and who can access it. `/api/v1/admin/projects` lists the projects to pick from, `/api/v1/admin/groups/*` creates, renames and deletes a project's groups (`lms_groups`), and `/api/v1/admin/users/*` invites people into a project and group, changes their role, group or profile, and removes them (`lms_accesses`, plus `lms_users` for the person).

**Authorization:** these endpoints do **not** take the LMS session from `auth/login/google`. They take the identity service's own session token, sent as `Authorization: Bearer <token>`, which is the value stored in its `tokens` table. The token has to match an active `tokens` row exactly (`is_active = true`), its `exp` claim must be in the future, and the `users` row it belongs to must have `role = 'administrator'`, `status = 'active'` and no `deleted_at`. The LMS doesn't hold the identity service's signing key, so it can't verify the signature itself. The exact match against a row the identity service wrote is the proof, and logging out there (deactivating or deleting the row) ends access here straight away. An administrator needs no `lms_users` row and no access to the project they manage.

Every endpoint below shares these errors:

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Bearer` token |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | no active `tokens` row has this exact token |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | the token's `exp` has passed, or it isn't a readable JWT |
| 403 | `FORBIDDEN` | `Only administrators can access this resource` | the token's user isn't an `administrator`, isn't `active`, or is deleted |
| 404 | `NOT_FOUND` | `Project not found` | `project_id` has no `lms_projects` row (every endpoint except `projects`) |

## Projects

### `POST {base_url}/api/v1/admin/projects`

Lists every project, newest first, with its group and member counts.

**Authorization:** `Bearer <token>` from the identity service, administrator.

**Request** — no body.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "projects retrieved successfully",
  "data": [
    { "id": "V7rdgcYkq9PHQZkwvoA-F", "name": "Hutama Karya AI Training", "company_name": "Hutama Karya", "group_count": 1, "member_count": 4 }
  ]
}
```

**Errors** — only the shared ones (never `Project not found`).

## Groups

### `POST {base_url}/api/v1/admin/groups`

Lists a project's groups, sorted by name.

**Authorization:** `Bearer <token>` from the identity service, administrator.

**Request** — `{ "project_id": "..." }`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "groups retrieved successfully",
  "data": [
    { "id": 1, "name": "Internal", "member_count": 4, "created_at": "2026-08-24T08:43:34.096184Z" }
  ]
}
```

`member_count` counts every access row in the group, whatever its role.

**Errors** — only the shared ones.

### `POST {base_url}/api/v1/admin/groups/create`

Creates a group in a project.

**Authorization:** `Bearer <token>` from the identity service, administrator.

**Request**

```json
{ "project_id": "V7rdgcYkq9PHQZkwvoA-F", "name": "Finance" }
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `name` | string, max 255 | yes |

**Response** — `201 CREATED`, with `data` shaped like one entry of the `groups` list (`member_count` is `0`).

```json
{
  "success": true,
  "code": 201,
  "status": "CREATED",
  "message": "group created successfully",
  "data": { "id": 4, "name": "Finance", "member_count": 0, "created_at": "2026-09-25T04:35:00.724702Z" }
}
```

The name is trimmed, and it must be unique within the project, ignoring case.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `name: must not be blank` | `name` missing or blank |
| 409 | `CONFLICT` | `A group with this name already exists in this project` | another group in the project has the same name, ignoring case |

### `POST {base_url}/api/v1/admin/groups/update`

Renames a group.

**Authorization:** `Bearer <token>` from the identity service, administrator.

**Request**

```json
{ "project_id": "V7rdgcYkq9PHQZkwvoA-F", "group_id": 4, "name": "Finance & Accounting" }
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `group_id` | integer | yes |
| `name` | string, max 255 | yes |

**Response** — `200 OK`, `data` is the updated group (same shape as `create`), message `group updated successfully`.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Group not found` | no group with that id in this project |
| 409 | `CONFLICT` | `A group with this name already exists in this project` | the new name clashes with another group |

### `POST {base_url}/api/v1/admin/groups/delete`

Deletes an empty group.

**Authorization:** `Bearer <token>` from the identity service, administrator.

**Request** — `{ "project_id": "...", "group_id": 4 }`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "group deleted successfully",
  "data": { "deleted": true }
}
```

Every access must belong to a group, so a group that still has members can't be deleted: move them with `users/update` or remove them first. A group with chapter sessions scheduled only for it can't be deleted either.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Group not found` | no group with that id in this project |
| 409 | `CONFLICT` | `This group still has members; move or remove them first` | an access row still points at the group |
| 409 | `CONFLICT` | `This group still has chapter sessions scheduled for it` | an `lms_chapter_sessions` row targets the group |
| 409 | `CONFLICT` | `This group is still in use and cannot be deleted` | anything else still references it |

```json
{
  "success": false,
  "code": 409,
  "status": "CONFLICT",
  "message": "This group still has members; move or remove them first",
  "data": null
}
```

## Users

### `POST {base_url}/api/v1/admin/users`

Lists everyone with access to a project, optionally only one group's members.

**Authorization:** `Bearer <token>` from the identity service, administrator.

**Request**

```json
{ "project_id": "V7rdgcYkq9PHQZkwvoA-F", "group_id": null }
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `group_id` | integer | no |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "users retrieved successfully",
  "data": [
    {
      "access_id": "-2xph_GzlulJA0SlZy1GP",
      "role": "champion",
      "group": { "id": 1, "name": "Internal" },
      "user": {
        "id": "1df9f6b8-0911-4b65-acb6-3dc797bbe8e9",
        "full_name": "Akmal Luthfiansyah",
        "email": "akmal@sevenpreneur.com",
        "avatar": "https://lh3.googleusercontent.com/...",
        "job_title": "Software Engineer",
        "last_active_at": "2026-08-31T08:19:38.949407Z"
      },
      "joined_at": "2026-08-24T04:47:44.427151Z"
    }
  ]
}
```

Sorted by group name, then full name. `access_id` identifies the person's membership in this project and is the id `users/update` and `users/delete` take. `user.id` identifies the person across projects. `role` is one of `champion`, `student` or `sponsor`. `last_active_at` is `null` until the person first signs in.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Group not found` | `group_id` is set but isn't a group in this project |

### `POST {base_url}/api/v1/admin/users/invite`

Gives an email address access to a project, in one group with one role, and emails the person a link into the LMS.

**Authorization:** `Bearer <token>` from the identity service, administrator.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "email": "rani@example.com",
  "full_name": "Rani Puspita",
  "job_title": "HR Generalist",
  "role": "student",
  "group_id": 1
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `email` | string, email, max 255 | yes |
| `full_name` | string, max 255 | only for a new person |
| `job_title` | string, max 255 | no |
| `role` | `champion` \| `student` \| `sponsor` | yes |
| `group_id` | integer | yes |

The email is trimmed and lower-cased. If no `lms_users` row has this email yet, one is created from `full_name` (required in that case) and `job_title` (an empty string when omitted). If the person already has an `lms_users` row, for example from another project, it is reused as-is, and `full_name` and `job_title` are ignored. Use `users/update` to change them. A sponsor also needs a group, since every access belongs to exactly one.

**Response** — `201 CREATED`

```json
{
  "success": true,
  "code": 201,
  "status": "CREATED",
  "message": "user invited successfully",
  "data": {
    "member": {
      "access_id": "I_HzbpnyqFX3F0YZRr2DV",
      "role": "student",
      "group": { "id": 1, "name": "Internal" },
      "user": { "id": "5f6f7e34-2662-4c17-bd80-4fe9722214d6", "full_name": "Rani Puspita", "email": "rani@example.com", "avatar": null, "job_title": "HR Generalist", "last_active_at": null },
      "joined_at": "2026-09-25T04:35:01.623907Z"
    },
    "email_sent": true,
    "access_url": "https://lms.ailene.id/V7rdgcYkq9PHQZkwvoA-F/student"
  }
}
```

`member` has the same shape as one entry of the `users` list. Once access is saved, the invitation email goes out through Mailtrap from `Sevenpreneur <no-reply@sevenpreneur.com>`, the same sender the Sevenpreneur app uses. It is written in Bahasa Indonesia, names the project, role and group, and has a "Buka Ailene LMS" button that links to `access_url`, which is `{LMS_APP_URL}/{project_id}/{role}` (`https://lms.ailene.id/...` by default). The person signs in there with Google using the invited address. Access is granted before the email is sent and is never undone because of the email: when Mailtrap isn't configured on the server (`MAILTRAP_API_TOKEN`), rejects the message, or can't be reached, the invite still succeeds with `email_sent: false`, and `access_url` is there so the link can be shared by hand. An invite that ends in `409` sends no email.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `email: must be a well-formed email address` | `email` isn't an email |
| 400 | `BAD_REQUEST` | `role: 'admin' is not one of [champion, student, sponsor]` | `role` isn't one of the three |
| 400 | `BAD_REQUEST` | `full_name is required for a user who has never been invited before` | a new email with no `full_name` |
| 404 | `NOT_FOUND` | `Group not found` | `group_id` isn't a group in this project |
| 409 | `CONFLICT` | `This user already has access to this project` | the email already has an access row here; change it with `users/update` instead |

### `POST {base_url}/api/v1/admin/users/update`

Changes a member's role, group, name or job title. Send only the fields to change.

**Authorization:** `Bearer <token>` from the identity service, administrator.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "access_id": "I_HzbpnyqFX3F0YZRr2DV",
  "role": "champion",
  "group_id": 4,
  "full_name": null,
  "job_title": "HR Lead"
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `access_id` | string | yes |
| `role` | `champion` \| `student` \| `sponsor` | no |
| `group_id` | integer | no |
| `full_name` | string, max 255 | no |
| `job_title` | string, max 255 | no |

`role` and `group_id` apply only to this project. `full_name` and `job_title` live on the person's `lms_users` row, so they change everywhere the person appears, in every project. A blank `full_name` is ignored. An empty `job_title` clears it. Moving someone to another group doesn't move their history: past submissions, notes and progress stay attached to the same access.

**Response** — `200 OK`, `data` is the updated member (same shape as the `users` list), message `user updated successfully`.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 400 | `BAD_REQUEST` | `Nothing to update` | none of the four optional fields was sent |
| 404 | `NOT_FOUND` | `Member not found` | no access with that id in this project |
| 404 | `NOT_FOUND` | `Group not found` | `group_id` isn't a group in this project |

### `POST {base_url}/api/v1/admin/users/delete`

Removes a person's access to a project.

**Authorization:** `Bearer <token>` from the identity service, administrator.

**Request** — `{ "project_id": "...", "access_id": "..." }`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "user removed from project successfully",
  "data": { "deleted": true }
}
```

Only the access row is deleted. The person's `lms_users` row stays, so they keep any other projects, and inviting them again later reuses it. Removing access never erases learning history: once anything references the access (quiz or material progress, prompt or use-case submissions, coaching notes, XP, pre-assessment, AI drafts, and so on), the delete is refused with `409`. Such a person can be moved to another group or role with `users/update`, but not removed.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Member not found` | no access with that id in this project |
| 409 | `CONFLICT` | `This member already has activity in this project (progress, submissions or notes) and cannot be removed` | another table still references the access |
