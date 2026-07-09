# ---------- Build stage ----------
FROM gradle:9.4.1-jdk25-alpine AS builder

WORKDIR /app

# Copy the entire project
COPY . .

# Build the executable Spring Boot JAR
RUN gradle clean bootJar --no-daemon

# ---------- Runtime stage ----------
FROM gcr.io/distroless/java25-debian13:nonroot

WORKDIR /app

# Copy the generated JAR (regardless of its name)
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]