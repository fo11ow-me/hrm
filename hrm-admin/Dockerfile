FROM node:22-alpine AS build

WORKDIR /workspace
ARG VUE_APP_BASE_API=/api
ENV VUE_APP_BASE_API=${VUE_APP_BASE_API}
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /workspace/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget --quiet --spider http://127.0.0.1:8080/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
