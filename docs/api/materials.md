# Materials

Endpoints for reading a single material's full detail and marking it complete. Listing the materials in a chapter or a level lives in `docs/api/learnings.md` instead.

## Endpoints

### `POST {base_url}/api/v1/materials/details`

Returns one material's full body (`content`/`file_url`/`image_url` included — this is the detail view the `POST /api/v1/learnings` list endpoint deliberately omits them from), plus the caller's own completion status.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "material_id": "430bb86dc0edc244188fd9eb"
}
```

| Field | Type | Required |
|---|---|---|
| `material_id` | string | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "material retrieved successfully",
  "data": {
    "id": "430bb86dc0edc244188fd9eb",
    "title": "Materi 1.1 — Orientasi & tetapkan target",
    "description": null,
    "content": "# F1.1 Orientasi dan Tetapkan Target\n\nSelamat datang di program adopsi AI...",
    "file_url": null,
    "image_url": null,
    "xp_reward": 30,
    "order_index": 1,
    "chapter": { "id": 5, "name": "Paham AI & pakai dengan benar" },
    "completed": false,
    "completed_at": null,
    "created_at": "2026-06-17T09:38:15.117Z",
    "updated_at": "2026-06-20T08:22:22.098Z"
  }
}
```

`completed`/`completed_at` come from the caller's `lms_material_completions` row (`false`/`null` if not completed yet). Materials don't track `xp_earned` separately (same as the list endpoint) — their `xp_reward` is granted in full on completion. `project_id` isn't part of the request — it's resolved internally from `material_id` via the material's chapter → level. The material's level must already be unlocked for the caller (see `Level not unlocked yet` below) — this applies to every access role, not students only.

**Errors**

Same shape and cases as `POST /api/v1/learnings` above (missing/invalid auth, expired session — see `docs/api/learnings.md`), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Material not found` | no `lms_materials` row matches `material_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the material's project |
| 403 | `FORBIDDEN` | `This level hasn't been unlocked yet.` | the material's `level_number` is above the caller's `lms_accesses.current_level_id` level number |

### `POST {base_url}/api/v1/materials/completion`

Marks a material complete for the caller and awards its XP. Idempotent — calling it again for an already-completed material still returns `completed: true` with the original `completed_at`, but `xp_awarded` is `0` since the XP was already granted.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Request**

```json
{
  "material_id": "430bb86dc0edc244188fd9eb"
}
```

| Field | Type | Required |
|---|---|---|
| `material_id` | string | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "material completed successfully",
  "data": {
    "material_id": "430bb86dc0edc244188fd9eb",
    "completed": true,
    "completed_at": "2026-08-26T09:38:15.117Z",
    "xp_awarded": 30
  }
}
```

`completed` is always `true` on success. `completed_at` is the timestamp of the *first* completion — replaying this call doesn't move it. `xp_awarded` is the material's `xp_reward` on first completion, `0` on every call after that (XP is granted once per `(access, material)`, backed by `lms_xp_earnings`'s unique constraint). Same level-unlock requirement as `details` above — this blocks completing (and farming XP from) a material whose level isn't open yet, not just viewing it.

**Errors**

Same shape and cases as `POST /api/v1/learnings` above (missing/invalid auth, expired session), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 404 | `NOT_FOUND` | `Material not found` | no `lms_materials` row matches `material_id` |
| 404 | `NOT_FOUND` | `No access found for this project` | the caller has no `lms_accesses` row for the material's project |
| 403 | `FORBIDDEN` | `This level hasn't been unlocked yet.` | the material's `level_number` is above the caller's `lms_accesses.current_level_id` level number |
