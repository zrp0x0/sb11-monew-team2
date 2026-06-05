FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew bootJar --no-daemon && \
    JAR_FILE="$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1)" && \
    cp "$JAR_FILE" app.jar

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S monew && adduser -S monew -G monew

COPY --from=build /workspace/app.jar app.jar

USER monew:monew

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
