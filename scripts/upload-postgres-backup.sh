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
    echo "Backup copied to file destination: $destination"
    ;;
  s3://*)
    : "${AWS_REGION:=us-east-1}"
    aws_args=""
    if [ -n "${S3_ENDPOINT_URL:-}" ]; then
      aws_args="--endpoint-url $S3_ENDPOINT_URL"
    fi
    # shellcheck disable=SC2086
    aws s3 cp "$BACKUP_FILE" "$BACKUP_DESTINATION/" --region "$AWS_REGION" $aws_args
    echo "Backup uploaded to: $BACKUP_DESTINATION"
    ;;
  *)
    echo "Unsupported BACKUP_DESTINATION: $BACKUP_DESTINATION" >&2
    echo "Use file:///absolute/path or s3://bucket/prefix" >&2
    exit 2
    ;;
esac
