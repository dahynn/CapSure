# 1단계: Build (Node.js 환경)
FROM node:20-slim AS build-stage
# pnpm 설치를 위한 corepack 활성화
RUN corepack enable && corepack prepare pnpm@10.33.0 --activate
WORKDIR /app
COPY package.json pnpm-lock.yaml ./

# 의존성 설치(pnpm)
RUN pnpm install --frozen-lockfile
COPY . .
ARG VITE_TOSS_CLIENT_KEY=
ENV VITE_TOSS_CLIENT_KEY=$VITE_TOSS_CLIENT_KEY
RUN pnpm run build

# 2단계: Production (Nginx 환경)
FROM nginx:1.28-alpine AS execute-stage
# 빌드 결과물(dist 또는 build)을 Nginx 웹 디렉토리로 복사
COPY --from=build-stage /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Nginx 기본 포트
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1
CMD ["nginx", "-g", "daemon off;"]
