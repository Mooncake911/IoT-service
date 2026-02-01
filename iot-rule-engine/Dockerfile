# syntax=docker/dockerfile:1
ARG SERVICE_NAME

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-24 AS build
ARG SERVICE_NAME
WORKDIR /app

# Copy ALL poms to satisfy Maven's module check
COPY pom.xml .
COPY shared/pom.xml shared/
COPY iot-analytics/pom.xml iot-analytics/
COPY iot-controller/pom.xml iot-controller/
COPY iot-data-simulator/pom.xml iot-data-simulator/
COPY iot-rule-engine/pom.xml iot-rule-engine/

# Resolve dependencies for the specific service and its shared dependency
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline dependency:resolve-plugins -pl shared,${SERVICE_NAME} -am -fn

# Copy only necessary source code
COPY shared/src ./shared/src/
COPY ${SERVICE_NAME}/src ./${SERVICE_NAME}/src/

# Package the application
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl ${SERVICE_NAME} -am -DskipTests

# Stage 2: Extract layers
FROM eclipse-temurin:24-jre-alpine AS layers
ARG SERVICE_NAME
WORKDIR /app

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