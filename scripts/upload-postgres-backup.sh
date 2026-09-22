#!/usr/bin/env sh
set -eu

: "${BACKUP_FILE:?BACKUP_FILE is required}"
: "${BACKUP_DESTINATION:?BACKUP_DESTINATION is required}"

test -s "$BACKUP_FILE"

case "$BACKUP_DESTINATION" in
  file://*)
    destination="${BACKUP_DESTINATION#file://}"
    mkdir -p "$destination"
    cp "$BACKUP_FILE" "$destination/"
    echo "Backup copied to external file destination: $destination"
    ;;
  *)
    echo "Unsupported BACKUP_DESTINATION scheme: $BACKUP_DESTINATION" >&2
    echo "Supported in this phase: file:///absolute/path/to/external/storage" >&2
    exit 2
    ;;
esac
