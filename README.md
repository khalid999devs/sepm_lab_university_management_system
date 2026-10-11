# University Management System

CSE 3230 · Home task for Lab 4

A small web application for managing Admin, Teacher, and Student accounts. The focus is backend design: REST CRUD, Spring Security, clear responsibilities, and Docker Compose. The interface will be minimal.

**Current stage:** runnable Maven/PostgreSQL foundation. Account CRUD, authentication, and the UI come next.

- [Build specification](docs/SPECIFICATION.md) — problem, permissions, architecture, API, and completion criteria.
- [Local setup report](docs/SETUP.md) — Java, Git, Docker, and verified checks.
- [IntelliJ and lab reference](docs/INTELLIJ.md) — how your teacher opens/runs the project and how it follows the lab repository.
- [Database access](docs/DATABASE.md) — connect using IntelliJ, pgAdmin, or another PostgreSQL client.

**Stack:** Java 17, Spring Boot 3.5.16, Maven Wrapper, PostgreSQL, and planned HTML/CSS/JavaScript.

**Core rule:** admins manage all three resources; teachers manage all teacher records; students manage all student records.

## Run the foundation

Copy `.env.example` to `.env` once. Then choose a mode:

- **IntelliJ:** open `pom.xml`, select JDK 17, run `docker compose up -d --wait db` in the terminal, then run `UniversityManagementApplication.main()`.
- **Docker:** run `docker compose up --build` from the project folder.

Check `http://localhost:8080/actuator/health`; the foundation returns `{"status":"UP"}`. The login and management pages are not implemented yet.

To inspect the database, use `localhost:15432`, database `ums`, and the database credentials from `.env`. For a browser UI, run `docker compose --profile tools up -d db pgadmin` and open `http://localhost:5050`; connection steps are in the [database guide](docs/DATABASE.md).
