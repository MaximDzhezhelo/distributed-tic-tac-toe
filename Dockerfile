# syntax=docker/dockerfile:1
# Multi-stage build shared by all services: pass the module name as a build arg.
# Building inside the image means only Docker is needed on the host (no JDK).

FROM eclipse-temurin:25-jdk AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew :${MODULE}:bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine
ARG MODULE
WORKDIR /app
COPY --from=build /workspace/${MODULE}/build/libs/${MODULE}-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
