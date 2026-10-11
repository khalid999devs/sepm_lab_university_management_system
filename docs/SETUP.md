# Local Setup Report

Checked on **11 October 2026**, Asia/Dhaka. The runnable application foundation is present; management features remain to be implemented.

| Component | Result |
|---|---|
| Git | Local repository initialized on `main` |
| Java | Default: Corretto 23.0.2; OpenJDK 25 also installed; project compiles to Java 17 |
| Maven | Global `mvn` absent; bundled Wrapper downloads Maven 3.9.12; build passed |
| Docker CLI / engine | 28.4.0; Docker Desktop running on ARM64 |
| Docker Compose | v2.39.4-desktop.1 |
| Docker resources | 10 CPUs, approximately 7.65 GiB memory |

## Verified checks

- **Image download and execution:** `docker run --rm hello-world` exited successfully.
- **Compose:** a temporary two-service project passed configuration validation and started PostgreSQL 17.
- **Database health and networking:** a client waited for database health, connected using the Compose hostname, and executed `SELECT 1 AS docker_compose_ok;`, returning `1` with exit code `0`.
- **Cleanup:** temporary containers, network, and anonymous volume removed. Downloaded images remain cached; Docker Desktop remains running.

**Docker is working for the planned app/database setup.** Its engine was initially stopped and was started for these checks.

## IntelliJ compatibility

The lab reference uses Java 17 and Spring Boot 3. Our project now targets **Java 17 / Spring Boot 3.5.16**, uses Maven's standard source layout, and includes a shared IntelliJ **Application** run configuration.

- Maven packaging passed; compiled bytecode has major version 61 (Java 17).
- Launching `UniversityManagementApplication.main()` directly with Maven's runtime classpath returned HTTP `200` and `{"status":"UP"}`. This verifies the application startup path used by IntelliJ; the IntelliJ UI itself was not exercised.
- Flyway applied migration V1 successfully and created `user_accounts`.
- The project image built successfully and ran under **Temurin Java 17** as non-root user `ums`; its health endpoint returned HTTP `200` and `{"status":"UP"}`.
- Switching from the direct main-class run to Docker reused migration V1 successfully. Verification containers, network, and test volume were removed afterward.
- Port 5432 was already occupied; the project uses configurable port **15432** for the local database connection.

Follow [the IntelliJ guide](INTELLIJ.md) to open and run the project. CRUD, login/RBAC, demo accounts, the UI, and feature tests are the next implementation step.

## Database viewer checks

- Added optional pgAdmin 9.18 through Compose's `tools` profile, with persistent viewer settings and localhost port 5050.
- Fresh app/database/pgAdmin startup passed; the app returned `{"status":"UP"}` and pgAdmin served its login page over HTTP.
- A desktop PostgreSQL client authenticated through `localhost:15432` and listed the project tables.
- The PostgreSQL driver inside pgAdmin authenticated through `db:5432` and listed `user_accounts` and `flyway_schema_history`.
- GUI clicks were not exercised because no browser session was connected. The tested connection values and manual steps are in [Database access](DATABASE.md).
- Removed the isolated verification stack and its temporary volumes after checking both connection paths.

REST method semantics, status codes, request fields, creation headers, and response examples are documented in [the specification](SPECIFICATION.md). CRUD endpoints remain to be implemented and tested against that contract.
