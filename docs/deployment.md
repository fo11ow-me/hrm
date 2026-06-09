# Server Deployment

This directory contains only non-sensitive deployment templates. Replace placeholders on the server; never commit the completed files.

## One-Time Preparation

1. Install Docker Engine, Docker Compose, Nginx, and Certbot.
2. Create a dedicated deployment account with access only to Docker and the deployment directory.
3. Create the external Docker network shared with the existing MySQL and Redis containers.
4. Copy `.env.example` to `.env` in the deployment directory, replace placeholders, and run `chmod 600 .env`.
5. Copy `nginx/hrm.conf.example` to the server Nginx configuration, replace placeholders, obtain the certificate with Certbot, validate with `nginx -t`, and reload Nginx.

Do not initialize, overwrite, or migrate production databases as part of the first deployment.

## Build and Deploy

### Build Images

```bash
# Build backend image
cd hrm-server
docker build -t hrm-server:latest .

# Build frontend image
cd ../hrm-admin
docker build -t hrm-admin:latest .
```

### Upload to Server

```bash
# Save images
docker save hrm-server:latest | gzip > hrm-server.tar.gz
docker save hrm-admin:latest | gzip > hrm-admin.tar.gz

# Upload to server
scp hrm-server.tar.gz hrm-admin.tar.gz user@server:/opt/app/hrm/

# On server, load images
docker load < hrm-server.tar.gz
docker load < hrm-admin.tar.gz
```

### Deploy

```bash
# Copy docker-compose.prod.yml to server
scp docker-compose.prod.yml user@server:/opt/app/hrm/

# On server, start services
cd /opt/app/hrm
./deploy.sh latest
```

## Verification

After deployment:

```bash
docker compose --env-file .env --env-file release.env -f docker-compose.prod.yml ps
curl --fail --silent --show-error --output /dev/null http://127.0.0.1:<BACKEND_BIND_PORT>/actuator/health
curl --fail --silent --show-error --output /dev/null http://127.0.0.1:<FRONTEND_BIND_PORT>/healthz
curl --fail --silent --show-error --output /dev/null https://<PUBLIC_DOMAIN>/healthz
```

Also verify the login page, an authenticated API request, file upload persistence after a container restart, and the file-task SSE connection.

## Rollback

If deployment fails, the script automatically restores the previous version. To manually rollback:

```bash
# Check previous tag in release.env
cat release.env

# Restore previous tag
./deploy.sh <previous-tag>
```
