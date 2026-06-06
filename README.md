# HRM

HRM is a Spring Boot and Vue 2 human-resources management application.

## Project Layout

- `hrm-server`: Spring Boot API
- `hrm-admin`: Vue administration UI
- `deploy`: production Compose, deployment scripts, and Nginx template
- `.github/workflows`: CI/CD workflows for the `dev` branch

## Local Development

Create local configuration from the checked-in examples. Never commit local `.env` files, certificates, private keys, database exports, access tokens, or production connection details.

Backend:

```bash
cd hrm-server
mvn test
mvn spring-boot:run
```

Frontend:

```bash
cd hrm-admin
npm ci
npm run serve
```

## Production Deployment

Production images are published to GitHub Container Registry:

- `ghcr.io/fo11ow-me/hrm-server`
- `ghcr.io/fo11ow-me/hrm-admin`

The server reuses externally managed MySQL and Redis services. Copy the deployment examples to the server deployment directory, replace every placeholder there, and keep the resulting `.env` readable only by the deployment account.

Required GitHub Environment secrets for environment `dev`:

- `DEPLOY_HOST`
- `DEPLOY_PORT`
- `DEPLOY_USER`
- `DEPLOY_SSH_PRIVATE_KEY`
- `DEPLOY_KNOWN_HOSTS`
- `DEPLOY_HEALTHCHECK_URL`

Isolated CI integration tests derive a disposable MySQL password from the
GitHub Actions run context and generate a masked JWT secret at runtime. They do
not require repository secrets.

Optional GitHub repository variable:

- `DEPLOY_PATH`: deployment directory on the server; defaults to `/opt/hrm`

The server must authenticate to GHCR once with a read-only package token:

```bash
printf '%s' '<GHCR_READ_TOKEN>' | docker login ghcr.io -u '<GHCR_USERNAME>' --password-stdin
```

Do not place the token in shell history, scripts, Compose files, or repository settings other than GitHub Secrets.

See [deploy/README.md](deploy/README.md) for server preparation, Nginx setup, deployment verification, and rollback.

## Security

- CI rejects private keys, environment files, deployment bundles, production profiles, and database exports.
- All production credentials are supplied through GitHub Secrets or the server-side `.env`.
- TLS certificates remain on the server and are managed by Certbot.
- Any credential that has previously appeared in Git history must be rotated before deployment.
