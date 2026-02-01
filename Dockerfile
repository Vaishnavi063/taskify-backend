# --- Stage 1: Build the application ---
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy only dependency files first (for caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Make Maven wrapper executable and download dependencies
RUN chmod +x ./mvnw && ./mvnw dependency:resolve -B

# Now copy source code
COPY src ./src

# Build the application, skipping tests
RUN ./mvnw clean package -DskipTests

# --- Stage 2: Create the final, lightweight image ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 5555

ENTRYPOINT ["java", "-jar", "/app/app.jar"]