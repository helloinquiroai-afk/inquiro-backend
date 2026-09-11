# Business Knowledge MVP validation

Validated locally on 2026-09-11 using Java 25.0.2 and Maven, compiling for Java 17. This milestone is complete locally; the commercial SaaS MVP is not complete.

## Automated tests and package

- Baseline before this milestone: **56 tests passed**.
- Final complete `mvn -o package`: **91 tests, zero failures, zero errors, zero skipped; BUILD SUCCESS**. The executable jar is `target/inquiro-backend-0.0.1-SNAPSHOT.jar`.
- Final source/documentation diff whitespace check passed. Git status was reviewed; no Messenger source or local runtime database changes are included.
- Added 35 deterministic tests across `BusinessKnowledgeApiTest`, `BusinessKnowledgeAiTest`, `KnowledgeRestartTest` and `ConversationServiceTest`. External AI calls are mocked in the automated suite.
- API coverage includes authenticated GET/PUT, complete replacement, FAQ removal, null collections, malformed/unknown fields, unknown businesses, business isolation, concurrent FAQ approval, transient suggestions, edited approval, duplicate approval, rejection, safe AI failures and ingestion without a Page ID.
- AI coverage verifies configured facts, FAQs, policies and hours; unknown/malformed source selection fails closed; generated prose cannot replace approved source answers; unsupported capabilities and availability caveats are preserved.
- Conversation coverage includes knowledge-only interruptions, initial mixed requests, mixed follow-ups, completed-request responses and knowledge failures without losing workflow state. Existing Messenger tests remain in the complete suite; Messenger source files are unchanged.
- Persistence is tested by closing an application context and reopening another against the same temporary file H2 database. Knowledge and an approved FAQ survive reopening. Tests do not use the user's local runtime database.

## Persistence and API behavior

Knowledge remains in the existing JPA `business_account.profile_json` field as part of `BusinessProfile`. No schema migration, extra knowledge table or second persistence technology was introduced. The former in-memory knowledge store now delegates to persisted accounts. Default-business question answering reloads the current persisted profile.

All new routes retain `X-Inquiro-Management-Key` protection:

| Method | Route | Behavior |
| --- | --- | --- |
| GET | `/api/business/accounts/{businessId}/knowledge` | Read approved knowledge |
| PUT | `/api/business/accounts/{businessId}/knowledge` | Explicit complete knowledge replacement; preserves other profile fields |
| POST | `/api/business/accounts/{businessId}/knowledge/faq-suggestions` | Generate transient drafts without publishing |
| POST | `/api/business/accounts/{businessId}/knowledge/faq-suggestions/review` | Explicit `APPROVE` or `REJECT`; operator may edit the draft |

Approval appends the FAQ to persisted knowledge. Rejection changes nothing. Missing businesses return 404; invalid input returns 400; unauthenticated management requests return 401. Generation failures return a sanitized 502. Pending suggestions/rejection history are not persisted.

## Packaged application and live OpenAI check

Started the packaged jar on port 18082 with an isolated in-memory database, fictional hotel profile and test-only operator key. The Messenger worker was disabled; no Meta messages were sent. Used the already configured OpenAI credential through the actual HTTP conversation endpoint.

- Protected profile and knowledge GET/PUT succeeded.
- FAQ generation returned 10 suggestions. Rejecting one kept the approved FAQ count at one; approving it increased the count to two.
- An initial live run exposed omission of the relative date in a mixed message. The extraction prompt was clarified, the entire test suite/package rerun, and the rebuilt app checked again with the following results:

| Customer message | Observed result on final build |
| --- | --- |
| I need a room in Paris next Friday for two people and do you have free parking? | Captured `location=Paris`, `checkInDate=next Friday`, `guestCount=2`; answered free parking once and asked only for duration |
| What time is breakfast? | Exact configured 7 AM to 10 AM hours; retained booking fields and missing duration |
| How much does airport pickup cost? | Said the business had not provided that information; invented no price |
| Do you have a swimming pool? | Stated the configured unsupported capability; retained booking state |
| Three nights. | Captured `durationNights=3`; retained earlier fields; exactly one request in `PENDING_CONFIRMATION` |

These observations validate these examples, not universal LLM accuracy. The response renderer uses approved source text, but source relevance and intent extraction still depend on the model. Owners must resolve contradictory knowledge. Relative dates remain phrases in the current workflow; normalized dates and real inventory are later work. Requests are not automatically confirmed reservations.

## Remaining manual checks and limitations

- Configure a real business's approved facts/policies and exercise GET, full PUT, draft generation, edited approval, rejection and FAQ removal using its operator key.
- Test the deployed Messenger Page end to end once the public HTTPS webhook and Meta configuration are available. This milestone does not change Messenger transport.
- Verify business-specific wording and mixed conversations with real owner content before customer use. Approved source rendering favors owner wording over unrestricted paraphrasing/translation.
- The management key remains a shared operator credential; business-user login and self-service tenant authorization are outside this milestone. Full knowledge PUT intentionally replaces the object; clients must GET first and retain unrelated sections. Concurrent stale full-object replacements are not version-checked.
- Drafts have no persisted review history. No dashboard, real inventory, billing, PostgreSQL migration or automatic reservation confirmation was added.
- No production database or Meta configuration was changed. Local runtime database files were not included in this change.
- The isolated smoke-test process was stopped after verification.

# Previous Messenger milestone validation

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
- Business-user authentication, self-service onboarding/Page connection, real inventory/booking confirmation, billing and complete SaaS tenant authorization remain on the implementation checklist. FAQ approval is now implemented in the Business Knowledge milestone above.
