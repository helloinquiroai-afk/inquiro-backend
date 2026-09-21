#!/usr/bin/env sh
set -eu
: "${POSTGRES_HOST:?POSTGRES_HOST is required}"
: "${POSTGRES_PORT:=5432}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${BACKUP_DIR:=./backups}"
mkdir -p "$BACKUP_DIR"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="$BACKUP_DIR/inquiro-$timestamp.dump"
PGPASSWORD="${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}" pg_dump --host "$POSTGRES_HOST" --port "$POSTGRES_PORT" --username "$POSTGRES_USER" --format=custom --file "$file" "$POSTGRES_DB"
echo "Backup created: $file"
