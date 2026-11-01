# API Example

Base URL: `http://localhost:8080`. Start the app and use an admin demo login from `.env`.

This CRUD example uses an admin. Teachers/students can GET their own collection/record and PUT their own record only. Another person's record returns `404`; their POST/DELETE requests return `403`.

## 1. Get a login token

```sh
curl -c /tmp/ums-cookies http://localhost:8080/api/auth/csrf
```

Copy the response's `token`. The header name is `X-CSRF-TOKEN`.

## 2. Sign in

Replace the two placeholders with that token and `DEMO_ADMIN_PASSWORD` from `.env`:

```sh
curl -b /tmp/ums-cookies -c /tmp/ums-cookies \
  -H 'X-CSRF-TOKEN: <login-token>' \
  -d 'email=admin@ums.test' --data-urlencode 'password=<admin-password>' \
  http://localhost:8080/api/auth/login
```

Expected: `200`, JSON identity, and a session cookie. Fetch a fresh CSRF token after signing in:

```sh
curl -b /tmp/ums-cookies http://localhost:8080/api/auth/csrf
```

## 3. Manage a teacher

Use the fresh token for writes:

```sh
curl -i -b /tmp/ums-cookies \
  -H 'X-CSRF-TOKEN: <fresh-token>' -H 'Content-Type: application/json' \
  -d '{"fullName":"Lab Teacher","email":"lab.teacher@ums.test","password":"TeacherPass123"}' \
  http://localhost:8080/api/teachers
```

Expected: `201`, a `Location: /api/teachers/<id>` header, and JSON with `id`, `fullName`, `email`, and `role`. Substitute the returned ID below:

```sh
curl -b /tmp/ums-cookies 'http://localhost:8080/api/teachers/<id>'

curl -X PUT -b /tmp/ums-cookies \
  -H 'X-CSRF-TOKEN: <fresh-token>' -H 'Content-Type: application/json' \
  -d '{"fullName":"Updated Teacher","email":"lab.teacher@ums.test"}' \
  'http://localhost:8080/api/teachers/<id>'

curl -i -X DELETE -b /tmp/ums-cookies \
  -H 'X-CSRF-TOKEN: <fresh-token>' 'http://localhost:8080/api/teachers/<id>'
```

GET/PUT return `200`; DELETE returns an empty `204`. The omitted PUT password stays unchanged. Duplicate email returns `409`; a wrong collection or absent ID returns `404`.

## 4. Sign out

```sh
curl -i -X POST -b /tmp/ums-cookies \
  -H 'X-CSRF-TOKEN: <fresh-token>' http://localhost:8080/api/auth/logout
```

Expected: `204`. Protected requests with that old session then return `401`. Refresh the CSRF token before another login. The full method and permission tables are in [the specification](SPECIFICATION.md).

## Academic resources

All routes below require a session; writes also require a fresh CSRF token. Responses use the status conventions in [the specification](SPECIFICATION.md).

| Resource | Request fields | Response fields |
|---|---|---|
| Course | `code`, `title`, `credits`, `capacity`, `teacherId` | `id`, request fields, `teacherName`, `enrolledCount` |
| Enrollment | `courseId`; admin must also supply `studentId` | `id`, `courseId`, `courseCode`, `courseTitle`, `credits`, `teacherName`, `studentId`, `studentName`, `studentEmail`, `enrolledAt` |
| Notice | `title`, `body` | `id`, `title`, `body`, `publishedAt`, `authorName` |

Courses and notices support collection GET/POST and record GET/PUT/DELETE. Enrollments support collection GET/POST and record GET/DELETE; withdrawal removes an enrollment rather than updating it.

**Course creation (Admin):**

```json
{"code":"CSE 3230","title":"Web Programming Laboratory","credits":2,"capacity":40,"teacherId":2}
```

POST to `/api/courses`. Substitute an existing teacher ID. Code is trimmed/uppercased; duplicate code returns `409`. PUT uses the same complete writable fields.

**Enrollment (Student):**

```json
{"courseId":1}
```

POST to `/api/enrollments`. The server uses the signed-in student; another `studentId` returns `403`. Admins use `{"courseId":1,"studentId":3}`. Duplicate registration/full capacity returns `409`. DELETE `/api/enrollments/{id}` withdraws the student and frees the seat.

GET `/api/courses` returns the catalog for admins/students and assigned courses for teachers. GET `/api/enrollments` returns all registrations for admins, assigned rosters for teachers, and only personal registrations for students. Private record IDs return `404`.

**Notice publication (Admin):**

```json
{"title":"Course registration","body":"Browse the course catalog to register for your courses."}
```

POST to `/api/notices`. Every signed-in role can read notices, newest first; only admins can publish/edit/delete. Publication time and author are server-managed. Notice text is plain text.

Deleting a course with enrollments or an account with academic dependencies returns `409`. Withdraw enrollments/reassign courses first. A notice remains available if its author account is removed.
