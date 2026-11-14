# University Management System

CSE 3230 · Home task for Lab 4

A small university portal with Admin, Teacher, and Student dashboards. It manages accounts, courses, student enrollments, and notices. The focus is backend design: REST, Spring Security, clear responsibilities, and Docker Compose.

**Implemented:** 29 resource endpoints, session login/logout, BCrypt passwords, role/ownership permissions, course capacity checks, persistent PostgreSQL data, and three simple dashboards.

| Dashboard | Main functions |
|---|---|
| Admin | Account directory, course CRUD/teacher assignment, student enrollments, notice CRUD |
| Teacher | Assigned courses, student rosters, notices, own profile |
| Student | Course catalog, enroll/withdraw self, enrolled credits, notices, own profile |

- [Dashboard plan](docs/DASHBOARD_PLAN.md) — scope, permissions, and build order.
- [Build specification](docs/SPECIFICATION.md) — problem, permissions, architecture, API, and completion criteria.
- [Local setup report](docs/SETUP.md) — Java, Git, Docker, and verified checks.
- [IntelliJ and lab reference](docs/INTELLIJ.md) — how your teacher opens/runs the project and how it follows the lab repository.
- [Database access](docs/DATABASE.md) — connect using IntelliJ, pgAdmin, or another PostgreSQL client.
- [API example](docs/API.md) — login, CSRF, and a complete teacher account workflow.
- [Lab walkthrough](docs/DEMO.md) — a short presentation and manual UI check.
- [Testing guide](docs/TESTING.md) — the unit and integration tests, commands, and viva explanation.

**Stack:** Java 25 LTS, Spring Boot 3.5.16, Maven Wrapper, PostgreSQL 17, HTML/CSS/JavaScript.

**Core rule:** admins manage all three resources. Teachers and students can view/edit only their own profile; admins create and delete accounts.

## Run

Copy `.env.example` to `.env` once. Then choose a mode:

- **IntelliJ:** open `pom.xml`, select JDK 25, stop any Docker app with `docker compose stop app`, run `docker compose up -d --wait db` in the terminal, then run the shared **University Management** configuration.
- **Docker:** run `docker compose up --build` from the project folder.

On this Mac, you can also double-click `scripts/open-intellij.command`, then click **Run** in IntelliJ.

Open **http://localhost:8080** to sign in. Health: `http://localhost:8080/actuator/health` returns `{"status":"UP"}`. Use only one application run mode at a time to avoid a port conflict.

To inspect the database, use `localhost:15432`, database `ums`, and the database credentials from `.env`. For a browser UI, run `docker compose --profile tools up -d db pgadmin` and open `http://localhost:5050`. The **University Management** server is pre-registered on a fresh pgAdmin volume; connection steps are in the [database guide](docs/DATABASE.md).

The app and pgAdmin have separate logins. With `.env.example` values:

| Page | Login email | Password |
|---|---|---|
| App at `localhost:8080` | `admin@ums.test` | `AdminPass123` |
| pgAdmin at `localhost:5050` | `admin@example.com` | `pgadmin-local-demo` |

Enter the full email address; passwords are case-sensitive. When pgAdmin asks for the PostgreSQL connection, use host `db`, port `5432`, database/user `ums`, and `DB_PASSWORD` from `.env` (example: `ums-local-demo`).

## Demo accounts

The default `demo` profile seeds these accounts, three courses, one enrollment, and one notice only when the account table is empty. Existing databases keep their accounts and academic records; use Admin to add courses if the new lists are empty. Passwords come from `.env`; the example values are shown below.

| Role | Email | Example password / variable |
|---|---|---|
| Admin | `admin@ums.test` | `AdminPass123` / `DEMO_ADMIN_PASSWORD` |
| Teacher | `teacher@ums.test` | `TeacherPass123` / `DEMO_TEACHER_PASSWORD` |
| Student | `student@ums.test` | `StudentPass123` / `DEMO_STUDENT_PASSWORD` |

Restarting preserves accounts and password changes. Changing a demo password variable does not update an existing account. An explicit non-demo Spring profile disables seeding.

## Backend design

`controller → service → repository → PostgreSQL`. Three account controllers fix the resource role and share one account service. Separate course, enrollment, and notice services own their rules and transactions. `AccountAccess` enforces ownership and admin-only writes in that service. Request/response DTOs keep credentials out of JSON. Spring Security enforces route access and CSRF; controller advice formats application errors. Components use constructor injection.

The final admin cannot be deleted, including by concurrent requests. Course-row locking prevents concurrent enrollment from overbooking the last seat. Enrolled courses and accounts with academic dependencies return `409` on deletion; withdraw/reassign first. Deleted accounts lose access on their next protected request. `PUT` requires name and email; omitting its password preserves the credential.

## Verify

Run the 15 unit tests without Docker:

```sh
./mvnw -Dtest='*UnitTest' test
```

With Docker running, run the complete suite:

```sh
./mvnw verify
```

On Windows, use `.\mvnw.cmd verify`. The 15 unit tests in `src/test/java/com/kuet/ums/unit` cover password rules, account access, password hashing, final-admin protection, course input clean-up, enrollment rules, and notices. The integration tests are in the `integration` folder beside it. The 54 integration test executions use isolated PostgreSQL containers and real Flyway migrations. They check account CRUD, role/ownership boundaries, course/notice CRUD, private rosters, enrollment and capacity, dependent deletion, validation, CSRF, sessions, and concurrent enrollment/admin deletion. See the [testing guide](docs/TESTING.md) for the three short integration tests in `SimpleIntegrationTest`.

With the demo app running, optionally check its real HTTP endpoints:

```sh
python3 scripts/smoke_test.py
```

This uses Python's standard library and removes its temporary accounts, courses, enrollments, and notices. Add `--base-url http://localhost:8081` if using a different app port. GUI clicks and IntelliJ import remain manual checks; see the [setup report](docs/SETUP.md).
