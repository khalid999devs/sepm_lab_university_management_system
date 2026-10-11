# Lab Walkthrough

## Show the behavior

1. Open the Maven project in IntelliJ using [the run guide](INTELLIJ.md), then open `http://localhost:8080`.
2. Sign in as Admin. Show all three tabs; add, edit, and delete a temporary teacher. Try a duplicate email and show its error.
3. Sign out. Sign in as Teacher, then Student. Each sees a personal dashboard and can edit only their own profile. Neither can create/delete accounts or access another person's record.
4. Show an API response with no password fields. A teacher's direct request to `/api/students` returns `403`; an unsigned request returns `401`.
5. Browse `public.user_accounts` in IntelliJ or pgAdmin. UI changes appear in PostgreSQL immediately.
6. Restart the application and show that records remain. Show `./mvnw verify` passing with Docker running.

## Explain the design

| Open this file | Explain |
|---|---|
| `controller/TeacherController.java` | HTTP methods, JSON DTOs, resource `Location`, fixed teacher role |
| `service/AccountService.java` | Shared rules, transactions, scoped lookups, final-admin lock |
| `repository/AccountRepository.java` | Spring Data repository and role/email queries |
| `entity/UserAccount.java` and `dto/` | Database model separated from public request/response data |
| `config/SecurityConfig.java` | Session authentication, BCrypt, RBAC, CSRF, logout |
| `security/AccountAccess.java` | Ownership checks and admin-only creation/deletion inside the shared service |
| `security/ActiveAccountFilter.java` | Deleted accounts lose access using their stable account ID |
| `db/migration/` | Flyway schema versioning, constraints, role/ID index |
| `Dockerfile` and `compose.yaml` | Separate app/database containers and persistent volumes |

These are layered architecture, Repository, DTO, dependency injection, and central error handling. Three small controllers reuse the same service rather than duplicating business rules.

## Manual UI check

The automated checks cover backend behavior and page serving. Before the lab, click through login, admin tabs and CRUD, personal dashboards and profile editing, cancel/Escape in dialogs, field errors, sign-out, and a narrow phone-size window. Also verify IntelliJ's Maven import and your chosen database viewer's connection dialog. These GUI clicks were not automated in this environment.
