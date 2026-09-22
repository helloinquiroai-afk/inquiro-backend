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

## Rollback
Rollback the application image to the previous known-good version. Do not manually roll back Flyway migrations. For schema recovery, use a verified database backup or a forward migration.
