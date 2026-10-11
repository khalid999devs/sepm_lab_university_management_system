# Local Setup Report

Checked on **11 October 2026**, Asia/Dhaka.

| Component | Result |
|---|---|
| Git | Local repository on `main` with commits at implementation checkpoints |
| Java | Host Corretto 23.0.2; project targets Java 17; Docker uses Temurin 17 |
| Maven | Wrapper 3.9.12; no global Maven installation required |
| Docker | CLI/engine 28.4.0; Compose v2.39.4-desktop.1; ARM64 |
| PostgreSQL / pgAdmin | PostgreSQL 17; optional pgAdmin 9.18 |
| Ports | App 8080, PostgreSQL 15432, pgAdmin 5050; localhost bindings |

## Automated verification

- `./mvnw verify`: **37 tests, 0 failures, 0 errors, 0 skipped**, using an isolated PostgreSQL Testcontainers database and both Flyway migrations.
- Tests cover all nine role/resource pairs for all CRUD methods, anonymous access, response fields/statuses, validation, email uniqueness, CSRF, login/logout, password updates, deleted sessions, and concurrent final-admin deletion.
- `python3 scripts/smoke_test.py`: passed against the Docker app and direct Java main-class run. Checks pages, cookies, all role logins, RBAC, CRUD, and logout.
- Fresh Docker image/stack startup passed. The app runs as non-root user `ums`.
- Container recreation preserved the same account records. Switching to the direct main-class run preserved them too; demo accounts were not duplicated.
- JavaScript syntax checks and Git whitespace checks passed.

## IntelliJ and database access

The Maven layout, Java 17 bytecode target, project-root working directory, and shared Application run configuration are ready for IntelliJ. Direct `UniversityManagementApplication.main()` startup passed the full HTTP smoke test; no Spring-specific IDE integration is required.

A local PostgreSQL client connected through `localhost:15432`. The PostgreSQL driver inside the pgAdmin container connected through `db:5432`; both could inspect the project tables. pgAdmin served its login page successfully.

## Verification limits and current state

IntelliJ import/run clicks, database viewer dialogs, and visual/interactive UI clicks were not exercised: no browser session was connected. Follow [the IntelliJ guide](INTELLIJ.md), [database guide](DATABASE.md), and [lab walkthrough](DEMO.md) for those manual checks.

Temporary verification containers, volumes, and the direct Java process were removed. The normal project Compose stack is left running for local review at `http://localhost:8080`; pgAdmin is at `http://localhost:5050`. Stop it with `docker compose --profile tools down`; named volumes preserve your data.
