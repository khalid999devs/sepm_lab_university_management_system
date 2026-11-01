# Dashboard extension plan

Keep the lab's account CRUD, security, Java 17 target, and IntelliJ main-class run configuration. Add three small academic resources using the same layered design.

## What each role gets

| Role | Overview | Working screens |
|---|---|---|
| Admin | Student, teacher, course, and enrollment totals | Account directory; course CRUD and teacher assignment; enroll/withdraw students; notice CRUD; own profile |
| Teacher | Assigned courses, roster size, and teaching credits | Assigned courses and student rosters; university notices; own profile |
| Student | Enrolled courses, credits, and available courses | Course catalog; enroll/withdraw self; university notices; own profile |

Use a responsive sidebar, one accent color, real counts, searchable lists, labelled modal forms, confirmations for destructive actions, and loading/empty/error states. No frontend build tool or external assets.

## Data and rules

- `courses`: generated ID, unique uppercase code, title, credits (1–6), capacity (1–200), required teacher account.
- `enrollments`: generated ID, course, student account, enrollment timestamp; unique course/student pair.
- `notices`: generated ID, title, body, publication timestamp, author. All signed-in roles can read; only admins publish/edit/delete.
- Admins alone change courses. Teachers can read only assigned courses and their rosters. Students can read the catalog but cannot read classmates' enrollments.
- Students enroll/withdraw only themselves; admins can manage student enrollments. Teachers cannot change enrollments.
- Duplicate enrollment and full courses return `409`. Lock the course row during enrollment and capacity changes to prevent overbooking.
- Reject removing a course with enrollments or an account with academic dependencies (`409`). Explicitly withdraw/reassign first. Preserve notices if their author is deleted.
- All writes require authentication and CSRF; services enforce ownership as well as route rules. Response DTOs expose no credentials.

## REST additions

| Resource | Routes | Access |
|---|---|---|
| Courses | `GET/POST /api/courses`, `GET/PUT/DELETE /api/courses/{id}` | Scoped reads; admin writes |
| Enrollments | `GET/POST /api/enrollments`, `GET/DELETE /api/enrollments/{id}` | Admin all; teacher assigned roster reads; student own |
| Notices | `GET/POST /api/notices`, `GET/PUT/DELETE /api/notices/{id}` | Authenticated reads; admin writes |

`POST` returns `201` and `Location`; `DELETE` returns empty `204`; validation `400`, missing/private record `404`, role denial `403`, state conflict `409`. Existing account endpoints stay intact.

## Build and verification order

1. Add a versioned Flyway migration, entities, repositories, DTOs, and focused services/controllers.
2. Test real PostgreSQL constraints, role boundaries, roster privacy, course capacity, and account deletion dependencies alongside the existing regression suite.
3. Build overview, courses, directory, enrollments, notices, and profile views with role-specific navigation.
4. Add idempotent sample academic data in the demo profile; never reset existing accounts or passwords.
5. Verify JavaScript, Maven integration tests, Docker packaging, and live HTTP workflows. Run the final app through its Java main class against the same database, matching IntelliJ.
6. Update README, API/specification, and the teacher's demo guide; make natural checkpoint commits with actual timestamps.

Grades, attendance, fees, timetabling, and degree audits remain outside this extension. Credits here are a simple enrollment total, not an academic transcript.
