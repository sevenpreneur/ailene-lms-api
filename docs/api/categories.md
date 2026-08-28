# Categories

Endpoint for listing `lms_categories` — the full category list used to tag prompts and use cases (`category_ids` on `POST /api/v1/prompts/self-create`, `POST /api/v1/use-cases/self-create`, etc.). Global, not scoped per project — every project shares the same category list.

## Endpoints

### `POST {base_url}/api/v1/categories`

Returns every `lms_categories` row, sorted by name. No request body — any authenticated user (champion or student) can call it.

**Authorization:** `Bearer <jwt>` — the `data.token` from `auth/login/google`.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "categories retrieved successfully",
  "data": [
    { "id": 87, "name": "Human Capital" }
  ]
}
```

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active |
