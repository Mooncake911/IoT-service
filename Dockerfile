# syntax=docker/dockerfile:1

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-24 AS build
WORKDIR /app
ARG SERVICE_NAME

# Copy POMs for dependency caching
COPY pom.xml .
COPY shared/pom.xml shared/
# Copy all service POMs to ensure the reactor can build the module graph
COPY iot-analytics/pom.xml iot-analytics/
COPY iot-controller/pom.xml iot-controller/
COPY iot-data-simulator/pom.xml iot-data-simulator/
COPY iot-rule-engine/pom.xml iot-rule-engine/

# Resolve dependencies with cache mount
# -pl shared,${SERVICE_NAME} ensures we only resolve what's needed for this specific service + shared
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline dependency:resolve-plugins -pl shared,${SERVICE_NAME} -am -fn

# Copy source code
COPY . .

# Copy common config to service resources before build
COPY config/application-common.yml ${SERVICE_NAME}/src/main/resources/application-common.yml

# Build with cache mount
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl shared,${SERVICE_NAME} -am -DskipTests

# Stage 2: Extract layers
FROM eclipse-temurin:24-jre-alpine AS layers
WORKDIR /app
ARG SERVICE_NAME
# Find the built JAR for the specific service
COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# Stage 3: Final runtime image
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=layers /app/extracted/dependencies/ ./
COPY --from=layers /app/extracted/spring-boot-loader/ ./
COPY --from=layers /app/extracted/snapshot-dependencies/ ./
COPY --from=layers /app/extracted/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
