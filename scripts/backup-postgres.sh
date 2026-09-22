#!/usr/bin/env sh
set -eu

: "${POSTGRES_HOST:?POSTGRES_HOST is required}"
: "${POSTGRES_PORT:=5432}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
: "${BACKUP_DIR:=./backups}"
: "${BACKUP_RETENTION_DAYS:=30}"

mkdir -p "$BACKUP_DIR"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="$BACKUP_DIR/inquiro-$timestamp.dump"
tmp_file="$file.tmp"

cleanup() {
  rm -f "$tmp_file"
}
trap cleanup EXIT INT TERM

echo "Creating PostgreSQL backup: $file"
PGPASSWORD="$POSTGRES_PASSWORD" pg_dump   --host "$POSTGRES_HOST"   --port "$POSTGRES_PORT"   --username "$POSTGRES_USER"   --format=custom   --file "$tmp_file"   "$POSTGRES_DB"

test -s "$tmp_file"
mv "$tmp_file" "$file"

echo "Verifying backup archive"
pg_restore --list "$file" >/dev/null
test -s "$file"

echo "Removing local backups older than $BACKUP_RETENTION_DAYS days"
find "$BACKUP_DIR" -type f -name 'inquiro-*.dump' -mtime +"$BACKUP_RETENTION_DAYS" -delete

echo "Backup created and verified: $file"
