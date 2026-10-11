# IntelliJ IDEA and Lab Reference

## What we follow from the lab

Reviewed [faiyazsadi/WebApp](https://github.com/faiyazsadi/WebApp) at revision `fec1130`. Its [POM](https://github.com/faiyazsadi/WebApp/blob/fec1130fd7e2fce6255f7deb9e60c11d18c5a624/pom.xml) uses Java 17, Spring Boot 3.4.2, Maven, JPA, PostgreSQL, and Thymeleaf. Its Java packages separate controllers, services, repositories, entities, DTOs, and configuration.

Our project follows that Java target, Maven layout, constructor injection, and layered structure. It uses Spring Boot **3.5.16**, with the same Spring Boot 3 programming model. We will add JSON REST controllers, Spring Security, and all three roles to meet this assignment. The small frontend will call those APIs.

The normal Java `main()` method runs in IntelliJ's **Application** configuration; special Spring or Docker IDE integrations are not required. Maven supplies dependencies. This follows JetBrains' [Maven import](https://www.jetbrains.com/help/idea/import-project-or-module-wizard.html) and [Java application run](https://www.jetbrains.com/help/idea/run-debug-configuration-java-application.html) workflows.

## Run in IntelliJ

Requirements: JDK 17, IntelliJ IDEA, Docker Desktop/Engine with Compose, and internet access for the first dependency download.

1. Open the project folder or `pom.xml`; import it as a **Maven** project and let dependencies load.
2. Under **Project Structure → Project SDK**, select/download **JDK 17**. Use it for Maven importing/running too.
3. Copy `.env.example` to `.env` in the project root. On macOS/Linux: `cp .env.example .env`; in Windows PowerShell: `Copy-Item .env.example .env`.
4. In IntelliJ's terminal, run `docker compose up -d --wait db`. Only the database starts.
5. Select the shared **University Management** run configuration and click Run. Alternatively, open `src/main/java/com/kuet/ums/UniversityManagementApplication.java` and run its `main()` method.
6. Open `http://localhost:8080/actuator/health`. Expected result: `{"status":"UP"}`.

The working directory must be the project root; the shared configuration sets this. The application explicitly imports `.env` as a properties file, so no environment-file plugin is needed. Use plain `KEY=value` entries without quotes or `export`.

If the shared configuration has an unresolved module, choose `university-management-system` under **Use classpath of module**, or run `main()` to let IntelliJ create its own configuration.

## Other run modes

- **Terminal:** after starting `db`, run `./mvnw spring-boot:run`; on Windows, use `.\mvnw.cmd spring-boot:run`.
- **All Docker:** stop the IntelliJ application first, then run `docker compose up --build`.
- **Stop containers:** `docker compose down`. This preserves the database volume.

| App runs in | Database address | Same application code |
|---|---|---|
| IntelliJ / Maven | `localhost:15432` | Yes |
| Docker Compose | `db:5432` | Yes |

Ports are configurable through `.env`. If 8080 or 15432 is occupied, change `PORT` or `DB_PORT`; use the chosen app port in the browser. Keep database credentials consistent with its existing volume.

## Current boundary

The main class, Maven Wrapper, shared run configuration, initial database migration, health endpoint, and Docker packaging are present. **The management APIs, login/RBAC, demo accounts, and UI are still to be implemented.** Opening `/` currently does not show a management page.
