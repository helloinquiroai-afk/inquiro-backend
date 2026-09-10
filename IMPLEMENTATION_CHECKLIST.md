# Inquiro implementation checklist

Repository inspection, 2026-09-11. This checklist follows repository evidence, not earlier phase numbers.

## Baseline

- Java 17 target, Spring Boot 3.5.15-SNAPSHOT, Maven; local runtime available is Java 25.
- Existing AI request/follow-up extraction, entity merging, configured slot filling, business boundaries and question answering are implemented.
- Business accounts, channel mappings, pending requests and conversation state use JPA/H2.
- Knowledge extraction exists; the knowledge store is in memory and ingestion only conditionally updates the persistent account.
- Availability currently reports business capabilities; it is not inventory or confirmed bookings.
- Messenger receive/send exists but initially handles only the first event synchronously, without signature verification or deduplication.
- Website REST endpoints exist. No frontend, landing page, dashboard or browser-limit implementation is present in this checkout.
- No authentication, billing, real inventory, or business-owner Meta authorization flow is present.
- Baseline: 12 tests, 10 passing, 2 live OpenAI tests fail without credentials. Existing local database files have user changes and must be preserved.

## First milestone: Messenger round trip

- [x] Verified GET /webhook, preserve /messenger/webhook compatibility and WhatsApp routing.
- [x] Validate raw-body SHA-256 signature before accepting POSTs.
- [x] Parse all entries, ignore echoes/non-text events, retain message ID/timestamp.
- [x] Persist inbox before acknowledgment; deduplicate by Page/message ID.
- [x] Share existing conversation engine with scoped business/channel/customer identity.
- [x] Configure Page, token, API version and bounded HTTP timeouts.
- [x] Send replies and best-effort seen/typing actions.
- [x] Friendly AI errors, durable send retries, safe logs.
- [x] Regression, webhook, parsing, sending, persistence and tenant-isolation tests.
- [x] Local application/HTTP smoke test and packaged build.
- [x] Document HTTPS, Meta setup, environment variables and operational limits.
- [ ] Real Page message/reply test (external Page credentials, public HTTPS and Meta subscription required).

## Subsequent milestones (not yet implemented)

1. Persistent owner-managed knowledge and FAQs; AI suggestions with approval; mixed questions without losing workflow state.
2. Business onboarding and authenticated dashboard; self-service Page authorization and encrypted credentials.
3. Configurable inventory, normalized dates, options, confirmation, modification/cancellation for all five initial workflows.
4. Complete tenant authorization across every management endpoint and data model; customer/message history.
5. PostgreSQL migrations, authentication/authorization, rate/usage limits, monitoring and deployment/backup validation.
6. Subscription/billing structure, public demo and first-customer acceptance tests.

Do not treat a passing build or mocked Messenger test as a completed commercial MVP.
