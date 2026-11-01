# Lab walkthrough

## Start in IntelliJ

1. Open `pom.xml` as a Maven project; select JDK 25.
2. Copy `.env.example` to `.env` only if `.env` does not exist.
3. In the IDE terminal: `docker compose stop app`, then `docker compose up -d --wait db`.
4. Run the shared **University Management** configuration, or the application's `main()` method.
5. Open `http://localhost:8080`. Use [the demo logins](../README.md#demo-accounts).

A fresh demo database has three courses, one enrollment, and a notice. Existing databases retain their data; create a course and notice through Admin if those lists are empty.

## Present the three dashboards

| Role | Short demonstration |
|---|---|
| Admin | Show real overview counts. In People, search/add/edit an account. In Courses, add a course with a teacher, credits, and capacity. Enroll a student; post a notice. |
| Teacher | See only assigned courses. Use View roster to see the enrolled student's name/email. Show notices and edit own profile. No account-directory or enrollment-write controls appear. |
| Student | Browse/search the catalog, enroll, and show updated credits in Overview. Open My courses and withdraw. Show notices and own profile. Other students' registrations remain private. |

Show a conflict: duplicate course code, duplicate enrollment, full course, or deleting a course with students. Show a direct API response with no credential fields; anonymous requests return `401`, forbidden actions `403`, private IDs `404`.

Open `user_accounts`, `courses`, `enrollments`, and `notices` in IntelliJ or pgAdmin. Explain the foreign keys and Flyway V3 migration. Restart the app to show persistence. Run `./mvnw verify` with Docker running.

## Explain the design

| Open | Explain |
|---|---|
| `controller/CourseController.java` | REST methods, validated DTO, `201` and `Location`, empty `204` |
| `service/CourseService.java` | Course rules, teacher assignment, short transactions |
| `service/EnrollmentService.java` | Student ownership, teacher roster scope, capacity lock |
| `repository/` | Spring Data queries, entity graphs, aggregate counts |
| `entity/` and `dto/` | Database model separated from public JSON |
| `service/AccountService.java` | Shared account rules and final-admin protection |
| `security/AccountAccess.java`, `config/SecurityConfig.java` | Service authorization plus route RBAC, BCrypt, session/CSRF protection |
| `db/migration/V3__add_academic_resources.sql` | Versioned schema, unique/FK/check constraints, indexes |
| `static/js/management.js`, `static/js/ui.js` | API-driven views and reusable DOM/dialog components |
| `Dockerfile`, `compose.yaml`, `.run/` | Java 25 container, persistent database, standard IntelliJ main-class run |

The patterns are layered architecture, Repository, DTO, constructor injection, and central exception handling. Keep the explanation tied to these concrete responsibilities.

## Final manual check

Automated checks cover API behavior and the served JavaScript's DOM flows. Before presenting, review the actual browser at desktop/phone widths, keyboard focus, dialog Escape/cancel, form messages, and sign-out. Verify IntelliJ's Maven import/Run button and your database viewer's connection dialog. Browser rendering and IDE clicks were not automated in this environment; see [SETUP.md](SETUP.md).
