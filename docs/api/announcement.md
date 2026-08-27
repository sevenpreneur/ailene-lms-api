# Announcement

Endpoints for a project's single announcement banner (`lms_announcement`). Each project has at most one announcement — `project_id` is unique on the table — so `upsert` creates it on first call and updates it on every call after that.

## Endpoints

### `POST {base_url}/api/v1/announcement/details`

Returns every column of the project's `lms_announcement` row.

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
  "message": "announcement retrieved successfully",
  "data": {
    "id": 1,
    "project_id": "V7rdgcYkq9PHQZkwvoA-F",
    "title": "Libur Idul Fitri",
    "callout": "Kelas diliburkan sampai 10 April",
    "status": "active",
    "start_date": "2026-04-01T00:00:00Z",
    "end_date": "2026-04-10T23:59:59Z",
    "updated_at": "2026-03-20T08:22:22.098Z"
  }
}
```

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |
| 404 | `NOT_FOUND` | `Announcement not found` | the project has no `lms_announcement` row yet |

### `POST {base_url}/api/v1/announcement/upsert`

Creates the project's announcement if it doesn't exist yet, or replaces every field on the existing one.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "project_id": "V7rdgcYkq9PHQZkwvoA-F",
  "title": "Libur Idul Fitri",
  "callout": "Kelas diliburkan sampai 10 April",
  "status": "active",
  "start_date": "2026-04-01T00:00:00Z",
  "end_date": "2026-04-10T23:59:59Z"
}
```

| Field | Type | Required |
|---|---|---|
| `project_id` | string | yes |
| `title` | string | yes |
| `callout` | string | no |
| `status` | string (`active` / `inactive`) | yes |
| `start_date` | ISO-8601 timestamp | yes |
| `end_date` | ISO-8601 timestamp | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "announcement upserted successfully",
  "data": {
    "id": 1,
    "project_id": "V7rdgcYkq9PHQZkwvoA-F",
    "title": "Libur Idul Fitri",
    "callout": "Kelas diliburkan sampai 10 April",
    "status": "active",
    "start_date": "2026-04-01T00:00:00Z",
    "end_date": "2026-04-10T23:59:59Z",
    "updated_at": "2026-03-20T08:22:22.098Z"
  }
}
```

Every field is overwritten on each call — this isn't a partial update, so omitting `callout` clears it. `id` and `updated_at` are always server-assigned and echoed back.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
| 400 | `BAD_REQUEST` | `projectId: must not be blank` | missing/empty `project_id` field |
| 400 | `BAD_REQUEST` | `title: must not be blank` | missing/empty `title` field |
| 400 | `BAD_REQUEST` | `status: must not be null` | missing/invalid `status` field |
| 400 | `BAD_REQUEST` | `startDate: must not be null` | missing `start_date` field |
| 400 | `BAD_REQUEST` | `endDate: must not be null` | missing `end_date` field |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for `project_id` |
