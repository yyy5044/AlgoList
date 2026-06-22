# ===== 1단계(build): Vue 정적 파일 빌드 (Node 필요) =====
FROM node:20 AS build
WORKDIR /app

# 의존성 캐싱: package 파일 먼저 복사 → 설치 (소스만 바뀌면 npm 재설치 건너뜀)
COPY package*.json ./
RUN npm ci

# 소스 복사 후 빌드 → dist/ 생성
COPY . .
RUN npm run build

# ===== 2단계(run): nginx로 정적 파일 서빙 (가벼움) =====
FROM nginx:alpine

# 1단계에서 만든 정적 파일만 가져와 nginx 서빙 폴더에 넣음
COPY --from=build /app/dist /usr/share/nginx/html

# 우리가 만든 nginx 설정으로 기본 설정을 덮어씀
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
# nginx 이미지엔 실행 명령이 이미 내장돼 있어서 ENTRYPOINT 안 적어도 됨