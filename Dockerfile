FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# 1) 빌드 설정 파일 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# 2) 소스 복사 후 bootJar 생성
COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S monew && adduser -S monew -G monew

COPY --from=build /app/build/libs/*.jar app.jar

USER monew:monew

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
