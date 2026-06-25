# syntax=docker/dockerfile:1

# ---- Build stage: produce static assets ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install

COPY . .
RUN npm run build

# ---- Run stage: serve via Nginx, proxy /api to backend ----
FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
  CMD ["sh", "-c", "wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1"]
