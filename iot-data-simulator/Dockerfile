# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-24 AS build
WORKDIR /app

# Copy POMs for dependency caching
COPY pom.xml .
COPY shared/pom.xml shared/
COPY iot-analytics/pom.xml iot-analytics/
COPY iot-data-simulator/pom.xml iot-data-simulator/

# Resolve dependencies (offline mode safe)
# We invoke go-offline AND explicit plugin resolution to ensure Spring Boot repackage deps are present
RUN mvn dependency:go-offline dependency:resolve-plugins -pl shared,iot-data-simulator -am -fn

# Copy source and build
COPY . .
RUN mvn clean package -pl shared,iot-data-simulator -am -DskipTests

# Stage 2: Extract layers
FROM eclipse-temurin:24-jre-alpine AS layers
WORKDIR /app
COPY --from=build /app/iot-data-simulator/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Final runtime image
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
