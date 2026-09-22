# Phase 60 — Real Production Server Deployment

Phase 60 moves the repository from production-ready deployment assets to a repeatable deployment procedure for a real Linux server.

## Scope

The repository already provides:
- Java 17 Spring Boot production image
- PostgreSQL service and persistent volume
- Nginx reverse proxy configuration
- production profile and Flyway migrations
- readiness/liveness endpoints
- production security validation
- backup/restore scripts
- Phase 57–59 automated production-like validation

Phase 60 adds a server-side deployment helper that validates the production Compose configuration, updates the checkout, starts the stack, waits for backend readiness, and prints the final container state.

## Server prerequisites

1. Linux host supported by Docker
2. Docker Engine with Compose v2
3. Git
4. curl
5. persistent disk for PostgreSQL
6. firewall allowing only required public ports (normally 80/443)
7. a private .env file with real production values
8. a DNS name for the reverse proxy

Do not put .env or production credentials in GitHub.

## Initial server setup

Use a layout such as:
/opt/inquiro/inquiro-backend/

Clone the repository and create .env from .env.example. Replace every placeholder with real values.

Before first deployment verify:
docker --version
docker compose version
git status
test -f .env

## Deploy

From the repository checkout:
chmod +x scripts/deploy-production.sh
./scripts/deploy-production.sh

The script:
1. fetches the selected Git ref (default master)
2. fast-forwards the checkout
3. validates the Compose configuration
4. builds the backend image unless skipped
5. starts PostgreSQL, backend, and Nginx
6. waits for backend readiness
7. prints container status
8. prints backend logs if readiness fails

Optional variables:
- DEPLOY_REF — Git ref, default master
- DEPLOY_SKIP_PULL=true — use an already checked-out revision
- DEPLOY_SKIP_BUILD=true — reuse the existing image
- HEALTHCHECK_URL — readiness URL
- DEPLOY_WAIT_SECONDS — readiness timeout, default 180 seconds
- COMPOSE_FILE — production Compose filename

## Production acceptance gate

A deployment is not accepted merely because containers started.

After deployment verify the container state and the HTTPS health, liveness, and readiness endpoints. Then run scripts/production-smoke-test.ps1 against the real HTTPS endpoint from a trusted machine.

## Important boundary

This repository change automates and documents the deployment procedure. A real production deployment cannot truthfully be marked complete until a real server is provisioned, real secrets are installed, DNS/TLS configuration is applied, and the acceptance checks succeed.

Phase 60 therefore has two states:
- Implementation complete: deployment automation and server procedure are in Git.
- Environment acceptance complete: real server deployment and external HTTPS smoke test have succeeded.

Do not commit real credentials, private keys, certificates, database passwords, API keys, or .env files.
