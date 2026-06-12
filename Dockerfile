# ... (위의 build 단계는 그대로 유지)

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ENV TZ=Asia/Seoul
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Seoul"

# 1. 사용자 생성
RUN addgroup -S monew && adduser -S monew -G monew

# 2. 로그 폴더 생성 및 권한 부여 
RUN mkdir -p /app/.logs && chown -R monew:monew /app/.logs

# 3. JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 4. 소유권 변경 (혹시 모를 app.jar 권한까지 확실하게)
RUN chown monew:monew app.jar

USER monew:monew

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
