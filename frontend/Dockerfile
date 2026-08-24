# ---------- Etapa 1: build de Vite ----------
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
# Vacio = URLs relativas (el proxy sirve API y WebSocket en el mismo dominio)
ENV VITE_API_URL=""
RUN npm run build

# ---------- Etapa 2: nginx estatico ----------
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
