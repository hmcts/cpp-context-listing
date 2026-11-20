# Breaking changes — WildFly (CQRS/ES) → Spring Boot migration

**Guiding principle (§3 of `docs/MBD_REFACTOR_PROMPT.md`): a drop-in replacement.** Upstream
callers, downstream consumers of `public.event` and the legacy IT suite must not be able to tell
the implementation changed. Parity is defined by D31: the same command or inbound message leaves
the same viewstore data and emits the same public messages as JEE + WildFly. The legacy suite on
the Spring stack (335 run / 0 failed / 3 skipped, `it-logs/run-20261119T223718Z-p2-final-legacy.log`)
is the check.

Each entry below is a difference that is still there, with the decision that accepts it, the
before/after and the evidence. Entries are split into:

- **Wire (W):** visible to an HTTP caller, a JMS consumer or an outbound integration.
- **Operational (O):** visible to operators, deployers or the build, not to callers.

"Verified" means a test or captured artefact shows it; "inferred" means read from the code on both
sides but not exercised against a running JEE app.

---

## Summary

| Id | Kind | Difference | Status |
|---|---|---|---|
| W1 | Wire (JMS) | Public messages carry a new `_metadata.id` | Accepted (ADR 015) |
| W2 | Wire (HTTP) | Command failures reach the caller (commands are synchronous) | Accepted (D5) |
| W3 | Wire (HTTP) | `/courtlist` without the vendor `Accept` is 403 | Open: accept or add a rule |
| W4 | Wire (HTTP) | `Accept` with more than one vendor type is 406 | Accepted (ADR 005 §6) |
| W5 | Wire (HTTP) | Malformed input JEE answered with 500 is now 400 | Accepted, inferred |
| W6 | Wire (HTTP) | Upstream REST failure is 502; generic 500 body is `{"error":…}` | Accepted, inferred |
| W7 | Wire (HTTP) | Health and metrics endpoints moved to Actuator | Accepted (ADR 008, D11) |
| W8 | Wire (outbound) | Authorisation reads usersgroups *logged-in-user permissions* | Open (§15.4, §15.15b) |
| O1 | Operational | No event store, private events, `listing.event`, `listing.handler.command` | Accepted (D29) |
| O2 | Operational | No JMX system commands, catch-up, replay or selective catch-up | Open (§15.8) |
| O3 | Operational | Inbound duplicates (same `_metadata.id`) are ignored | Accepted (D16, D26) |
| O4 | Operational | Undeliverable public message dropped after 10 attempts | Accepted (ADR 002, 013) |
| O5 | Operational | Court-list async steps are in memory, not durable | Accepted (ADR 015) |
| O6 | Operational | Per-instance advisory locks; a deadlock is not retried | Accepted (ADR 013 P2.4) |
| O7 | Operational | No backfill of aggregate-only state at cut-over | Open (D30) |
| O8 | Operational | Liquibase checksums upgraded `8:` → `9:`; two `017` index drops run | Accepted (D21) |
| O9 | Operational | Jar, env vars, logging, audit switches | Accepted (ADR 007, D12, D24) |
| O10 | Operational | Build and IT tooling | Accepted (D4, D7, D13) |

---

## Wire differences

### W1 — Public message `_metadata.id` is new per outbox write
- **Decision:** ADR 015 ("Public message metadata"), D29.
- **Before:** the processor built each public message with `metadataFrom(privateEvent).withName(…)`,
  so its `_metadata.id` was the private event's id.
- **After:** private events do not exist; `EventOutbox.envelopeFor(command, …)` gives each public
  message a new random id. `name`, `createdAt` format, `causation` (command id appended),
  `context.user`, `correlation.client` and `stream.id` (the former aggregate id) follow the same
  convention. Ids are opaque to framework consumers (they dedup on them, which still works).
- **Evidence:** `EventOutboxTest`; byte-level wire format `EnvelopeCodecTest` and
  `MessagingIntegrationTest` against the message captured from the JEE app
  (`listing-messaging/src/test/resources/legacy/`); payloads `PublicMessageContractTest`. Verified.

### W2 — Commands are synchronous: failures reach the caller
- **Decision:** D5 (synchronous in the request transaction, still `202 Accepted`), ADR 015.
- **Before:** the command API validated and enriched, sent the command to
  `listing.handler.command` and answered `202`. Aggregate rejections, handler exceptions and
  processor failures happened later and were invisible to the caller (logged, retried by JMS, or
  dead-lettered).
- **After:** the whole chain (rules, listener writes, processor steps, outbox) runs before the
  response. A rejected command that JEE logged and ignored is still logged and still `202` (each
  guard is ported as log-and-no-op), but an exception now gives the caller an error status
  (`{"error":…}`, usually 500) and nothing is committed. Specific cases:
  - external reads inside processor steps (refdata organisation unit, progression case for
    hearing-confirmed) failing fail the command; JEE retried the step and then dropped it
    (ADR 015 P2.3.2 "Other decisions"); the courtscheduler slot release is still caught and logged;
  - a deadlock between two commands on the same instances gives 500 (O6);
  - latency of heavy commands (court-list publish, next hearings) is now in the request.
- **Evidence:** D5; ADR 015 P2.3.2 notes; `HearingCommandIntegrationTest`,
  `CourtListCommandIntegrationTest`; legacy suite green with the same assertions. Verified for
  success paths; error statuses inferred.

### W3 — `/courtlist` needs the vendor `Accept` to be authorised
- **Decision:** none yet; consequence of ADR 005 §2 (`Action.name` = vendor token, §11.3).
- **Before:** `DefaultQueryApiCourtlistResource` (hand-written JAX-RS adapter, deleted at P2.6; in
  git at `b932de14b:listing-query/listing-query-api/src/main/java/uk/gov/justice/api/resource/DefaultQueryApiCourtlistResource.java`)
  built the query with the fixed name `listing.search.court.list` and ran the interceptor chain, so
  any `Accept` JAX-RS let through (e.g. `*/*` or none) was authorised by that rule.
- **After:** `cp-auth-rules-filter` takes the action from the first `Accept` vendor token. Without
  `application/vnd.listing.search.court.list+json` the action is `GET /query/api/rest/listing/courtlist`,
  which has no rule → 403. The response is still `application/pdf` whatever the `Accept`. The same
  applies to any query sent with `Accept: */*` or no `Accept`; for the generated JEE resources the
  action was the RAML mapping of the matched method, so those would have been authorised too.
- **Fix if needed:** a DRL rule on the `GET <path>` fallback with the same group list.
- **Evidence:** `listing-query-api.drl` rule `listing.search.court.list`;
  `QueryEndpointsIntegrationTest.courtList_isThePdfRenderedBySystemDocGenerator` (sends the vendor
  type). Inferred; no IT sends `*/*`.

### W4 — `Accept` listing more than one vendor type is 406
- **Decision:** ADR 005 §6 (`AuthzActionGuardFilter`, ported from courtscheduler).
- **Before:** JAX-RS negotiated across the listed types.
- **After:** when `Content-Type` has no vendor token, more than one distinct vendor type in `Accept`
  → 406 `{"error":"Accept must not list more than one vendor media type"}`. Stops a caller
  authorising as one action and dispatching to another. The `CPP-ACTION` header is ignored, as
  the framework ignored it.
- **Evidence:** `AuthzActionGuardFilter`; `WebPlumbingTest.severalVendorTypesInAcceptAre406`. Verified on Spring; no known client
  sends several vendor types.

### W5 — Malformed input that JEE answered with 500 is 400
- **Decision:** `GlobalExceptionHandler` (courtscheduler pattern, ADR 007 §7).
- **Before:** an unmapped `IllegalArgumentException` (e.g. `UUID.fromString` on a bad id or a bad
  `CJSCPPUID`), `NumberFormatException` or JSON parse failure reached RESTEasy as a 500.
- **After:** these, plus missing required parameters and type mismatches, are 400 `{"error":…}`.
  Where JEE already mapped the case it is unchanged: `GET /hearings/not-a-uuid` was 400 in JEE
  (`HearingQueryApi.ensureThatHearingIdIsAValidUUID`) and is 400 now.
- **Evidence:** `GlobalExceptionHandler#handleBadRequestInput`, `#handleIllegalArg`;
  `QueryEndpointsIntegrationTest.searchHearing_unknownIdIs404_invalidIdIs400`. Inferred for the
  JEE 500s.

### W6 — Upstream failures are 502; generic 500 body
- **Decision:** `GlobalExceptionHandler#handleUpstream`, `#handleAny` (from courtscheduler).
- **Before:** an unmapped failure of a downstream REST call surfaced as 500 with WildFly's body.
- **After:** `RestClientResponseException` → 502 `{"error":"Upstream service responded with <n>"}`;
  any other unhandled exception → 500 `{"error":"Internal Server Error"}`. Cases JEE mapped
  explicitly keep their JEE status and body (see "Unchanged").
- **Evidence:** `GlobalExceptionHandler`. Inferred (no test covers the 502, no JEE capture of a
  500 body).

### W7 — Health and metrics endpoints
- **Decision:** ADR 008, D11; `application-server-management.yml`.
- **Before:** framework healthchecks at `/listing-service/internal/healthchecks/all`
  (cpp-developers-docker `healthcheck-functions.sh`), job/file-store checks ignored
  (`ListingIgnoredHealthcheckNamesProvider`, deleted).
- **After:** `/listing-service/actuator/health` (+ `/health/liveness`, `/health/readiness`),
  `/actuator/info`, `/actuator/prometheus`, `/actuator/metrics`. Per-action metrics are
  `cpp.action.total` / `cpp.action.individual`. JMS health follows `listing.messaging.enabled`.
- **Evidence:** `SmokeIntegrationTest.actuatorHealthIsUpUnderTheContextPath`, `MessagingSwitchTest`. Verified. Probes in the Helm chart
  must change (§15.7).

### W8 — Authorisation reads usersgroups *logged-in-user permissions*
- **Decision:** ADR 005 (library behaviour); open items §15.4 (D23) and §15.15(b).
- **Before:** the framework's `UserAndGroupProvider` queried groups by user on
  `/usersgroups-service/…`.
- **After:** `cp-auth-rules-filter` calls
  `${LISTING_BASE_URL}/usersgroups-query-api/query/api/rest/usersgroups/users/logged-in-user/permissions`
  (`Accept: application/vnd.usersgroups.get-logged-in-user-permissions+json`) for each request.
  Same allow/deny only if that response carries the same group names. Other outbound calls keep
  their paths, media types and user (D19).
- **Evidence:** `application-authz.yml`; `AuthorizationIntegrationTest`,
  `QueryAccessControlIntegrationTest` (WireMock). To verify in a non-prod environment.

---

## Operational differences

### O1 — No event store, no private events
- **Decision:** D29, ADR 015; invariants §5.
- **Before:** events in `event_log`/`event_stream`, published on topic `listing.event`
  (`listing.events.*`); commands queued on `listing.handler.command`; stream buffer and
  `processed_event`.
- **After:** none of these are written or read. `listing.events.*` are never published; the
  framework tables stay in the database untouched. Only the legacy ITs consumed `listing.event`
  (harness changed, D1). Any other consumer of `listing.event` would see nothing.
- **Evidence:** `EventOutbox` rejects non-`public.*` names (`EventOutboxTest`); no
  `uk.gov.justice.*` on the runtime classpath (`:listing-api:dependencies`).

### O2 — No JMX system commands, catch-up or replay
- **Decision:** open, §15.8.
- **Before:** `runSystemCommand.sh` (framework JMX client: CATCHUP, rebuild, …) and
  `listing-selective-catchup` rebuilt the viewstore from the event store.
- **After:** both deleted. The viewstore is the source of truth; recovery is a database restore.

### O3 — Inbound duplicates are ignored
- **Decision:** D16, D26, D27; ADR 013.
- **Before:** the processor's public-event subscription had no `processed_event` check
  (`BackwardsCompatibleSubscriptionManager`), so a redelivered message was handled again.
- **After:** `inbound_event_dedup` keyed on `_metadata.id`, written in the handler's transaction;
  a message with a seen id is acknowledged and skipped. Rows kept 30 days
  (`LISTING_INBOUND_DEDUP_RETENTION`). A handler failure rolls back the dedup row, so redelivery
  still retries. Poison messages: unchanged, 10 deliveries then the broker's DLQ
  (`jms.queue.DLQ`; `_AMQ_ORIG_QUEUE` unchanged).
- **Evidence:** `InboundPathTest`, `InboundDedupPurgeTest`, `MessagingIntegrationTest` (duplicate
  ignored; DLQ after 10).

### O4 — Undeliverable public messages are dropped after 10 attempts
- **Decision:** ADR 002, ADR 013 (relay); `LISTING_OUTBOX_RELAY_MAX_ATTEMPTS`.
- **Before:** the processor sent `public.*` from its MDB transaction; a failure rolled back and
  `listing.event` redelivered, then dead-lettered.
- **After:** the relay retries an outbox row on each run (25 ms) up to 10 attempts, then logs ERROR
  and deletes it; there is no DLQ copy. Delivery is at-least-once (a crash after send republishes).
  Order is outbox id order through one deliverer across nodes.
- **Evidence:** `OutboxDelivererTest`, `OutboxRelayAndRoutesTest`.

### O5 — Court-list async steps are not durable
- **Decision:** ADR 015 P2.3.1 and P2.3.2 notes.
- **Before:** the export saga's steps were processor events: durable in the event store / outbox,
  retried by redelivery.
- **After:** after commit on the in-memory single-thread `courtListExecutor`, 10 immediate attempts,
  then ERROR. A crash between commit and the step loses it; "publish again" is the recovery, as
  for any failed export.
- **Evidence:** `AfterCommitTest`, `CourtListProcessorTest`.

### O6 — Per-instance advisory locks
- **Decision:** ADR 013 / ADR 015 P2.4 notes.
- **Before:** the event stream's optimistic version check; the framework retried a conflicting
  command.
- **After:** `pg_advisory_xact_lock` per hearing / case / application / seeding hearing before
  reading rules state. Concurrent work on one instance serialises. Opposite lock orders can
  deadlock: Postgres aborts one; an inbound message is redelivered, an HTTP command gets 500 and is
  not retried.
- **Evidence:** `RulesStateLocksTest`, `HearingStateStoreTest`.

### O7 — No backfill of aggregate-only state at cut-over
- **Decision:** D30 (out of scope), ADR 015 §4.
- **After:** `hearing_rules_state`, `case_rules_state`, `application_rules_state`,
  `seed_hearing_rules_state` and `publish_court_list_request` start empty for data created on JEE.
  Rules for those instances run on viewstore state plus empty aggregate-only state (e.g. no
  duplicate/deleted tombstone, no case hearing links). Must be resolved before a production
  cut-over.

### O8 — Liquibase
- **Decision:** D21.
- **After:** same changeset identities as JEE; new changesets `031`–`034` (`028`–`030` from Phase 1).
  On a JEE database the first start also runs two `017` `drop-process-event-index` changesets
  (drop two indexes on the unused `processed_event`). Liquibase 5 upgrades every stored checksum
  `8:` → `9:`; rolling back to the WAR (D6) needs `UPDATE databasechangelog SET md5sum = NULL`.
- **Evidence:** `LiquibaseChangelogIdentityTest`.

### O9 — Packaging, configuration, logging, audit
- **Decision:** ADR 007, D11, D12, D17, D24.
- **After:** a Boot jar in an `eclipse-temurin:25-jre`/`apm-services` image instead of the WAR on
  WildFly; config by `LISTING_*` env vars and `/mnt/secrets/rpe/` (no `standalone.xml`); JSON logs
  on stdout with `correlationId`/`requestId`; root level `WARN`, app `INFO`. Audit uses
  `cp-audit-filter-springboot` and is **off by default** (`LISTING_AUDIT_ENABLED`,
  `LISTING_HTTP_AUDIT_ENABLED`); set it per environment. `listing.messaging.enabled=false` turns
  off listener, relay and JMS health together.

### O10 — Build and IT tooling
- **Decision:** D4, D7, D13.
- **After:** Gradle replaces Maven. `./runIntegrationTests.sh` runs Spring ITs then the legacy suite
  (`spring` / `legacy` to pick one; `durations`, `nounit`, `nomidnightguard`); `errorlog` is gone;
  `CPP_DOCKER_DIR`/WildFly not needed. Persistence ITs and JMeter tests dropped (D7).

---

## Audited and unchanged

| Behaviour | Evidence |
|---|---|
| Context roots `/listing-service/{command,query}/api/rest/listing`; all paths, methods, media types, statuses (commands `202`, no body) | ADR 008; `CommandOperationsCoverageTest`; legacy suite |
| Error body `{"error":"<msg>"}` (several joined by `"; "`), incl. 401/403/404/405/406/415 | `EnvelopeAndErrorShapeIntegrationTest`; WildFly shape proven in courtscheduler `doc/breaking.md`; no listing legacy IT asserts an error body |
| `Accept` matching nothing → 406, unknown path 404, wrong method 405, bad `Content-Type` 415 | `RequestRoutingGuardFilter`; `EnvelopeAndErrorShapeIntegrationTest` |
| Schema-invalid vendor body → 400, `error` = `JSON schema validation has failed: …` plus the framework's `validationErrors` tree; everit draft-04 | `RequestSchemaValidationEndToEndIT`, `EnvelopeAndErrorShapeIntegrationTest` |
| JEE command-API mappers: split hearing 409 → 422 (`HEARING_NOT_SPLITTABLE` unless progression gave a code), crown multi-day extension, change court room for multi-day, move hearing to past date | `SplitHearingRejectedHandlerTest`, `CommandExceptionHandlerTest`; `SplitHearingIT` |
| Query responses payload-only with a `CPPID` header (envelope off) | `EnvelopeEnabledTest`, `EnvelopeAndErrorShapeIntegrationTest` |
| `sessionEndDate` optional on hearing slots; courtscheduler's non-JSON error passed through | D33; `HearingSlotsIT` 6/6 |
| Trailing slash ignored | `WebPlumbingTest` |
| RAML response-schema defects kept (D20) | `QueryEndpointsIntegrationTest` documented workarounds |
| Four access-control bypasses kept: hearing slots, CSV report, `/sessionAvailabilityValidation`, `/courtScheduleDraftStatus` (D18) | `application-authz.yml`; legacy `HearingSlotsIT`, `SessionAvailabilityValidationIT`, `CourtScheduleDraftStatusIT` |
| Group lists per rule; the four RAML-only `POST /hearings/{hearingId}` media types still denied (D34) | `AccessControlRulesTest` |
| JMS wire format: `TextMessage`, compact envelope, `CPPNAME` only, persistent / priority 4, `jms.topic.public.event` (D14, D28) | `EnvelopeCodecTest`, `MessagingIntegrationTest` |
| Inbound subscription name and selector string; DLQ after 10 (D15, D27) | `PublicEventSubscriptionTest`, `MessagingIntegrationTest` |
| Public message names, payloads, conditions and order per hearing | legacy suite; `PublicMessageContractTest` |
| Public payloads valid against the legacy public schemas: 28 of the 34 names validated on every message the legacy run published; 6 not checkable (no legacy schema: `court-application-added-for-hearing`, `hearing-requested-for-listing`; never published by the legacy suite: `events.listing.hearing-deleted`, `hearing-unallocated`, `offences-removed-from-unallocated-hearing`, `cases-added-to-hearing`). One **legacy** deviation kept: `public.events.listing.offences-removed-from-allocated-hearing` carries `isResultFlow`, which its schema (`additionalProperties: false`) forbids — the JEE processor copied every private-event key except `sourceContext` (same code at `b932de14b`); tolerated by exact message only | `PublicMessageContractTest` (`KNOWN_LEGACY_DEVIATIONS`), final run `it-logs/run-20261119T233230Z-p2.6-final-runIntegrationTests.log` |
| `ZonedDateTime` format and zone (D35) | `FrameworkDateFormatTest` |
| Outbound paths, media types and user id (system user only where JEE used `requestAsAdmin`, D19) | ADR 012 §3; client tests in `listing-common-spring` |
| XHIBIT WebDAV request (one `PUT`, preemptive Basic, `Expect: 100-continue`) and file content | `XhibitServiceTest`, mapper tests |
| Inbound schema check: the framework only logged failures at DEBUG; not ported, no effect | ADR 013 findings |
