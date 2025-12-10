# Multi-stage build for translation-service
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy everything (needed for Maven reactor)
COPY . .

# Build only translation-service and its dependencies
RUN mvn clean package -pl translation-ms/translation-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Add non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy the built artifact
COPY --from=build /app/translation-ms/translation-service/target/*.jar app.jar

# Expose port
EXPOSE 3003

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:3003/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
