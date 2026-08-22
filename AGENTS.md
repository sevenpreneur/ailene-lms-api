# AGENTS.md

Instructions for coding agents (Claude Code, Codex, or others) working in this repo. Read this before making changes.

## What this is

Ailene LMS backend: a Spring Boot REST API for the LMS module of a larger Neon Postgres database (the same project also hosts a separate CRM/B2B module and an account/identity service — see Database below). Deployed on Railway.

## Stack

Java 21, Spring Boot 4.1.0 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`), PostgreSQL (Neon), Flyway (`spring-boot-flyway`/`flyway-core`/`flyway-database-postgresql`, wired up but not yet used — see Database), Lombok. Maven (`./mvnw`). Module/artifact name: `lms`.

## Running locally

1. Copy `.env.example` to `.env` and fill in `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (Neon Postgres) and `GOOGLE_OAUTH_ID`. `PORT` is optional, defaults to `8080`.
2. `./mvnw spring-boot:run` — serves on `:$PORT`. `.env` is loaded automatically (see `LmsApplication.loadDotenv()`); no manual export needed, and it never overrides a var that's already set in the real environment.
3. No real automated test suite exists yet — `LmsApplicationTests` is just the default `contextLoads()` placeholder, and it boots the full Spring context (needs a live DB connection since `ddl-auto: validate`). `./mvnw test` also picks up `.env` automatically — see the note in Conventions about why that needed its own static block instead of just relying on `LmsApplication`'s. There is no linter configured in `pom.xml`.

## Project structure

Package-by-feature under `com.ailene.lms.<feature>`: each feature owns its own `Entity`, `EntityController`, `EntityDto`, `EntityRepository`, etc. directly in one package (no separate controller/service/repository/dto layer packages — was tried, reverted). Cross-feature code (response envelope, exception handling) lives in `com.ailene.lms.common`.

| Package | Owns |
|---|---|
| `role` | `Role` entity + read-only `/api/roles` endpoints (`RoleController`, `RoleDto`, `RoleRepository`) |
| `user` | `User`/`UserRole` entity + `UserDto`/`UserRepository` — the LMS's own user profile, no endpoints of its own yet (consumed by `auth`) |
| `auth` | Google login (`AuthController`, `AuthService`, `GoogleTokenVerifier`, `GoogleLoginRequest`, `AuthLoginResponse`) and its `Token`/`TokenRepository` |
| `common.response` | `ApiResponse<T>` envelope, `StatusName` |
| `common.exception` | `GlobalExceptionHandler`, sentinel exception classes |

## Conventions — follow these exactly, they're load-bearing

- **Response envelope:** always return `ApiResponse.success(HttpStatus, message, data)` or `ApiResponse.error(HttpStatus, message)` from `common.response`, wrapped in `ResponseEntity` — never build a `ResponseEntity` or return a raw body directly. `StatusName.fromCode()` must stay in sync with every `HttpStatus` actually used.
- **JSON is snake_case** (`spring.jackson.property-naming-strategy: SNAKE_CASE` in `application.yaml`), matching the DB's column naming — Java fields stay camelCase (`fullName`), Jackson converts both ways at the boundary, so DTOs never need `@JsonProperty`. One quirk: `@Valid` field-validation error messages (`GlobalExceptionHandler.handleValidation`) report the Java property name, not the JSON one — e.g. sending a blank `id_token` comes back as `"idToken: must not be blank"`, not `"id_token: ..."`. Don't try to "fix" that by renaming the Java field; it's a Bean Validation limitation, not a bug.
- **Errors:** declare exception types under `common.exception` and register a handler in `GlobalExceptionHandler` (`@RestControllerAdvice`) that maps it to `ApiResponse.error(...)`. The catch-all `Exception` handler must never leak raw exception text — keep it a generic message.
- **DTOs are records with a `public static from(Entity)` factory** — see `RoleDto`/`UserDto`. Controllers return DTOs, never JPA entities directly. Keep the factory `public` even though the DTO usually shares a package with its entity: some DTOs are consumed from another feature package too (`auth.AuthLoginResponse` wraps `user.UserDto`), and package-private would break that.
- **Entities:** Lombok `@Getter`/`@Setter`, explicit `@Column(name = "snake_case")`, `OffsetDateTime` for `TIMESTAMPTZ` columns. Match the live column type exactly (e.g. `CHAR(21)` nanoid-style ids are `String`, not a numeric type). Postgres native enum columns use `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`, and the Java enum constants must be spelled exactly like the Postgres labels (lowercase, e.g. `UserRole.student`) since Hibernate matches by `name()`.
- **Entity class names don't always match the table**, on purpose (mirrors `Role`→`roles`): `user.User`→`lms_users`, `auth.Token`→`lms_tokens`. Those are the LMS's own tables — don't confuse them with `role.Role`, which maps to the separate account/identity service's `roles` table (see Database). `user.UserRole` (the `student`/`champion`/`sponsor` enum) is intentionally not called `Role`, to avoid colliding with that unrelated `role.Role` entity.
- **Comments:** one line, no multi-line comment blocks. If it needs more than one line, it needs a shorter explanation instead.
- **API docs live in `docs/<area>.md`** (e.g. `docs/auth.md`), one file per feature area, not per controller class. Format: one intro paragraph, then per endpoint a one-sentence description, `**Authorization:**` line, request as a JSON block + Field/Type/Required table, response as a JSON block (no field table) with any non-obvious fields explained in a paragraph after it, and an `**Errors**` table (`Code | Status | Message | When`) plus one example error response. Update the relevant doc whenever a controller's request/response shape or error cases change.
- **`.env` loading is intentionally not SPI-based.** `me.paulschwarz:spring-dotenv` and a hand-rolled `EnvironmentPostProcessor` (both registered via `META-INF`) were tried first and silently never got invoked on this Spring Boot version — instead `LmsApplication` loads `.env` in a `static {}` block before `main()` calls `SpringApplication.run()`. That block does **not** fire for `@SpringBootTest`, because Spring only reads `LmsApplication`'s bytecode metadata there and never actually loads the class — so `LmsApplicationTests` has its own `static {}` block calling the same package-private `LmsApplication.loadDotenv()`. Any other test class that needs `.env` (e.g. a future `@SpringBootTest`) needs that same static block copied in, since it isn't inherited automatically.

## Database

`docs/db/ailene-lms.sql` documents the **LMS module only** (`lms_*` tables and the trainer pipeline) of the Neon project **Ailene** (`orange-union-08059820`) — it intentionally leaves out the CRM/B2B module (`b2b_*`, `contacts`, etc.) and `users`/`roles`/`teams`/`phone_country_codes`, which are owned by a separate account/identity service and referenced only as external tables (see the notes inside that file). `docs/db/nanoid.sql` is the Postgres `nanoid()`/`nanoid_optimized()` function pair (Viascom, Apache-2.0) already deployed on that Neon project; `CHAR(21)` id columns (e.g. `trainers.id`) are nanoid-shaped but have no DB-level `DEFAULT` — the application sets them explicitly.

**There is no real migration history yet.** `spring.jpa.hibernate.ddl-auto` is `validate` (Hibernate never auto-migrates), Flyway is configured but `src/main/resources/db/migration` is empty, and the live schema was hand-authored directly against Neon (via the Neon MCP tools) and documented after the fact in `docs/db/ailene-lms.sql`. Until real Flyway migrations exist, any schema change must be made in **both** places by hand — the DDL doc and the live Neon project — and they must never drift. Since `ddl-auto` is `validate`, `./mvnw test -Dtest=LmsApplicationTests` (with a real `.env` in place) is a fast way to catch an entity/schema mismatch without writing any data.

## Known gaps

- No automated tests beyond the Spring context-load placeholder. No CI config in this repo.
- Auth so far is just Google ID token login issuing an opaque `lms_tokens` row (`AuthController`/`AuthService`) — nothing validates that token on subsequent requests yet, so every other endpoint is still effectively open.
- Flyway is a dependency but unused in practice (see Database) — this is a real gap, not a deliberate choice.
