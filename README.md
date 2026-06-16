# Monew

> 관심사 기반 뉴스 모니터링 백엔드 서비스
> 사용자의 관심사에 맞춰 외부 뉴스를 자동 수집하고, 기사에 대한 조회, 댓글, 좋아요, 알림을 제공합니다.

## 프로젝트 소개

Monew는 외부 Open API(Naver 뉴스, 한국경제 RSS)로부터 뉴스를 주기적으로 수집해, 사용자가 등록한 관심사에 맞는 기사를 제공하는 뉴스 모니터링 서비스입니다. 단순 조회 시스템을 넘어 데이터 수집(배치), 운영 모니터링, AWS 환경 배포까지 고려한 백엔드 시스템을 목표로 했습니다.

- 개발 기간: 2026.05.22 ~ 2026.06.16

## 주요 기능

- 사용자: 회원가입, 로그인, 정보 수정
- 관심사: 관심사 등록 및 구독, 키워드 기반 관리
- 뉴스 수집: Naver / 한국경제 RSS에서 관심사 기반 기사 자동 수집 (스케줄러)
- 기사: 목록/상세 조회, 사용자별 조회 이력(조회수) 기록
- 댓글/좋아요: 기사 댓글 작성, 댓글 좋아요
- 알림: 관심사 신규 기사 등 활동 알림
- 사용자 활동 내역: 구독 관심사, 최근 댓글, 최근 좋아요, 최근 조회 기사 통합 조회
- 데이터 운영: 기사 백업 배치(S3 업로드), 하드 삭제 배치, 배치 실행 메트릭/이력 모니터링

## 기술 스택

Backend
- Java 17, Spring Boot 3.5.14
- Spring Data JPA, QueryDSL 5.0, Spring Security
- Spring Batch, Spring Scheduling
- Spring Cloud OpenFeign (외부 API 연동)

Database
- PostgreSQL 16, H2 (테스트), Flyway (마이그레이션)

Infra / DevOps
- Docker, GitHub Actions, GHCR
- AWS ECS / ECR / S3 / Secrets Manager (production)
- JaCoCo (커버리지 80% 게이트)

Docs / Monitoring
- Springdoc OpenAPI (Swagger UI)
- Spring Actuator, Micrometer / Prometheus

## 인프라 및 CI/CD

```
push/PR ──> [CI Pipeline]
              빌드, 테스트, JaCoCo(80%), Flyway 검증, Docker 빌드, 헬스체크
                        │ (성공 시 GHCR push)
          ┌─────────────┴─────────────┐
  develop │                           │ main
          ▼                           ▼
   [CD Staging]                 [CD Production]
   GHCR pull → 원격 서버         OIDC 인증 → ECR push
   docker compose 교체 배포      → ECS task 배포 → 헬스체크
```

- develop 브랜치는 staging, main 브랜치는 production(AWS ECS)으로 분리 배포
- 동일 Docker 이미지를 검증과 배포에 함께 사용해 산출물 일관성 확보
- 민감 정보는 AWS Secrets Manager로 분리 관리

## 프로젝트 구조

```
src/main/java/com/codeit/monew
├── domain
│   ├── user / userActivity        # 사용자, 활동 내역
│   ├── article / articleView      # 기사, 조회 이력
│   ├── comment / commentLike      # 댓글, 좋아요
│   ├── interest / subscription    # 관심사, 구독
│   └── notification               # 알림
├── batch
│   ├── collector                  # 뉴스 수집
│   ├── backup / delete / restore  # 백업, 삭제, 복구 배치
│   └── ...
└── global                         # 공통 설정, 필터, 예외, 모니터링
```

## 실행 방법

1) 로컬 DB 기동
```bash
docker compose up -d postgres-dev
```

2) 애플리케이션 실행 (dev 프로필)
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

3) 빌드 및 테스트
```bash
./gradlew test jacocoTestCoverageVerification bootJar
```

4) API 문서
```
http://localhost:8080/swagger-ui/index.html
```

## 주요 환경 변수

| 변수 | 설명 |
| --- | --- |
| SPRING_DATASOURCE_URL / USERNAME / PASSWORD | DB 접속 정보 |
| SPRING_PROFILES_ACTIVE | 실행 프로필 (dev / staging / prod) |
| NAVER_CLIENT_ID / NAVER_CLIENT_SECRET | 네이버 뉴스 API 인증 |
| AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_S3_BUCKET_NAME | S3 백업 |

## 브랜치 전략

- `main`: 운영 배포 (production)
- `develop`: 통합 및 staging 배포
- `feature/{이슈번호}-{설명}`: 기능 개발 후 develop으로 PR
