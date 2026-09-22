# Inquiro Production Runbook

## Phase 51 — Deployment
Provision a Linux host or container platform with Docker and persistent storage. Create a private .env from .env.example, configure secrets, build the image, start the stack, and confirm readiness returns HTTP 200.

## Phase 52 — Domain and HTTPS
Point the API DNS record to the host, set the real hostname in deploy/nginx/inquiro.conf, issue a certificate with the hosting provider or Let's Encrypt, force HTTP to HTTPS, and set INQUIRO_SECURITY_ALLOWED_ORIGINS to the real frontend origin.

## Phase 53 — PostgreSQL
Prefer managed PostgreSQL. If self-hosting, use persistent storage and restricted network access. Start PostgreSQL before Inquiro. Flyway applies classpath:db/migration automatically. Keep Hibernate ddl-auto=validate. Never edit an already-applied migration; add the next version.

## Phase 54 — End-to-end testing
Run scripts/production-smoke-test.ps1 against the real HTTPS URL. Verify health, authentication, widget/public chat, business routing, availability, booking, webhook signature validation, and database persistence.

## Phase 55 — Monitoring, backup and recovery
Monitor readiness and logs, configure restart alerts, schedule PostgreSQL backups, store backups outside the database host, and periodically restore a backup into a separate database. A successful backup is not proof of recoverability.

## Phase 57 — Backup and restore validation
The repository contains scripts/backup-postgres.sh and scripts/restore-postgres.sh for PostgreSQL custom-format backups and restores.

The automated Backup Restore Validation workflow validates the actual repository scripts against the same PostgreSQL/Flyway production integration stack:

1. Start PostgreSQL and the Inquiro backend with the production profile.
2. Wait for backend readiness so Flyway has applied the real migrations.
3. Insert a known sentinel row into the migrated database.
4. Create a custom-format backup using scripts/backup-postgres.sh.
5. Remove the sentinel table.
6. Restore the backup using scripts/restore-postgres.sh.
7. Verify the sentinel data is restored.
8. Verify the Flyway history contains at least the four current successful migrations.
9. Verify a non-empty backup artifact was produced.

This is a recoverability test, not just a backup-file test. The workflow runs on pushes and pull requests targeting master.

For real production operations, keep backup files outside the database host or container, restrict access to them, retain multiple recovery points, and periodically perform the same restore test against a separate recovery database. Do not restore over the live production database as a routine validation procedure.

## Phase 58 — Production backup operations
Phase 58 turns the validated backup scripts into an operational backup workflow.

### Backup command
Use `scripts/run-production-backup.sh` from a trusted production host. It requires `BACKUP_DESTINATION` so a successful run has an external copy, not only a file on the PostgreSQL host.

Required environment:
- `POSTGRES_HOST`
- `POSTGRES_PORT` (default 5432)
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `BACKUP_DESTINATION`, normally an `s3://bucket/prefix`

Optional:
- `BACKUP_DIR` (default ./backups)
- `BACKUP_RETENTION_DAYS` (default 30)
- `AWS_REGION`
- `S3_ENDPOINT_URL` for an S3-compatible provider

The backup is written atomically, checked with `pg_restore --list`, and old local dump files are removed according to `BACKUP_RETENTION_DAYS`. The upload helper supports AWS S3 and S3-compatible endpoints. Keep credentials in the server secret manager or protected environment, never in Git.

### Scheduling
Run the production backup wrapper from the host scheduler, for example cron:

```cron
0 2 * * * /opt/inquiro/scripts/run-production-backup.sh >> /var/log/inquiro-backup.log 2>&1
```

Use the production server's real absolute paths and environment-loading mechanism. The scheduler must run as a dedicated service account with access only to the database credentials, backup directory, and backup destination.

### External retention and recovery
Configure object-storage lifecycle retention separately at the bucket/provider level. Keep multiple recovery points and protect backups from accidental deletion where the provider supports object lock/versioning. Periodically restore a recent external backup into a separate recovery database using `scripts/restore-postgres.sh`.

Phase 58 CI includes a disposable S3-compatible object store and verifies that a generated dump is uploaded successfully. It does not prove credentials, bucket policy, retention, or restore behavior for a real cloud account.

## Phase 59 — Full API/E2E production smoke testing
The repository now includes scripts/phase-59-e2e-smoke-test.py and the E2E Production Smoke workflow. It starts the same production-profile PostgreSQL integration stack used by the other production workflows and exercises the main authenticated customer-management and booking path end to end.

The automated flow verifies readiness, business-user registration/login/logout, business creation, tenant membership, website channel creation and origin configuration, booking inventory, availability, booking creation, idempotent booking retry, capacity exhaustion, cancellation, inventory release, and cross-tenant access denial.

The E2E test intentionally does not call the real OpenAI or Meta services. Those external acceptance tests require real credentials and provider-side configuration and remain a deployment-stage acceptance step. Likewise, HTTPS/DNS and the browser widget are tested separately against the real deployment.

## Rollback
Rollback the application image to the previous known-good version. Do not manually roll back Flyway migrations. For schema recovery, use a verified database backup or a forward migration.
