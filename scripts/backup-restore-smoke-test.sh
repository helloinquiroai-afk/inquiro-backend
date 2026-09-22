#!/usr/bin/env sh
set -eu

: "${POSTGRES_HOST:?POSTGRES_HOST is required}"
: "${POSTGRES_PORT:=5432}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
: "${BACKUP_DIR:=./backups/phase-57}"

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
mkdir -p "$BACKUP_DIR"
export PGPASSWORD="$POSTGRES_PASSWORD"

echo "Creating PostgreSQL backup with scripts/backup-postgres.sh"
BACKUP_DIR="$BACKUP_DIR" sh "$SCRIPT_DIR/backup-postgres.sh"

backup_file="$(ls -1t "$BACKUP_DIR"/inquiro-*.dump | head -n 1)"
test -s "$backup_file"
echo "Backup validated: $backup_file"

echo "Removing sentinel data to prove restore changes the database"
psql   --host "$POSTGRES_HOST"   --port "$POSTGRES_PORT"   --username "$POSTGRES_USER"   --dbname "$POSTGRES_DB"   -v ON_ERROR_STOP=1   -c "DROP TABLE IF EXISTS phase57_restore_sentinel;"

echo "Restoring PostgreSQL backup with scripts/restore-postgres.sh"
BACKUP_FILE="$backup_file" sh "$SCRIPT_DIR/restore-postgres.sh"

echo "Verifying restored sentinel"
sentinel="$(psql   --host "$POSTGRES_HOST"   --port "$POSTGRES_PORT"   --username "$POSTGRES_USER"   --dbname "$POSTGRES_DB"   -tAc "SELECT payload FROM phase57_restore_sentinel WHERE id = 1;")"

test "$sentinel" = "phase-57-backup-restore-ok"

echo "Verifying real Flyway history survived restore"
migration_count="$(psql   --host "$POSTGRES_HOST"   --port "$POSTGRES_PORT"   --username "$POSTGRES_USER"   --dbname "$POSTGRES_DB"   -tAc "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true;")"

test "$migration_count" -ge 4

echo "Phase 57 backup/restore validation passed"
