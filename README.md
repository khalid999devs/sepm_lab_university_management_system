# University Management System

CSE 3230 · Home task for Lab 4

A small web application for managing Admin, Teacher, and Student accounts. The focus is backend design: REST CRUD, Spring Security, clear responsibilities, and Docker Compose, with a minimal interface.

**Implemented:** 15 CRUD endpoints, session login/logout, BCrypt passwords, role permissions, validation, persistent storage, and sign-in/account management pages.

- [Build specification](docs/SPECIFICATION.md) — problem, permissions, architecture, API, and completion criteria.
- [Local setup report](docs/SETUP.md) — Java, Git, Docker, and verified checks.
- [IntelliJ and lab reference](docs/INTELLIJ.md) — how your teacher opens/runs the project and how it follows the lab repository.
- [Database access](docs/DATABASE.md) — connect using IntelliJ, pgAdmin, or another PostgreSQL client.
- [API example](docs/API.md) — login, CSRF, and a complete teacher account workflow.
- [Lab walkthrough](docs/DEMO.md) — a short presentation and manual UI check.

**Stack:** Java 17 target (JDK 21 works too), Spring Boot 3.5.16, Maven Wrapper, PostgreSQL 17, HTML/CSS/JavaScript.

**Core rule:** admins manage all three resources; teachers manage all teacher records; students manage all student records.

## Run

Copy `.env.example` to `.env` once. Then choose a mode:

- **IntelliJ:** open `pom.xml`, select JDK 17 or 21, run `docker compose up -d --wait db` in the terminal, then run the shared **University Management** configuration.
- **Docker:** run `docker compose up --build` from the project folder.

Open **http://localhost:8080** to sign in. Health: `http://localhost:8080/actuator/health` returns `{"status":"UP"}`. Use only one application run mode at a time to avoid a port conflict.

To inspect the database, use `localhost:15432`, database `ums`, and the database credentials from `.env`. For a browser UI, run `docker compose --profile tools up -d db pgadmin` and open `http://localhost:5050`; connection steps are in the [database guide](docs/DATABASE.md).

## Demo accounts

The default `demo` profile seeds these accounts only when the database is empty. Passwords come from `.env`; the example values are shown below.

| Role | Email | Example password / variable |
|---|---|---|
| Admin | `admin@ums.test` | `AdminPass123` / `DEMO_ADMIN_PASSWORD` |
| Teacher | `teacher@ums.test` | `TeacherPass123` / `DEMO_TEACHER_PASSWORD` |
| Student | `student@ums.test` | `StudentPass123` / `DEMO_STUDENT_PASSWORD` |

Restarting preserves accounts and password changes. Changing a demo password variable does not update an existing account. An explicit non-demo Spring profile disables seeding.

## Backend design

`controller → service → repository → PostgreSQL`. Three controllers fix the resource role; one service owns transactions and account rules. Request/response DTOs keep credentials out of JSON. Spring Security enforces role access and CSRF; controller advice formats application errors. Components use constructor injection.

The final admin cannot be deleted, including by concurrent requests. Deleted accounts lose access on their next protected request. `PUT` requires name and email; omitting its password preserves the credential.

## Verify

With Docker running:

```sh
./mvnw verify
```

On Windows, use `.\mvnw.cmd verify`. The 37 integration tests start an isolated PostgreSQL container, apply the real Flyway migrations, and check all role/resource pairs, CRUD, validation, CSRF, sessions, and concurrent admin deletion.

With the demo app running, optionally check its real HTTP endpoints:

```sh
python3 scripts/smoke_test.py
```

This uses Python's standard library and removes the accounts it creates. Add `--base-url http://localhost:8081` if using a different app port. GUI clicks and IntelliJ import remain manual checks; see the [setup report](docs/SETUP.md).
