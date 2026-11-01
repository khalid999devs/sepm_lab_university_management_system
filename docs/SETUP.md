# Local verification report

Checked on **1 November 2026**, Asia/Dhaka. This extends the original account-management build.

| Component | Setup |
|---|---|
| Java | Host Corretto 23.0.2; project targets Java 17; Docker uses Temurin 17 |
| Build | Maven Wrapper 3.9.12; Spring Boot 3.5.16; no frontend build step |
| Database | PostgreSQL 17; Flyway V1–V3; Hibernate schema validation |
| Docker | Engine 28.4.0; Compose with persistent named volumes |
| Local ports | App 8080, database 15432, optional pgAdmin 5050 |
| IntelliJ | Standard Maven import and shared Application configuration; project-root working directory |

## Checks performed

- `./mvnw verify`: **51 tests, 0 failures, 0 errors, 0 skipped**, using isolated PostgreSQL Testcontainers databases and real migrations.
- Account regression coverage: all role/resource pairs, own-profile restrictions, CRUD, validation, BCrypt, CSRF, sessions, deleted accounts, and concurrent final-admin deletion.
- Academic coverage: course/notice CRUD, teacher assignments and roster privacy, student ownership, duplicate/full-course registration, concurrent last-seat enrollment, capacity reduction, dependent deletion, author deletion, and strict integer validation.
- `python3 scripts/smoke_test.py`: live HTTP checks against direct Java main-class and Docker runs; temporary accounts/courses/enrollments/notices are removed afterward.
- A temporary DOM harness executed the served JavaScript against the real API: admin forms/search, teacher roster, student enrollment/withdrawal/profile, private views, safe text rendering, and server error feedback. This checks behavior, not browser layout.
- Fresh Docker build/startup: Java 17 non-root runtime, all migrations, three demo courses, one enrollment, and one notice. Container recreation preserved the demo records without duplication.
- JavaScript syntax, Python compilation, Markdown local links, and Git whitespace checks passed.

## IntelliJ and data preservation

Direct `UniversityManagementApplication.main()` startup passed the live HTTP workflow using the project database at `localhost:15432`. The shared IDE configuration uses the same main class, module classpath, and project-root working directory. No Spring-specific IDE integration or environment-file plugin is required.

The migration adds academic tables without deleting existing accounts or changing passwords. Fresh demo data is inserted only when the account table is empty. Existing databases may have empty academic lists until an admin creates courses/notices.

The previously configured pgAdmin server still uses `db:5432`; desktop database clients use `localhost:15432`. See [DATABASE.md](DATABASE.md).

## Limits and final running state

Actual browser rendering, responsive screenshots, keyboard/focus behavior, and IntelliJ import/Run-button clicks remain manual checks. Browser automation was unavailable and a native browser review attempt timed out. Follow [DEMO.md](DEMO.md) before the lab.

Temporary verification containers/volumes and the direct Java verification process were removed. The normal Compose application and database are running at `http://localhost:8080`. For IntelliJ, run `docker compose stop app`, keep/start `db`, and run **University Management**. For shutdown, `docker compose --profile tools down` preserves database and pgAdmin data.
