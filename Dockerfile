FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src src
RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system ums && useradd --system --gid ums --home-dir /app ums

COPY --from=build --chown=ums:ums /workspace/target/university-management-system-0.0.1-SNAPSHOT.jar app.jar
USER ums
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
