# Auth

Two ways to log in, both producing the same session JWT: with Google, or with an email and password (`login/password`, for accounts that have a `password_hash`). With Google, the client signs the user in with Google and hands us the resulting OAuth **access token** (the `ya29....` one, not the ID token/JWT), which we verify by calling Google's `userinfo` endpoint and exchange for our own JWT, tracked in `lms_tokens`. There is no sign-up flow — the Google account's email must already exist as an `lms_users` row, or the call is rejected. `login/google` is additionally gated by a static bearer token (see below), since the caller has no per-user credential yet at that point; `check-session` and `logout` are gated by that JWT instead. Every endpoint here (and everywhere else in this API) is `POST`, including the ones below that take no request body.

## Endpoints

### `POST {base_url}/api/v1/auth/login/google`

Verifies a Google access token and logs the matching LMS user in.

**Authorization:** `Bearer <SECRET_KEY>` — a single static token shared by every legitimate client, checked against the `SECRET_KEY` env var. This isn't per-user auth (the Google access token in the body is what identifies the user); it just keeps the endpoint from being callable by anyone who stumbles on the URL.

**Request**

```json
{
  "access_token": "ya29.a0AfH6SMC...xxx"
}
```

| Field | Type | Required |
|---|---|---|
| `access_token` | string | yes |

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOi...xxx",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "full_name": "Akmal Luthfiansyah",
      "email": "akmal@example.com",
      "avatar": "https://lh3.googleusercontent.com/a/xxx",
      "job_title": "Software Engineer"
    }
  }
}
```

`data.token` is a JWT (HS256, signed with `SECRET_KEY`) — claims are `sub` (the user's `id`), `email`, `jti` (a random UUID, so back-to-back logins never produce the same token), `iat`, and `exp` (1 year out). A new row is inserted into `lms_tokens` on every successful login, so a user can hold several active tokens at once — pass this same `token` as `Bearer` on `check-session`/`logout` below. `data.user.avatar` is overwritten from Google's `picture` field on every login, so it always reflects the Google account's current photo.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Bearer token is invalid` | header present, but doesn't match `SECRET_KEY` |
| 400 | `BAD_REQUEST` | `accessToken: must not be blank` | missing/empty `access_token` field — the message uses the Java field name (`accessToken`), not the snake_case JSON one, since validation errors report the bean property name |
| 401 | `UNAUTHORIZED` | `Failed to verify Google access token` | Google's `userinfo` call rejected the token (expired, revoked, malformed, wrong scopes, etc.) |
| 401 | `UNAUTHORIZED` | `Invalid or unverified Google access token` | `userinfo` succeeded, but the account's email isn't verified |
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

### `POST {base_url}/api/v1/auth/login/password`

Logs an LMS user in with their email and password, as an alternative to Google.

**Authorization:** `Bearer <SECRET_KEY>`, the same static token as `login/google`.

**Request**

```json
{
  "email": "akmal@example.com",
  "password": "kata-sandi-rahasia"
}
```

| Field | Type | Required |
|---|---|---|
| `email` | string, max 255 | yes |
| `password` | string, max 72 | yes |

**Response** — `200 OK`, exactly the same shape as `login/google`: `data.token` is the same kind of JWT, recorded in `lms_tokens`, and works on `check-session`/`logout` and every other endpoint.

The email is trimmed and matched case-insensitively against `lms_users.email`. The password is checked against `lms_users.password_hash`, a BCrypt hash; the plain password is never stored or logged. Unlike `login/google`, this endpoint doesn't touch `avatar`. A user whose `password_hash` is `null` can only sign in with Google. The only way to set a password today is the optional `password` on `admin/users/invite` (see `docs/api/admin.md`), which also emails it to the person. It never replaces an existing password: it applies to a new person, or to a registered one who has none yet. There is no endpoint yet for a user to change or reset their own password.

An unknown email, a wrong password, and an account with no password all get the same `401`, and take about the same time to answer, so the endpoint can't be used to find out which emails are registered. There is no rate limit or lockout on failed attempts yet.

**Errors**

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Bearer token is invalid` | header present, but doesn't match `SECRET_KEY` |
| 400 | `BAD_REQUEST` | `email: must not be blank` / `password: must not be blank` | a field is missing or empty |
| 400 | `BAD_REQUEST` | `password: size must be between 0 and 72` | password longer than 72 characters (BCrypt only reads 72 bytes) |
| 401 | `UNAUTHORIZED` | `Invalid email or password` | unknown email, wrong password, or the account has no password set |

```json
{
  "success": false,
  "code": 401,
  "status": "UNAUTHORIZED",
  "message": "Invalid email or password"
}
```

### `POST {base_url}/api/v1/auth/check-session`

Validates a session JWT and returns the caller's current profile — for restoring a session on page reload (e.g. showing name/avatar in a sidebar) without logging in again.

**Authorization:** `Bearer <jwt>` — the `data.token` from `login/google`.

**Request**

No request body.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "session is valid",
  "data": {
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "full_name": "Akmal Luthfiansyah",
      "email": "akmal@example.com",
      "avatar": "https://lh3.googleusercontent.com/a/xxx",
      "job_title": "Software Engineer"
    },
    "project_access": [
      {
        "id": "V7rdgcYkq9PHQZkwvoA-F",
        "name": "Hutama Karya AI Training",
        "company_name": "Hutama Karya",
        "company_slug": "hutama-karya",
        "avatar": "https://.../logo-hk.webp",
        "group_id": 12,
        "group_name": "Batch 1",
        "role": "champion",
        "has_pre_assessment": false
      }
    ]
  }
}
```

`data.user` is fetched fresh from `lms_users` on every call (not decoded from the JWT), so it reflects any profile changes made since the token was issued. `data.project_access` lists every `lms_accesses` row for this user — one entry per project they have a role on (`champion`, `student`, or `sponsor`); `company_name`, `company_slug` and `avatar` are the company fields denormalised onto `lms_projects` itself (`company_name`/`company_slug`/`company_image_url`) — the LMS no longer joins the CRM's company table, and each of the three can be `null` when the project has no company details filled in. `group_id` and `group_name` identify the user's group in that project and are always present -- `lms_accesses.group_id` is `NOT NULL`, so every access belongs to exactly one group. `has_pre_assessment` is `true` when this access already has an `lms_pre_assessments` row (see `docs/api/pre-assessment.md`) — a submitted pre-assessment is one-shot, so this is how the client knows whether to show the questionnaire or the report. An empty array means the user isn't attached to any project yet.

**Errors**

All error responses share the shape `{ "success": false, "code", "status", "message" }` (no `data`).

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Missing or invalid authorization header` | no `Authorization` header, or it doesn't start with `Bearer ` |
| 401 | `UNAUTHORIZED` | `Invalid or expired token` | bad signature, malformed JWT, or past `exp` |
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | the JWT is valid, but no matching `lms_tokens` row is active — already logged out, or the row was removed some other way |

Example error response (`401 Unauthorized`):

```json
{
  "success": false,
  "code": 401,
  "status": "UNAUTHORIZED",
  "message": "Session not found or already ended"
}
```

### `POST {base_url}/api/v1/auth/logout`

Deletes the caller's session row, so that exact JWT can never pass `check-session` again — not just discarded client-side.

**Authorization:** `Bearer <jwt>` — the `data.token` from `login/google`.

**Request**

No request body.

**Response** — `200 OK`

```json
{
  "success": true,
  "code": 200,
  "status": "OK",
  "message": "logout successful",
  "data": null
}
```

**Errors**

Same two `UNAUTHORIZED` cases as `check-session` above (missing/invalid header, invalid/expired token), plus:

| Code | Status | Message | When |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Session not found or already ended` | no `lms_tokens` row matches this token — already logged out |
