# Use current Java LTS for both build and runtime
FROM eclipse-temurin:21-jdk-jammy

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/spring-boot-java-refresh-0.0.1-SNAPSHOT.jar"]
