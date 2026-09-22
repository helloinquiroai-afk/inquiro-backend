#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

DEPLOY_REF="${DEPLOY_REF:-master}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.production-stack.yml}"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://127.0.0.1:8080/actuator/health/readiness}"
WAIT_SECONDS="${DEPLOY_WAIT_SECONDS:-180}"

command -v docker >/dev/null 2>&1 || { echo "ERROR: docker is required" >&2; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "ERROR: docker compose is required" >&2; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "ERROR: curl is required" >&2; exit 1; }
test -f "$COMPOSE_FILE" || { echo "ERROR: $COMPOSE_FILE not found" >&2; exit 1; }
test -f ".env" || { echo "ERROR: .env is required and must contain production secrets" >&2; exit 1; }

if [ "${DEPLOY_SKIP_PULL:-false}" != "true" ]; then
  git fetch --prune origin "$DEPLOY_REF"
  git checkout "$DEPLOY_REF"
  git pull --ff-only origin "$DEPLOY_REF"
fi

echo "Validating production compose configuration..."
docker compose --env-file .env -f "$COMPOSE_FILE" config >/dev/null

echo "Starting production stack..."
if [ "${DEPLOY_SKIP_BUILD:-false}" = "true" ]; then
  docker compose --env-file .env -f "$COMPOSE_FILE" up -d
else
  docker compose --env-file .env -f "$COMPOSE_FILE" up -d --build
fi

echo "Waiting for backend readiness..."
deadline=$((SECONDS + WAIT_SECONDS))
while [ "$SECONDS" -lt "$deadline" ]; do
  if body="$(curl --fail --silent --show-error --max-time 5 "$HEALTHCHECK_URL" 2>/dev/null)"; then
    echo "Backend readiness: $body"
    docker compose --env-file .env -f "$COMPOSE_FILE" ps
    echo "Production deployment completed successfully."
    exit 0
  fi
  sleep 5
done

echo "ERROR: backend did not become ready within ${WAIT_SECONDS}s" >&2
docker compose --env-file .env -f "$COMPOSE_FILE" ps >&2 || true
docker compose --env-file .env -f "$COMPOSE_FILE" logs --tail=200 inquiro-backend >&2 || true
exit 1
