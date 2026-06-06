# Server Deployment

This directory contains only non-sensitive deployment templates. Replace placeholders on the server; never commit the completed files.

## One-Time Preparation

1. Install Docker Engine, Docker Compose, Nginx, and Certbot.
2. Create a dedicated deployment account with access only to Docker and the deployment directory.
3. Create the external Docker network shared with the existing MySQL and Redis containers.
4. Copy `.env.example` to `.env` in the deployment directory, replace placeholders, and run `chmod 600 .env`.
5. Authenticate Docker to GHCR with a read-only package token supplied through standard input.
6. Copy `nginx/hrm.conf.example` to the server Nginx configuration, replace placeholders, obtain the certificate with Certbot, validate with `nginx -t`, and reload Nginx.

Do not initialize, overwrite, or migrate production databases as part of the first deployment.

## GitHub Configuration

Create GitHub Environment `dev` and add:

- `DEPLOY_HOST`
- `DEPLOY_PORT`
- `DEPLOY_USER`
- `DEPLOY_SSH_PRIVATE_KEY`
- `DEPLOY_KNOWN_HOSTS`
- `DEPLOY_HEALTHCHECK_URL`

Optionally set repository variable `DEPLOY_PATH`. The workflow defaults to `/opt/hrm`.

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

Run the manual `Rollback dev` GitHub Actions workflow and provide a previously published `sha-<commit>` image tag. The same server deployment script automatically restores the previous tag when a deployment health check fails.
