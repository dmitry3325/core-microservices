# Multi-stage build for document-service
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy everything (needed for Maven reactor)
COPY . .

# Build only document-service and its dependencies
RUN mvn clean package -pl document-ms/document-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built artifact
COPY --from=build /app/document-ms/document-service/target/*.jar app.jar

# Expose port
EXPOSE 3002

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3002/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
