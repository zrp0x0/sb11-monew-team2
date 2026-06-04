FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S monew && adduser -S monew -G monew

COPY build/libs/*.jar app.jar

USER monew:monew

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
