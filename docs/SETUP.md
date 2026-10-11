# Local Setup Report

Checked on **11 October 2026**, Asia/Dhaka. This report verifies the local environment; the application is not implemented yet.

| Component | Result |
|---|---|
| Git | Local repository initialized on `main` |
| Java | Default: Corretto 23.0.2; OpenJDK 25 also installed; Java 21 target compilation/run passed |
| Maven | Global `mvn` absent; the planned Maven Wrapper will supply it |
| Docker CLI / engine | 28.4.0; Docker Desktop running on ARM64 |
| Docker Compose | v2.39.4-desktop.1 |
| Docker resources | 10 CPUs, approximately 7.65 GiB memory |

## Verified checks

- **Image download and execution:** `docker run --rm hello-world` exited successfully.
- **Compose:** a temporary two-service project passed configuration validation and started PostgreSQL 17.
- **Database health and networking:** a client waited for database health, connected using the Compose hostname, and executed `SELECT 1 AS docker_compose_ok;`, returning `1` with exit code `0`.
- **Cleanup:** temporary containers, network, and anonymous volume removed. Downloaded images remain cached; Docker Desktop remains running.

**Docker is working for the planned app/database setup.** Its engine was initially stopped and was started for these checks.

## Next implementation step

Generate the Spring Boot project and Maven Wrapper, targeting Java 21. The installed newer JDKs can compile this target. No global Maven or local PostgreSQL installation is needed for the planned Docker workflow.

The project `Dockerfile`, `compose.yaml`, application startup, and application tests remain to be implemented and verified. The smoke test used a temporary Compose file outside this repository.
