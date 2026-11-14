# Testing guide for the lab

The teacher asks for at least **6 unit tests** and **3 integration tests**. The tests live in
two folders under `src/test/java/com/kuet/ums/`:

```
unit/           6 files, 15 tests, no Docker needed
integration/    3 files, 54 test runs, needs Docker
```

`integration/SimpleIntegrationTest` holds the 3 short integration tests to present. The two
larger files (`AccountApiTest`, `AcademicApiTest`) cover the rest of the API; some of their
methods run several times with different roles or input values.

## What the two test types mean here

| Type | What runs | Database needed? |
|---|---|---|
| Unit test | One class or rule, with dependencies replaced by Mockito mocks | No |
| Integration test | Spring Security, controller, service, repository, Flyway, and PostgreSQL together | Yes, a temporary Testcontainers database |

The unit tests do not start the application or Docker. Integration tests use MockMvc to send
HTTP-style requests through Spring's servlet stack. MockMvc does not open a browser or a TCP port.
Testcontainers starts a separate PostgreSQL container, so these tests do not use the demo database.

## The 15 unit tests (`unit/` folder)

Open each class in IntelliJ and use the green Run icon next to a test method.

| Class | Test | What it proves |
|---|---|---|
| `PasswordValidatorUnitTest` | `acceptsAValidPassword` | An eight-character password is accepted. |
| | `rejectsPasswordsShorterThanEightCharacters` | A shorter password is rejected. |
| | `checksTheBcryptLimitInBytes` | The 72-byte limit uses UTF-8 bytes, which can differ from character count. |
| `AccountAccessUnitTest` | `adminCanOpenAnotherPersonsAccount` | An admin may access another role's account. |
| | `teacherCanOpenOwnAccountButNotAnotherTeachersAccount` | A teacher may access their own record; another teacher's ID returns 404. |
| | `studentCannotPerformAdminOnlyActions` | A student receives 403 for an admin-only action. |
| | `missingLoginIsRejected` | No authenticated principal produces 401. |
| `AccountServiceUnitTest` | `creatingAStudentHashesThePasswordAndFixesTheRole` | The service uses the encoder, saves the hash, and creates a student account. |
| | `deletingTheLastAdminFailsBeforeDeletingTheRecord` | The final admin returns 409 and the repository's delete method is never called. |
| `CourseRequestUnitTest` | `courseCodeIsTrimmedAndMadeUpperCase` | `"  cse 3230  "` becomes `"CSE 3230"`. |
| | `courseTitleIsTrimmed` | Spaces around the title are removed. |
| `EnrollmentServiceUnitTest` | `studentCannotEnrollAnotherStudent` | A student enrolling someone else gets 403 and nothing is saved. |
| | `enrollingInAFullCourseFails` | A course with all seats taken returns 409 `COURSE_FULL`. |
| `NoticeServiceUnitTest` | `adminCreatesANoticeWithTheirNameAsAuthor` | The notice is saved with a trimmed title and the admin as author. |
| | `openingAMissingNoticeGivesNotFound` | An unknown notice ID returns 404. |

The tests follow **arrange, act, assert**:

1. **Arrange:** Set up the input and any fake dependencies. For example, a Mockito repository
   returns one admin account.
2. **Act:** Call the real method being tested, such as `service.delete(Role.ADMIN, 7L)`.
3. **Assert:** Check the result or exception. For the final-admin test, also verify that
   `accounts.delete(...)` was never called.

`AccountAccessUnitTest` places a fake signed-in principal in `SecurityContextHolder`, then clears
the context after every test. `AccountServiceUnitTest` constructs the real service with mocked
repository, password encoder, and access helper. `EnrollmentServiceUnitTest` and
`NoticeServiceUnitTest` work the same way. `CourseRequestUnitTest` and `PasswordValidatorUnitTest`
need no mocks at all. No Spring context is started for any unit test.

## Three integration tests to show the teacher

All three are in `src/test/java/com/kuet/ums/integration/SimpleIntegrationTest.java`. Each one sends a
request and then checks the real database.

| Test | Request | What it proves |
|---|---|---|
| `adminCreatesAStudentAndItIsSavedInTheDatabase` | Admin sends `POST /api/students` | Response is 201, the row exists in PostgreSQL, and the password is stored as a hash. |
| `studentIsForbiddenFromCreatingATeacher` | Student sends `POST /api/teachers` | Response is 403 and no account is saved. |
| `studentEnrollsInACourseOnlyOnce` | Student sends `POST /api/enrollments` twice | First is 201, second is 409 `ALREADY_ENROLLED`, and the table holds exactly one enrollment. |

How the class is built, top to bottom:

- `@SpringBootTest` starts the whole application, so the real controllers, services,
  repositories, and security rules are used. Nothing is mocked.
- `@Testcontainers` with `@Container PostgreSQLContainer` starts a temporary PostgreSQL in Docker.
- `@DynamicPropertySource` gives Spring that database's URL, username, and password. Flyway then
  creates the tables with the real migrations.
- `@AutoConfigureMockMvc` provides `MockMvc`, which sends requests through the Spring Security
  filter chain and controllers without opening a browser or a TCP port.
- `@BeforeEach prepareData()` empties the tables and saves one admin, teacher, student, and
  course, so no test depends on another.
- `.with(user(new AccountPrincipal(admin)))` runs the request as that signed-in account, and
  `.with(csrf())` adds the CSRF token that every write request needs.

The difference from a unit test: `AccountServiceUnitTest` uses a fake repository and only checks
that `saveAndFlush` was called. The integration test reads the saved row back from PostgreSQL.

The real login flow (CSRF token, session, logout) is tested separately in
`AccountApiTest.loginRefreshesCsrfAndLogoutInvalidatesSession`.

## Run commands

With JDK 25 selected, run the unit tests without Docker:

```sh
./mvnw -Dtest='*UnitTest' test
```

Start Docker Desktop yourself before running the complete suite:

```sh
./mvnw verify
```

On this Mac, if the terminal still selects Java 23, first run:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
```

In IntelliJ, select the project JDK 25, then right-click a test class or one test method and
choose **Run**. The unit tests need no Docker engine; integration tests do.

## Short explanation for the viva

> A unit test checks one Java rule by itself. I mock its dependencies and call the real method.
> An integration test sends a request through Spring Security, the controller and service, then
> checks the real PostgreSQL result in a temporary container. We have fifteen unit tests in six files, three
> short integration tests in `SimpleIntegrationTest`, and two larger integration files.
