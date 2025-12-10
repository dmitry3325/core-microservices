# Multi-stage build for communication-service
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy everything (needed for Maven reactor)
COPY . .

# Build only communication-service and its dependencies
RUN mvn clean package -pl communication-ms/communication-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Add non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy the built artifact
COPY --from=build /app/communication-ms/communication-service/target/*.jar app.jar

# Expose port
EXPOSE 3001

# Health check (longer start period due to multiple dependencies)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:3001/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
