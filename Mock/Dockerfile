# 1단계: Build (Gradle 환경)
# 주의: Mac(ARM) -> AWS(x86) 배포 시 아래 FROM 줄을 FROM --platform=linux/amd64 eclipse-temurin:21-jdk-alpine AS build-stage 로 변경해야 한다.
FROM eclipse-temurin:21-jdk-alpine AS build-stage
WORKDIR /app

# Gradle Wrapper 및 설정 파일 복사 (캐싱 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 스크립트 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 빌드 수행 (bootJar)
COPY src src
RUN ./gradlew bootJar --no-daemon

# 2단계: Production (Java 런타임 환경)
# 주의: 여기도 아키텍처 충돌 시 --platform=linux/amd64 명시가 필요하다.
FROM eclipse-temurin:21-jre-alpine AS execute-stage
WORKDIR /app

# 타임존 설정을 위한 패키지 설치 (선택 사항이나 한국 시간대 기준 운영 시 권장)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 1단계에서 빌드된 jar 파일 복사
COPY --from=build-stage /app/build/libs/*-SNAPSHOT.jar app.jar

# 백엔드 API 포트 (예시: 8000)
EXPOSE 8000

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]