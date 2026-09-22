#!/usr/bin/env sh
set -eu

: "${BACKUP_DIR:=./backups}"
: "${BACKUP_DESTINATION:?BACKUP_DESTINATION is required for production backups}"
: "${BACKUP_REQUIRE_EXTERNAL:=true}"

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)

BACKUP_DIR="$BACKUP_DIR" sh "$SCRIPT_DIR/backup-postgres.sh"

backup_file="$(ls -1t "$BACKUP_DIR"/inquiro-*.dump | head -n 1)"
test -s "$backup_file"

BACKUP_FILE="$backup_file" BACKUP_DESTINATION="$BACKUP_DESTINATION"   sh "$SCRIPT_DIR/upload-postgres-backup.sh"

echo "Production backup completed: $backup_file"
