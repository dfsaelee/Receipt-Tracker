# Build
FROM gradle:8.14.3-jdk-alpine AS build
WORKDIR /app
COPY PersonalCPI/ .
RUN gradle build -x test
# remove -x test later

# Runtime
FROM openjdk:21-jdk-slim
RUN addgroup --system turni && adduser --system --ingroup turni turni
USER turni:turni
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]