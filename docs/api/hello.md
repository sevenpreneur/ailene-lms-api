# Hello

A single smoke-test endpoint with no business purpose — for confirming the deployed app is reachable and `SECRET_KEY` is configured correctly, e.g. from Postman.

## Endpoints

### `POST {base_url}/api/hello-world`

Returns a static greeting if the caller's bearer token matches `SECRET_KEY`.

**Authorization:** `Bearer <SECRET_KEY>` — see `docs/auth.md` for what this token is.

**Request**

No request body.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "hello world",
  "data": "Hello, World!"
}
```

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Bearer token is invalid` | header present, but doesn't match `SECRET_KEY` |

Example error response (`401 Unauthorized`):

```json
{
  "success": false,
  "code": 401,
  "status": "UNAUTHORIZED",
  "message": "Bearer token is invalid"
}
```
