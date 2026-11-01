# IntelliJ IDEA and Lab Reference

## What we follow from the lab

Reviewed [faiyazsadi/WebApp](https://github.com/faiyazsadi/WebApp) at revision `fec1130`. Its [POM](https://github.com/faiyazsadi/WebApp/blob/fec1130fd7e2fce6255f7deb9e60c11d18c5a624/pom.xml) uses Java 17, Spring Boot 3.4.2, Maven, JPA, PostgreSQL, and Thymeleaf. Its Java packages separate controllers, services, repositories, entities, DTOs, and configuration.

Our project follows its Maven layout, constructor injection, and layered structure. This project targets **Java 25 LTS** as requested; use JDK 25 in IntelliJ, Maven, and Docker. It uses Spring Boot **3.5.16**, with the same Spring Boot 3 programming model. JSON REST controllers, Spring Security, and all three roles meet this assignment; the small frontend calls those APIs.

The normal Java `main()` method runs in IntelliJ's **Application** configuration; special Spring or Docker IDE integrations are not required. Maven supplies dependencies. This follows JetBrains' [Maven import](https://www.jetbrains.com/help/idea/import-project-or-module-wizard.html) and [Java application run](https://www.jetbrains.com/help/idea/run-debug-configuration-java-application.html) workflows.

## Run in IntelliJ

Requirements: JDK 25, IntelliJ IDEA with Java 25 support, Docker Desktop/Engine with Compose, and internet access for the first dependency download. Spring Boot 3.5.16 supports Java 25, as documented in its [system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

1. Open the project folder or `pom.xml`; import it as a **Maven** project and let dependencies load.
2. Under **Project Structure → Project SDK**, select/download **JDK 25**. Use the same JDK for Maven importing/running.
3. Copy `.env.example` to `.env` in the project root. On macOS/Linux: `cp .env.example .env`; in Windows PowerShell: `Copy-Item .env.example .env`.
4. Stop any terminal Java run. In IntelliJ's terminal, run `docker compose stop app`, then `docker compose up -d --wait db`. The application port must be free; only the database is needed for the IDE run.
5. Select the shared **University Management** run configuration and click Run. Alternatively, open `src/main/java/com/kuet/ums/UniversityManagementApplication.java` and run its `main()` method.
6. Open `http://localhost:8080` and sign in using a [demo account](../README.md#demo-accounts). Health is available at `/actuator/health`.

The working directory must be the project root; the shared configuration sets this. The application explicitly imports `.env` as a properties file, so no environment-file plugin is needed. Use plain `KEY=value` entries without quotes or `export`.

If the shared configuration has an unresolved module, choose `university-management-system` under **Use classpath of module**, or run `main()` to let IntelliJ create its own configuration.

## Other run modes

- **Terminal:** after starting `db`, run `./mvnw spring-boot:run`; on Windows, use `.\mvnw.cmd spring-boot:run`.
- **All Docker:** stop the IntelliJ application first, then run `docker compose up --build`.
- **Stop containers:** `docker compose --profile tools down`. This preserves database and pgAdmin volumes.

| App runs in | Database address | Same application code |
|---|---|---|
| IntelliJ / Maven | `localhost:15432` | Yes |
| Docker Compose | `db:5432` | Yes |

Ports are configurable through `.env`. If 8080 or 15432 is occupied, change `PORT` or `DB_PORT`; use the chosen app port in the browser. Keep database credentials consistent with its existing volume.

To browse `public.user_accounts` in IntelliJ's Database window or pgAdmin, follow [Database access](DATABASE.md). The app runs independently of the chosen database viewer.

## Tests and verified run path

Right-click `src/test/java/com/kuet/ums/AccountApiTest.java` and run it, or run `./mvnw verify` in the terminal. Docker must be running; Testcontainers uses a separate temporary PostgreSQL database and does not erase your app's database.

Direct Java main-class startup and the full HTTP smoke test passed against PostgreSQL. The project and Docker runtime now use Java 25. IntelliJ's GUI import/run clicks have not been exercised here; the shared run configuration uses the verified main class and project-root working directory.

## This Mac

IntelliJ IDEA 2025.2.3 is installed. The local project selects the checksum-verified Amazon Corretto 25.0.5 SDK, registered as `corretto-25`, at `~/Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home`; Maven importer/runner inherit that project SDK. These machine settings stay in ignored `.idea` files, while the shared run configuration stays in Git.

In Finder, double-click `scripts/open-intellij.command`. It starts Docker if needed, prepares `.env` only if missing, stops the Docker app, waits for PostgreSQL, checks that the app port is free, and opens this project in IntelliJ. Then choose **University Management** and click Run. Running the script with `--check` performs the preflight without opening an IDE window.

For Maven commands in a macOS terminal that still selects an older JDK, first run `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` and `export PATH="$JAVA_HOME/bin:$PATH"`. IntelliJ's Maven importer/runner already inherit the configured project JDK.

Desktop automation could not open/control IntelliJ in this session. The local configuration is prepared; Maven import completion and the IDE Run-button action still need a manual check.
