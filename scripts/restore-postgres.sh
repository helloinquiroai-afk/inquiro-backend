#!/usr/bin/env sh
set -eu

: "${POSTGRES_HOST:?POSTGRES_HOST is required}"
: "${POSTGRES_PORT:=5432}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
: "${BACKUP_FILE:?BACKUP_FILE is required}"

test -s "$BACKUP_FILE"
pg_restore --list "$BACKUP_FILE" >/dev/null

echo "WARNING: restore may overwrite target database objects."
PGPASSWORD="$POSTGRES_PASSWORD" pg_restore   --clean   --if-exists   --no-owner   --host "$POSTGRES_HOST"   --port "$POSTGRES_PORT"   --username "$POSTGRES_USER"   --dbname "$POSTGRES_DB"   "$BACKUP_FILE"

echo "Restore completed: $BACKUP_FILE"
