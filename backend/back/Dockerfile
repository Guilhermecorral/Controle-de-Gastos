#                Stage 1: Build the application (Compile Maven project)

# Use a maven image with java 25 to build the application
FROM maven:3.9.14-eclipse-temurin-25-alpine AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml
COPY pom.xml .

# Copy the source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

#                Stage 2: Final image to run the application

# Usage Java 25 slim
FROM eclipse-temurin:25-jdk-alpine

# Set final working directory
WORKDIR /app

# Copy .jar file from the build stage
# The * to catch the generated jar file
COPY --from=build /app/target/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]