# syntax=docker/dockerfile:1

# ---- Stage 1: build ----
# Installs deps (cached unless the lockfile/package.json change) and builds the
# static SPA bundle.
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
# VITE_API_BASE_URL is baked in at build time; defaults to same-origin "/api".
ARG VITE_API_BASE_URL=/api
ARG VITE_BUILD_BRANCH=unknown
ARG VITE_BUILD_COMMIT=unknown
ARG VITE_BUILD_TIMESTAMP=unknown
ARG VITE_APP_VERSION=0.1.0
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_BUILD_BRANCH=$VITE_BUILD_BRANCH \
    VITE_BUILD_COMMIT=$VITE_BUILD_COMMIT \
    VITE_BUILD_TIMESTAMP=$VITE_BUILD_TIMESTAMP \
    VITE_APP_VERSION=$VITE_APP_VERSION
RUN npm run build

# ---- Stage 2: runtime ----
# Serves the static bundle with nginx and proxies /api to the backend.
FROM nginx:1.27-alpine AS runtime
ARG VITE_BUILD_COMMIT=unknown
ARG VITE_APP_VERSION=0.1.0
LABEL org.opencontainers.image.revision=$VITE_BUILD_COMMIT \
      org.opencontainers.image.version=$VITE_APP_VERSION
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
