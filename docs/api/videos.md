# Videos

Endpoints for reading a single video's full detail and marking it complete. Listing the videos in a chapter lives in `docs/api/learnings.md` instead.

## Endpoints

### `POST {base_url}/api/v1/videos/details`

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

`completed`/`completed_at` come from `lms_video_completions`. `project_id` isn't part of the request — it's resolved internally from `video_id` via the video's chapter → level. Same level-unlock requirement as `docs/api/materials.md`'s `details`.

**Errors**

Same shape and cases as `POST /api/v1/learnings/task` above (missing/invalid auth, expired session — see `docs/api/learnings.md`), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Video not found` | no `lms_videos` row matches `video_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the video's project |
| 403 | `FORBIDDEN` | `This level hasn't been unlocked yet.` | the video's `level_number` is above the caller's `lms_accesses.current_level_id` level number |

### `POST {base_url}/api/v1/videos/completion`

Marks a video complete for the caller and awards its XP. Same idempotency behavior as `docs/api/materials.md`'s `completion`.

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

`completed`/`completed_at`/`xp_awarded` follow the same rules as `docs/api/materials.md`'s `completion`, including the level-unlock requirement.

**Errors**

Same shape and cases as `POST /api/v1/learnings/task` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Video not found` | no `lms_videos` row matches `video_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the video's project |
| 403 | `FORBIDDEN` | `This level hasn't been unlocked yet.` | the video's `level_number` is above the caller's `lms_accesses.current_level_id` level number |
