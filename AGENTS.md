# AGENTS.md

Instructions for coding agents (Claude Code, Codex, or others) working in this repo. Read this before making changes.

## What this is

Ailene LMS backend: a Spring Boot REST API for the LMS module of a larger Neon Postgres database (the same project also hosts a separate CRM/B2B module and an account/identity service — see Database below). Deployed on Railway.

## Stack

Java 21, Spring Boot 4.1.0 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`), PostgreSQL (Neon), Flyway (`spring-boot-flyway`/`flyway-core`/`flyway-database-postgresql`, wired up but not yet used — see Database), Lombok. Maven (`./mvnw`). Module/artifact name: `lms`.

## Running locally

1. Copy `.env.example` to `.env` and fill in `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (Neon Postgres). `PORT` is optional, defaults to `8080`.
2. `./mvnw spring-boot:run` — serves on `:$PORT`.
3. No real automated test suite exists yet — `LmsApplicationTests` is just the default `contextLoads()` placeholder, and it boots the full Spring context (needs a live DB connection since `ddl-auto: validate`). Verify changes with `./mvnw compile`, `./mvnw test`, plus manual requests against a running server. There is no linter configured in `pom.xml`.

## Project structure

Package-by-layer under `com.ailene.lms.<layer>` — every feature's files are split across the layer packages, not grouped by feature:

| Layer | Owns |
|---|---|
| `controller` | `@RestController`s (`RoleController`, `AuthController`) |
| `service` | Business logic (`AuthService`, `GoogleTokenVerifier`) — controllers that don't need one call `repository` directly (e.g. `RoleController`) |
| `dto` | Request/response records, each with a `public static from(Entity)` factory where applicable |
| `repository` | `JpaRepository` interfaces |
| `entity` | `@Entity` classes |
| `enums` | Java enums backing Postgres native enum columns (e.g. `UserRole`) |
| `response` | `ApiResponse<T>` envelope, `StatusName` |
| `exception` | `GlobalExceptionHandler`, sentinel exception classes |

## Conventions — follow these exactly, they're load-bearing

- **Response envelope:** always return `ApiResponse.success(HttpStatus, message, data)` or `ApiResponse.error(HttpStatus, message)` from `response`, wrapped in `ResponseEntity` — never build a `ResponseEntity` or return a raw body directly. `StatusName.fromCode()` must stay in sync with every `HttpStatus` actually used.
- **Errors:** declare exception types under `exception` and register a handler in `GlobalExceptionHandler` (`@RestControllerAdvice`) that maps it to `ApiResponse.error(...)`. The catch-all `Exception` handler must never leak raw exception text — keep it a generic message.
- **DTOs are records with a `public static from(Entity)` factory** — see `RoleDto`/`UserDto`. Controllers return DTOs, never JPA entities directly. Mark the factory `public`: DTO and entity now live in different packages, so package-private breaks compilation.
- **Entities:** Lombok `@Getter`/`@Setter`, explicit `@Column(name = "snake_case")`, `OffsetDateTime` for `TIMESTAMPTZ` columns. Match the live column type exactly (e.g. `CHAR(21)` nanoid-style ids are `String`, not a numeric type). Postgres native enum columns use `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`, and the Java enum constants in `enums` must be spelled exactly like the Postgres labels (lowercase, e.g. `UserRole.student`) since Hibernate matches by `name()`.
- **Entity class names don't always match the table**, on purpose (mirrors `Role`→`roles`): `User`→`lms_users`, `Token`→`lms_tokens`. `User`/`Token`/`UserDto`/`UserRepository`/`TokenRepository` are the LMS's own tables — don't confuse them with `Role`/`RoleRepository`, which map to the separate account/identity service's `roles` table (see Database). `UserRole` (the `student`/`champion`/`sponsor` enum) is intentionally not called `Role`, to avoid colliding with that unrelated `Role` entity.
- **Comments:** one line, no multi-line comment blocks. If it needs more than one line, it needs a shorter explanation instead.

## Database

`docs/db/ailene-lms.sql` documents the **LMS module only** (`lms_*` tables and the trainer pipeline) of the Neon project **Ailene** (`orange-union-08059820`) — it intentionally leaves out the CRM/B2B module (`b2b_*`, `contacts`, etc.) and `users`/`roles`/`teams`/`phone_country_codes`, which are owned by a separate account/identity service and referenced only as external tables (see the notes inside that file). `docs/db/nanoid.sql` is the Postgres `nanoid()`/`nanoid_optimized()` function pair (Viascom, Apache-2.0) already deployed on that Neon project; `CHAR(21)` id columns (e.g. `trainers.id`) are nanoid-shaped but have no DB-level `DEFAULT` — the application sets them explicitly.

**There is no real migration history yet.** `spring.jpa.hibernate.ddl-auto` is `validate` (Hibernate never auto-migrates), Flyway is configured but `src/main/resources/db/migration` is empty, and the live schema was hand-authored directly against Neon (via the Neon MCP tools) and documented after the fact in `docs/db/ailene-lms.sql`. Until real Flyway migrations exist, any schema change must be made in **both** places by hand — the DDL doc and the live Neon project — and they must never drift. Since `ddl-auto` is `validate`, `./mvnw test -Dtest=LmsApplicationTests` against a real DB (with real `DB_*` env vars exported) is a fast way to catch an entity/schema mismatch without writing any data.

## Known gaps

- No automated tests beyond the Spring context-load placeholder. No CI config in this repo.
- Auth so far is just Google ID token login issuing an opaque `lms_tokens` row (`AuthController`/`AuthService`) — nothing validates that token on subsequent requests yet, so every other endpoint is still effectively open.
- Flyway is a dependency but unused in practice (see Database) — this is a real gap, not a deliberate choice.
