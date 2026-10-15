# Stage 1: build the Angular app
FROM node:20-alpine AS frontend
ENV NG_CLI_ANALYTICS=false
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build --configuration production

# Stage 2: build the Spring Boot jar with the Angular app bundled inside it
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend /build/frontend/dist/frontend/browser ./src/main/resources/static
RUN mvn -q -B -DskipTests package

# Stage 3: a small image that only runs the jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /build/backend/target/salary-management-*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
