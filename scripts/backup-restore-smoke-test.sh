#!/usr/bin/env sh
set -eu

: "${POSTGRES_HOST:?POSTGRES_HOST is required}"
: "${POSTGRES_PORT:=5432}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
: "${BACKUP_DIR:=./backups/phase-57}"

mkdir -p "$BACKUP_DIR"
backup_file="$BACKUP_DIR/inquiro-phase-57.dump"

export PGPASSWORD="$POSTGRES_PASSWORD"

echo "Creating backup: $backup_file"
pg_dump   --host "$POSTGRES_HOST"   --port "$POSTGRES_PORT"   --username "$POSTGRES_USER"   --format=custom   --file "$backup_file"   "$POSTGRES_DB"

test -s "$backup_file"

echo "Removing sentinel data to prove restore changes the database"
psql   --host "$POSTGRES_HOST"   --port "$POSTGRES_PORT"   --username "$POSTGRES_USER"   --dbname "$POSTGRES_DB"   -v ON_ERROR_STOP=1   -c "DROP TABLE IF EXISTS phase57_restore_sentinel;"

echo "Restoring backup"
pg_restore   --clean   --if-exists   --no-owner   --host "$POSTGRES_HOST"   --port "$POSTGRES_PORT"   --username "$POSTGRES_USER"   --dbname "$POSTGRES_DB"   "$backup_file"

echo "Verifying restored sentinel"
sentinel="$(psql   --host "$POSTGRES_HOST"   --port "$POSTGRES_PORT"   --username "$POSTGRES_USER"   --dbname "$POSTGRES_DB"   -tAc "SELECT payload FROM phase57_restore_sentinel WHERE id = 1;")"

test "$sentinel" = "phase-57-backup-restore-ok"

echo "Verifying Flyway history survived restore"
migration_count="$(psql   --host "$POSTGRES_HOST"   --port "$POSTGRES_PORT"   --username "$POSTGRES_USER"   --dbname "$POSTGRES_DB"   -tAc "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true;")"

test "$migration_count" -ge 4

echo "Phase 57 backup/restore validation passed"
