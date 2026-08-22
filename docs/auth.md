# Auth

Google-only login: the client signs the user in with Google Identity Services and hands us the resulting ID token, which we verify directly against Google (signature + audience + `email_verified`) and exchange for our own opaque token, tracked in `lms_tokens`. There is no sign-up flow — the Google account's email must already exist as an `lms_users` row, or the call is rejected. The endpoint is additionally gated by a static bearer token (see below), since the caller has no per-user credential yet at that point.

## Endpoints

### `POST {base_url}/api/auth/login/google`

Verifies a Google ID token and logs the matching LMS user in.

**Authorization:** `Bearer <SECRET_KEY>` — a single static token shared by every legitimate client, checked against the `SECRET_KEY` env var. This isn't per-user auth (the Google ID token in the body is what identifies the user); it just keeps the endpoint from being callable by anyone who stumbles on the URL.

**Request**

```json
{
  "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6...xxx"
}
```

| Field | Type | Required |
|---|---|---|
| `id_token` | string | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "login successful",
  "data": {
    "token": "Uu1n4b7wq...xxx",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "full_name": "Akmal Luthfiansyah",
      "email": "akmal@example.com",
      "avatar": "https://lh3.googleusercontent.com/a/xxx",
      "role": "student",
      "job_title": "Software Engineer"
    }
  }
}
```

`data.token` is a random opaque string (not a JWT) — a new row is inserted into `lms_tokens` on every successful login, so a user can hold several active tokens at once; nothing currently checks this token on other endpoints. `data.user.avatar` is overwritten from Google's `picture` claim on every login, so it always reflects the Google account's current photo. `data.user.role` is one of `student`, `champion`, or `sponsor`.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Bearer token is invalid` | header present, but doesn't match `SECRET_KEY` |
| 400 | `BAD_REQUEST` | `idToken: must not be blank` | missing/empty `id_token` field — the message uses the Java field name (`idToken`), not the snake_case JSON one, since validation errors report the bean property name |
| 401 | `UNAUTHORIZED` | `Failed to verify Google ID token` | token is malformed, or Google's verification call itself failed |
| 401 | `UNAUTHORIZED` | `Invalid or unverified Google ID token` | signature/audience check failed, or the Google account's email isn't verified |
| 403 | `FORBIDDEN` | `This Google account is not registered as an LMS user` | token is valid, but no `lms_users` row matches the email |
| 500 | `INTERNAL_SERVER_ERROR` | `Unexpected error` | DB failure or anything unhandled |

Example error response (`403 Forbidden`):

```json
{
  "success": false,
  "code": 403,
  "status": "FORBIDDEN",
  "message": "This Google account is not registered as an LMS user"
}
```
