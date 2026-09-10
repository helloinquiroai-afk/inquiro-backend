# Messenger milestone validation

Validated locally on 2026-09-11 using Java 25.0.2 and Maven, compiling for Java 17.

## Automated verification

- Baseline: 12 tests; 10 passed, two failed because they depended on live OpenAI credentials.
- Final `mvn package`: **56 tests, zero failures, zero errors, zero skipped**; executable Spring Boot jar produced.
- External APIs are mocked in the automated suite. The test database is in-memory H2; the user's existing file database was not used by these tests.
- Source diff whitespace check passed.
- Concurrent duplicate inserts were tested against the actual database unique constraint, not just an in-memory flag.
- A failed send was retried using the persisted reply, with only one AI analysis invocation.
- Business/Page reassignment, cross-channel session separation, notification routing and business-scoped operator retry were tested.

## Packaged application smoke test

Started the packaged jar on port 18081 with an isolated in-memory database and test-only webhook/operator secrets. The background Messenger worker was disabled; no real Messenger messages were sent.

Verified over HTTP:

- Correct webhook handshake: 200 and exact challenge.
- Incorrect verification token: 403.
- Unsigned nonempty webhook: 403. Empty request bodies are rejected by Spring as 400.
- Management profile without an operator key: 401.
- Profile update with the operator key: success.
- Messenger event-summary endpoint with the operator key: success.
- Website conversation/reset endpoints: success.

## Live OpenAI conversation

Used the already configured OpenAI credential, through the packaged app's actual website conversation endpoint. Loaded the fictional hotel profile into the isolated smoke-test database.

| Customer message | Observed result |
| --- | --- |
| I need a hotel in Paris. | `location=Paris`; asks for check-in date |
| Next Friday for two adults. | `checkInDate=Next Friday`, `guestCount=2`; asks only for duration |
| Actually make it three adults. | `guestCount=3`; existing location/date retained |
| Do you have free parking? | Answers free parking using configured hotel facts; preserves unfinished request |
| Three nights. | `durationNights=3`; creates one `ROOM_BOOKING` request in `PENDING_CONFIRMATION` |

The first live run exposed counts returned as phrases. Both extraction prompts were updated to request numeric counts; the complete conversation above then passed on a second run. This observation validates the tested example, not universal LLM extraction accuracy.

The smoke-test process was stopped afterward. No production database or Meta configuration was changed.

## External and later-milestone checks

- Real Messenger receive/reply is **not verified**: `FACEBOOK_PAGE_ID`, `FACEBOOK_APP_SECRET`, public HTTPS callback and Meta Page subscription are still needed. The existing environment has legacy Messenger token and OpenAI variables, but token presence alone does not prove Page access.
- Dockerfile supplied; Docker execution was not available for validation.
- Java 17 runtime execution, PostgreSQL, distributed workers and public-host deployment were not tested.
- No frontend checkout was available, so browser rendering/localStorage/daily-limit behavior was not directly exercised.
- Business-user authentication, self-service onboarding/Page connection, FAQ approval, real inventory/booking confirmation, billing and complete SaaS tenant authorization remain on the implementation checklist.
