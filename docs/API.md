# API Example

Base URL: `http://localhost:8080`. Start the app and use an admin demo login from `.env`.

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
