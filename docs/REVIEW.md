# Migration code-review companion

Context for reviewers: this is the MBD refactor of `cpp-context-listing` from a WildFly WAR built
on the HMCTS/CJS framework (CQRS + event sourcing, RAML, DeltaSpike, Maven, Java 17) to Spring
Boot 4.1 on Java 25, built with Gradle. Work is on branch `mbd-refactor`:

| Commit | Content |
|---|---|
| `70296118b`, `740c7ce34` | Phase 1: event store, aggregates' event streams and command handler queue removed while still on JEE; transactional outbox (ADR 002, 009, 010, 011) |
| `756db7a84` | P2.0 Spring Boot foundations, P2.1 persistence and all queries (ADR 005, 007, 008, 012) |
| `976471b69` | P2.2 messaging: envelope, outbox relay, inbound listener, dedup, DLQ (ADR 013) |
| `cee3ca451` | P2.3.0 legacy IT harness on Spring, command infrastructure, listing notes (ADR 014, superseded) |
| `b932de14b` | P2.3.1–P2.4 every command and every inbound event on Spring; no aggregates (ADR 015) |
| P2.6 (this change) | JEE modules, Maven build and framework descriptors deleted; sources moved into the Gradle modules |

The structure copies `cpp-context-listing-courtscheduler` (its `doc/REVIEW.md` is the model for
this file). The instructions for the work are `docs/MBD_REFACTOR_PROMPT.md` (invariants §4,
decisions D1–D35 in §14, open items §15). Contract-visible differences are in
[breaking.md](breaking.md); this file gives the per-module rationale and the fixes the diff does not
explain on its own.

**Parity evidence.** The legacy `*IT` suite (60 classes) running against the Spring app:
335 run / 0 failed / 3 skipped, the same as the JEE baseline (`it-logs/run-20261119T223718Z-p2-final-legacy.log`;
JEE: `it-logs/run-20261119T001211Z.log`). Spring ITs 90/90 (`it-logs/run-20261119T223718Z-p2-final-spring.log`).
No scenario, input or assertion of the legacy suite was changed in Phase 2 (only harness classes,
§9 / D1); the three Phase 1 assertion changes are D10.

**Design in one paragraph (ADR 015, D29).** The Spring app keeps no aggregates, no private events
and no event log. A command checks the ported aggregate rules against viewstore state, writes the
viewstore as the JEE event listener did, and writes the public messages the JEE event processor
emitted to an outbox table, all in one transaction. A relay publishes the outbox to Artemis
`public.event`. Inbound `public.*` messages are deduplicated and call the same command services.
State that only the aggregates held lives in additive tables (changesets 032–034).

---

## 1. Files added — what and why

Base package `uk.gov.moj.cpp.listing` unless stated. Module names keep the `-spring` suffix for now
(ADR 012 §1); dropping it is a rename only.

### 1.1 Build and packaging

| File | Why |
|---|---|
| `settings.gradle`, `build.gradle` (root) | Gradle multi-project replacing the Maven reactor. Java 25 toolchain, Boot 4.1 / Testcontainers / JUnit 6 BOMs, Lombok, PMD only when named, `Europe/London` test timezone, reproducible archives (ADR 007 §1, copied from courtscheduler). |
| `gradle.properties` | Version pins (`cpAuthFilterVersion` 1.0.9, `cpAuditFilterVersion` 1.0.5, everit 1.14.6, …) and the Sonar exclusions ported from the root `pom.xml`. |
| `gradlew*`, `gradle/wrapper/*` | Wrapper (Gradle 9.6.1). |
| `Dockerfile`, `docker/startup.sh` | Root image on `eclipse-temurin:25-jre` (CI: `hmcts/apm-services:25-jre`), non-root user, `startup.sh` picks the non-`plain` jar and logs in JSON. Replaces `docker/Dockerfile_listing-service` (WildFly). |
| `listing-api/docker/Dockerfile` | Image for the IT compose stack. |
| `runIntegrationTests.sh` (rewritten) | Thin Gradle wrapper: unit tests (skip with `nounit`), Spring ITs, then the legacy suite and `publicMessageContractTest`; `spring` / `legacy` pick one; `durations`, `nounit`, `nomidnightguard` kept; `errorlog` dropped (no WildFly `server.log`); runs under `caffeinate -dims` on macOS because a host sleep mid-run lost the harness's JMS connection (P2.3.2 note in §10.3). Absorbs `runSpringIntegrationTests.sh`. |

### 1.2 `listing-api` — the Boot application

| Area / file | Why |
|---|---|
| `Application.java`, `application.yaml` + `application-{datasource,server-management,authz,audit,messaging,xhibit}.yml`, `application-integration.yaml` | Config fragments as in courtscheduler; every value `${LISTING_*:default}` (D12). Context path `/listing-service` serves both legacy roots from one app (ADR 008, D11). |
| `src/main/resources/openapi/listing-api.openapi.yml` | One spec ported 1:1 from both RAML files: paths, methods, query parameters, vendor media types, statuses. Bodies are `PassthroughObject`; the RAML JSON schemas stay the validation source (`x-json-schema`). Generator config as courtscheduler (`interfaceOnly`, `apiNameSuffix OpenApi`). |
| `src/raml/{command,query}/`, `src/listing-json/` | The RAML files and their schemas, moved from the deleted Maven modules; `processResources` copies the schemas to `request-schemas/` on the classpath. Kept as the contract source and for the IT response-schema checks. |
| `request-schemas/catalog/vendored-core-domain/` | 100 core-domain schemas the RAML `$ref`s reach, vendored so CI needs no schema jars (D22). |
| `controllers/*QueriesApi` (6) | All 17 query paths / 18 names; implement the generated `*OpenApi` interfaces; `QueryParameters` builds the payload exactly as the framework REST adapter (absent parameters left out, RAML types kept). |
| `controllers/*CommandsApi` (5), `CommandRequests`, `HearingCommandRequests` | All 30 command media types, `202` with no body. `CommandRequests` rebuilds the framework `RestEnvelopeBuilder` metadata (new id, action name, `CJSCPPUID`, `CPPSID`, `CPPCLIENTCORRELATIONID`, `CPPCAUSATION`) and merges path parameters into the payload; shared paths dispatch on `Content-Type` (ADR 014 §5). |
| `controllers/GlobalExceptionHandler`, `LegacyErrorAttributes`, `ValidationFailedException` | Every 4xx/5xx is `{"error":"<msg>"}` (messages joined with `"; "`), including the auth filter's `sendError` 401/403. The four JEE command-API exception mappers (crown multi-day extension, change court room for multi-day, move hearing to past date, split hearing 409 → 422) are reproduced with their own bodies. |
| `validation/RequestSchemaValidationFilter`, `CachedBodyHttpServletRequest`, `RequestSchemaRegistry`, `LegacySchemaCatalog`, `ValidationErrorsRenderer` | everit draft-04 validation of vendor `+json` bodies (the framework's library), order +20, before authz as the JAX-RS reader interceptor ran. 400 body has `error` plus the framework's `validationErrors` tree (ADR 007 §6). |
| `validation/RequestRoutingGuardFilter` (order +15, new) | Resolves the Spring handler before the auth filter so unknown path / method / `Content-Type` / `Accept` give 404/405/415/406 as on JAX-RS, not the 403 that `deny-when-no-rules` would give (ADR 005 §6). |
| `validation/AuthzActionGuardFilter` (order +10, from courtscheduler) | Hides `CPP-ACTION` and rejects more than one vendor type in `Accept` (406) so a client cannot choose the DRL action that authorises it. |
| `uk.gov.moj.cpp.listing.api.accesscontrol.drl/` (2 DRL files), `api/accesscontrol/RuleConstants` | The JEE rules ported to `cp-auth-rules-filter` 1.0.9 with group lists unchanged; `Action.name` is the vendor token; seven queries sharing `listing.search.hearings` also match `attributes["path"]`; `isSystemUser` → `isMemberOfAnyOfTheSuppliedGroups(…, "System Users")` (ADR 005). |
| `application-authz.yml` | `deny-when-no-rules: true`; the four legacy bypasses in `exclude-path-prefixes` (D18). |
| `config/AuditFilterConfig`, `application-audit.yml` | `cp-audit-filter-springboot` replaces the framework audit interceptor. |
| `config/ActionMetricsInterceptor` | `cpp.action.total` / `cpp.action.individual` replacing the framework metrics interceptors. |
| `config/JacksonObjectMapperConfig`, `JakartaJsonModule`, `JsonValueConverter` | `jakarta.json` values serialise natively, not as beans. |
| `config/CorsConfig`, `PrimaryHandlerMappingConfig` | As courtscheduler. |
| `config/TrailingSlashConfig` | `/hearings/?…` is served as RESTEasy did (found by the legacy suite, §3.5). |
| `config/TracingFilter`, `logback.xml` | JSON logs to stdout with `correlationId` / `requestId` MDC (D24, `context/logging-standards.md`). |
| `envelope/EnvelopeResponseBodyAdvice`, `JsonEnvelopeWrapper`, `SkipEnvelope` | Listing's query resources returned the payload only with a `CPPID` header, so `listing.envelope.enabled` defaults to `false` and 2xx JSON gets `CPPID` (ADR 007 §5). |

### 1.3 `listing-domain-spring`, `listing-domain-event-spring`

| File | Why |
|---|---|
| `listing-domain-spring` (`domain/…`, `common/domain/DomainEvent`) | Framework-free domain POJOs and JSON helpers shared by every module; `ObjectMapperProducer` equivalent producing the framework's date format (D35). |
| `listing-domain-event-spring/src/generated/java` (560 classes), `refresh-generated-sources.sh` | The framework `pojo-generation-plugin` output (event, command and core-domain POJOs) vendored, because the plugin has no Gradle port and jsonschema2pojo generates different classes. Used only as payload DTOs; `@Event` is stripped so `framework-api-domain` is off the classpath (D32). The script regenerates them offline. |

### 1.4 `listing-viewstore-persistence-spring` and `listing-viewstore-liquibase`

| File | Why |
|---|---|
| `persistence/entity/*` (13 entities) | DeltaSpike entities on Hibernate 7. `jsonb` via an `AttributeConverter` to Jackson 2 `JsonNode` + `@ColumnTransformer(write="?::jsonb")`; standard (not snake_case) naming; `NORMALIZE_UTC` (ADR 012 §2). |
| `persistence/repository/*` | Spring Data interfaces in the DeltaSpike style (method names + `@Query`, `default` aliases), JDBC repositories on `JdbcTemplate` for the native court-list queries, plus the new JDBC repositories: `outbox/EventOutboxJdbcRepository`, `inbound/InboundEventDedupJdbcRepository`, `hearing/RulesStateJdbcRepository`, `PublishCourtListRequestRepository`. |
| `listing-viewstore-liquibase/` (moved to the top level) | The JEE changelogs, unchanged, as a resources-only project, `runtimeOnly` in listing-api. Changeset identities are unchanged (D21, §3.1). |

New changesets, all additive:

| Changeset | Table | Purpose |
|---|---|---|
| `028`, `029` | `event_outbox` (+ `attempts`) | Transactional outbox (Phase 1, ADR 002); used by the Spring relay. |
| `030` | `aggregate_snapshot` | Phase 1 only. Unused by the Spring app (ADR 015); kept so the JEE rollback path still works. |
| `031` | `inbound_event_dedup` | Inbound idempotency on `_metadata.id` (D16). |
| `032` | `publish_court_list_request` | `PublishCourtListRequestAggregate.weekCommencing`. |
| `033` | `hearing_rules_state` | Hearing aggregate-only state: `duplicate`/`deleted` tombstone + `state jsonb` (field map in ADR 015 P2.3.2). |
| `034` | `case_rules_state`, `application_rules_state`, `seed_hearing_rules_state` | Case, Application and SeedHearing aggregate state, one JSON document per former stream id. |

### 1.5 `listing-common-spring`

| Area | Why |
|---|---|
| `query/api`, `query/view`, `query/document` | The query side ported from `listing-query-api` / `-view` (416 unit tests ported with it); one read-only transaction per query because the ported code reads lazy collections after the repository call (ADR 012 §4). |
| `common/client/*` (`CppQueryClient`, courtscheduler, systemdocgenerator, progression, hearing, referencedata clients) | Spring `RestClient` on the JDK `HttpClient` replacing `Requester`. System user (`LISTING_SYSTEM_USER_ID`) only where JEE used `requestAsAdmin`, the caller's `CJSCPPUID` otherwise (D19); correlation id propagated (`CorrelationIdPropagationTest`). |
| `common/service`, `ListingNoteService`, `common/{crownfallback,courtroomchange,pastdate,progression,xhibit}` | Domain services and the exceptions the JEE command API mapped to specific statuses. |

### 1.6 `listing-messaging` (new, no courtscheduler equivalent)

| File | Why |
|---|---|
| `envelope/Metadata`, `Envelope`, `EnvelopeCodec` | Framework JSON envelope (`_metadata` first, payload at top level, nulls omitted). A message captured from the JEE app re-serialises byte for byte (`EnvelopeCodecTest`, `src/test/resources/legacy/`). |
| `outbox/EventOutbox` | `@Transactional(MANDATORY)` writer: rows commit with the caller's changes; accepts `public.*` names only; metadata convention of the framework (command metadata, new id, `createdAt`, command id appended to `causation`, `stream.id` = former aggregate id). |
| `outbox/OutboxRelay`, `OutboxDeliverer` | `@Scheduled` 25 ms relay; one entry per `REQUIRES_NEW` transaction under `pg_try_advisory_xact_lock` (one deliverer across nodes), oldest id first, delete by exact id, 10 attempts then ERROR and drop (ADR 002, 013). |
| `outbox/PublicEventPublisher`, `OutboxRoute` | `TextMessage`, single property `CPPNAME`, producer defaults, address `jms.topic.public.event` (D14, D28). |
| `inbound/PublicEventListener`, `PublicEventSubscription`, `InboundEventProcessor`, `InboundEventDispatcher`, `InboundEventHandler` | `@JmsListener` on the legacy durable shared subscription `listing.event.processor.public.event` with the generated selector string unchanged (D15), transacted session, concurrency 1–15; parse → dedup insert → dispatch by `_metadata.name`, all in one DB transaction. Start-up fails for a handler of an unsubscribed name or two handlers for one name. |
| `inbound/InboundDedupPurge` | Deletes dedup rows older than 30 days, daily, in batches under advisory lock `7340002` (D26). |
| `inbound/InboundTracing` | MDC `correlationId` / `requestId` per JMS message (D24). |
| `config/MessagingConfiguration`, `MessagingProperties`, `ArtemisBrokerUrl` | Own Artemis connection factory from `listing.messaging.artemis.*` (`LISTING_ARTEMIS_*`, the `cp.audit.*` key model, HA, SSL); `listing.messaging.enabled` switches listener, relay and JMS health together (D17). |
| `src/contract/` (`json/`, `subscriptions-descriptor.yaml`, `public-publications-descriptor.yaml`) | The processor's public schemas and descriptors, moved from the deleted `listing-event-processor`. The contract the outbox and listener must keep; read by `PublicMessageContractTest`. |

### 1.7 `listing-command-spring` — commands and inbound handlers

| Package | Why |
|---|---|
| `command.hearing.rules` (`HearingRules`, `CaseRules`, `ApplicationRules`, `SeedHearingRules`, value classes, converters) | The JEE aggregates ported verbatim as framework-free rules classes (`Aggregate`/`EventSwitcher` replaced by a local `EventSwitch`). 221 ported `HearingAggregateTest` cases plus per-rule tests. Every guard logs and no-ops as before. |
| `command.hearing.state` (`HearingStateStore`, `RulesStateStore`, `HearingRulesStateCodec`) | Restore rules state from `hearing.properties` + the 033/034 tables; take the per-instance advisory lock first (§3.8). |
| `command.hearing.flow` (`HearingFlow`, `ListenerPort`, `ProcessorPort`, `ProcessorQueue`, `ProcessorSender`, `InternalCommands`, `RaisedEvent`) | The JEE `updateHearingEventStream` path without an event store: raised events live in memory only, each goes through the listener port (viewstore write) and queues its processor step; `ProcessorQueue` runs steps breadth-first in raised order, so the order of public messages is the Phase 1 relay order. |
| `command.hearing.listener` (26 classes, 71 handlers) | The JEE event-listener classes, verbatim, writing the same rows, columns and jsonb. |
| `command.hearing.processor` (53 handlers + factories) | The JEE processor's private-event handlers; internal `listing.command.*` sends become direct calls; public messages go to the outbox. |
| `command.hearing.handler` (`HearingCommands`, `CaseCommands`, `NextHearingCommands`, handler classes) | The 72 internal handler commands, one `@Transactional` method each. |
| `command.hearing.api` (`HearingCommandApi`, enrichment services) | The JEE command API including the enrichment calls (courtscheduler, refdata, PTPH, progression split), run outside the DB transaction as before. |
| `command.courtlist` (`CourtListCommands`, `CourtListViewstore`, `CourtListProcessor`, `CourtListPublicMessages`) | Court-list / XHIBIT saga as a service pipeline; async steps after commit (§3.9). |
| `command.note` (`ListingNoteCommands`) | Listing notes slice. |
| `command.support` (`AfterCommit`, `CommandSupportConfiguration`) | After-commit steps on the single-threaded FIFO `courtListExecutor`, each in its own transaction, 10 attempts. |
| `command.inbound` (7 processors + `InboundProcessor`) | One handler per subscribed name (28), each the JEE `@Handles` method unchanged, calling the command services in the listener transaction (`InboundHandlerRegistrationTest`: handled names = selector list). |

### 1.8 `listing-xhibit` (new)

| File | Why |
|---|---|
| `src/main/xsd/` | The XHIBIT XSDs (moved from the JEE module). JAXB classes generated with the same XJC 2.3.7 and arguments as `maven-jaxb2-plugin`, rewritten to `jakarta.xml.bind` (identical class set and namespace prefixes). |
| `xhibit/courtlist/*`, `mapper/*` | Mappers, metadata and file generators, XSD validation, ported unchanged; the 49 JEE tests pass against the same expected XML. |
| `xhibit/webdav/XhibitService`, `XhibitWebDavProperties` | One HTTP/1.1 `PUT <outbound-url>/<file>` with preemptive Basic auth and `Expect: 100-continue` on the JDK `HttpClient`, the request Sardine 5.7 made; Sardine needs `javax.xml.bind` and Apache HttpClient 4. Same error messages (`XhibitServiceTest`). Config `listing.xhibit.webdav.*` (`LISTING_WEBDAV_*`). |

### 1.9 Integration-test modules

| Module | Why |
|---|---|
| `listing-integration-test-spring` | New Spring-style ITs on a compose stack (Postgres 16, WireMock, Artemis, app): smoke, query endpoints (with RAML response-schema checks, ADR 012 §5), authorisation allow/deny, error shape and envelope, request schema validation, messaging wire format / dedup / DLQ, one IT per command slice. `docker/artemis/broker.properties` states the DLQ settings (10 attempts, `DLQ`). |
| `listing-integration-test-legacy` | The 60 legacy `*IT` classes (moved in from the Maven `listing-integration-test`) run unchanged against the Spring stack. Harness-only overrides with the same FQCN (`legacyharness/`, ADR 014 §6): `BaseUriProvider`, `QueueUriProvider`, `TestJdbcConnectionProvider`, `DatabaseCleaner` (no event store; drains `event_outbox`), `ArtemisQueuePurger`, `WireMockStubUtils`, `LegacyHarnessExtension`; a `haproxy` gateway reproduces cpp-developers-docker's rewrite rules. Generated IT POJOs vendored (`src/generated/java`, `refresh-legacy-inputs.sh`). |
| `PublicMessageContractTest` + `PublicMessageRecorder` (legacy module) | The recorder (a JUnit Platform listener, harness only) subscribes to `public.event` for the whole run; the test validates every `public.*` message recorded during the legacy run against `listing-messaging/src/contract/` schemas, one dynamic test per message name (invariant §4.4). |

### 1.10 Documentation

`docs/pipeline/adrs/` 002, 005, 007–015; `docs/MBD_REFACTOR_PROMPT.md` status and decisions;
`docs/listing-model/` (PlantUML diagrams moved from `listing-model`); this file and `breaking.md`.

---

## 2. Files deleted — why each can go

### 2.1 Maven build

- Root `pom.xml` and every module `pom.xml`. Gradle builds everything (D4).
- `listing-integration-test-persistence` and `listing-performance-test` (DeltaSpike persistence ITs,
  JMeter): dropped by decision D7; the Spring repositories have Testcontainers tests
  (`listing-viewstore-persistence-spring`, 40 tests incl. `LiquibaseChangelogIdentityTest`).

### 2.2 JEE modules (logic ported, see §1)

| Deleted | Replaced by |
|---|---|
| `listing-command` (`-api`, `-handler`) | `listing-api` controllers, `listing-command-spring` (`api`, `handler`, `rules`). The command-handler queue `listing.handler.command` is gone (synchronous commands, D5). |
| `listing-domain` (`-aggregate`, `-common`, `-event`, `-xhibit`) | Aggregates → `command.hearing.rules` (ADR 015); events → vendored payload DTOs in `listing-domain-event-spring`; common → `listing-domain-spring`; XSDs → `listing-xhibit`. `listing-domain-aggregate-spring` was already removed at P2.3.1. |
| `listing-event` (`-listener`, `-processor`) | `command.hearing.listener`, `command.hearing.processor`, `command.inbound`; schemas and descriptors → `listing-messaging/src/contract/`. |
| `listing-event-sources` | Event-source / event-store config; there is no event store. |
| `listing-query` (`-api`, `-view`) | `listing-common-spring` `query.*` + `listing-api` controllers. |
| `listing-viewstore` (Maven part, `-persistence`) | `listing-viewstore-persistence-spring`; Liquibase moved to `listing-viewstore-liquibase/`. |
| `listing-common`, `listing-json` | `listing-common-spring`, `listing-domain-spring`; schemas → `listing-api/src/listing-json/`. |
| `listing-healthchecks` | Actuator health (db, JMS when messaging is on, liveness/readiness). |
| `listing-service` (the WAR) | `listing-api` `bootJar`. |
| `listing-selective-catchup` | Catch-up/replay has no meaning without an event store (§15.8). |
| `listing-integration-test` | Moved into `listing-integration-test-legacy` (sources and resources). |
| `listing-model` | Moved to `docs/listing-model/` (diagrams only). |

### 2.3 Framework descriptors

`META-INF/beans.xml`, `persistence.xml`, `apache-deltaspike.properties` and the other `META-INF/*`
framework descriptors of the deleted modules. Spring uses component scanning and
auto-configuration; `spring.liquibase.change-log` is set explicitly.

### 2.4 Scripts and WildFly files

| Deleted | Why |
|---|---|
| `standalone-listing.xml`, `docker/Dockerfile_listing-service`, `docker/scripts/liquibase.sh` | WildFly config and image; Liquibase runs in the app (`LISTING_LIQUIBASE_ENABLED`). |
| `quickredeploy.sh` | Redeployed the WAR into the WildFly container. |
| `runSystemCommand.sh` | JMX framework system commands (CATCHUP, …); nothing to run (§15.8, breaking.md O2). |
| `run-it-10x.sh`, `run-it-midnight.sh` | Maven-based IT loops; use `runIntegrationTests.sh`. |
| `runSpringIntegrationTests.sh` | Merged into `runIntegrationTests.sh` (D13 ends with P2.6). |

### 2.5 Earlier-phase scaffolding (already gone before P2.6)

`P20SkeletonApi` (P2.0 canned endpoints), `P22MessagingProbeHandler` (P2.2 test handler),
`AggregateStore`, `AggregateSnapshotJdbcRepository`, `EventRecorder`, `EventProjection(s)`,
`PrivateEventHandler`, `InProcessEventRoute`, `processor.deferred.DeferredCommands` (ADR 014 → 015).

---

## 3. Non-obvious fixes

Each item names the class or test that guards it.

### 3.1 Liquibase changelog identity (D21)

Risk: Spring writing different `DATABASECHANGELOG` (id, author, filename) rows would re-run every
changeset at cut-over. Checked against a JEE-deployed viewstore: identical, including the 9
changesets without `logicalFilePath`. On a copy of the JEE schema Spring ran only the new
changesets (then `031`) plus two `017` `drop-process-event-index` changesets (`onFail=CONTINUE`; JEE ran them before the
framework created `processed_event`). Liquibase 5 rewrites stored checksums `8:` → `9:`; rolling
back to the WAR then needs `UPDATE databasechangelog SET md5sum = NULL`.
Guard: `LiquibaseChangelogIdentityTest` against `liquibase-identity/jee-databasechangelog.psv`.

### 3.2 `ZonedDateTime` (D35)

Serialised strings were already the framework's (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`, UTC), but
deserialised values carried zone `Z` where the framework's Jackson 2.12 gave the region zone `UTC`,
which changed `equals` and some derived output. `JavaTimeFeature.NORMALIZE_DESERIALIZED_ZONE_ID` is
disabled. Guard: `FrameworkDateFormatTest`.

### 3.3 `sessionEndDate` and controller parameter order (D33)

Making `sessionEndDate` optional in the spec (the legacy adapter never enforced it) moved it after
the required parameters in the generated `searchHearingSlots` signature; the controller kept the
old order, so values bound to the wrong names (all three `HearingSlotsIT` failures). Fixed, and a
non-JSON error body from courtscheduler is passed through raw as the JAX-RS adapter did.
Guard: `ControllerParameterOrderTest` (every controller parameter carries the name the generated
method binds at that position).

### 3.4 Every command media type is served

`CommandOperationsCoverageTest` walks the spec so no command media type answers 404. The four
RAML-only `POST /hearings/{hearingId}` media types (D34) pass through to the command services, but
have no DRL rule, so end users still get 403 as on WildFly (`AccessControlRulesTest`,
`HearingCommandsControllersTest`).

### 3.5 Trailing slash

The legacy suite calls `/hearings/?…`; RESTEasy ignored the slash, Spring 6+ does not.
`TrailingSlashConfig`; guard `WebPlumbingTest.aTrailingSlashIsIgnoredAsOnWildFly`.

### 3.6 Rollback-only on update-hearings-for-listing

The handler catches "hearing not found" per hearing and carries on. The query view is
`@Transactional`, so its `QueryNotFoundException` crossing the proxy marked the command's
transaction rollback-only → `UnexpectedRollbackException` → 500 (pitfall §11.2, found by the
targeted legacy run of `ListCourtHearingIT`, `HearingIT`, `VacateHearingIT`). `HearingService` checks existence before calling the view.
Same family: `OutboxDeliverer` catches the delivery failure inside its own `REQUIRES_NEW` transaction
so the attempt count commits.

### 3.7 Dedup and outbox writes without exception-driven upserts

`inbound_event_dedup` is `INSERT … ON CONFLICT (event_id) DO NOTHING` in the listener transaction,
never "try insert, catch violation". A handler failure rolls back the dedup row too, so the broker's
redelivery is processed. Guards: `InboundPathTest`, `InboundEventDedupJdbcRepositoryTest`,
`MessagingIntegrationTest` (duplicate ignored).

### 3.8 Per-instance serialisation with advisory locks (P2.4)

The JEE event stream's version check serialised commands on one aggregate. Now every rules-state
load takes `pg_advisory_xact_lock(namespace, hash(id))` first (hearing / case / application /
seeding-hearing namespaces). Chosen over `SELECT … FOR UPDATE` / `@Version` because the first
command on an instance has no row to lock (two hearings of one case listed together both create
`case_rules_state`). Caveats: opposite lock orders can deadlock (Postgres aborts one; inbound is
redelivered, HTTP gets 500); reads before the first lock may be stale, so the CPS-prosecutor handler
clears the persistence context after its search. Guard: `RulesStateLocksTest`.

### 3.9 Court-list steps after commit

JEE ran "store the list, request the export" and "render, PUT, record, publish" as processor steps
in outbox order. `AfterCommit` runs them on a one-thread FIFO `courtListExecutor`, each in its own
transaction, which keeps the order crown-court publishing needs (every centre stored before any
export). A failing step is retried up to 10 times (the relay's count) and then logged at ERROR.
Guards: `AfterCommitTest`, `CourtListProcessorTest`, `CourtListCommandIntegrationTest`.

### 3.10 Relay lock and ordering

One deliverer across nodes via `pg_try_advisory_xact_lock` (key `7340001`, the Phase 1 key);
oldest id first; delete by exact id so a late-committing lower id is never lost; a failed entry
waits one relay delay. Guards: `OutboxDelivererTest`, `OutboxRelayAndRoutesTest`.

### 3.11 Reusing the legacy subscription and selector (D15)

The listener uses the same durable shared subscription name and the generated selector string
byte for byte. A different filter on an existing shared subscription is rejected or recreates the
queue, losing the backlog at cut-over. Artemis escapes the dots in the queue name
(`listing\.event\.processor\.public\.event`); DLQ'd messages keep that `_AMQ_ORIG_QUEUE`.
Guards: `PublicEventSubscriptionTest`, `MessagingIntegrationTest` (DLQ after 10).

### 3.12 Health when messaging is disabled

With JMS off the JMS health contributor reported DOWN. `listing.messaging.enabled` now also drives
`management.health.jms.enabled`. Guard: `MessagingSwitchTest`.

### 3.13 `javax.xml.bind:jaxb-api` excluded

`liquibase-core` declares it; no runtime class references it, and it would put `javax.*` on the
runtime classpath (§12). Excluded in `listing-api/build.gradle`.

### 3.14 Generated POJOs and serialVersionUIDs

The WAR held four generated copies of many POJOs with different serialVersionUIDs; WildFly loaded
whichever jar came first in directory order. P2.3.0 read Phase 1 snapshots tolerating a UID
difference when the field layout was identical. ADR 015 removed snapshots, so the tolerance is gone
with `AggregateStore`; what remains is that `refresh-generated-sources.sh` output must be diffed on
refresh (UIDs and `@Event` stripping).

### 3.15 Hibernate 7 behaviour kept as DeltaSpike's

- New `Hearing` rows must be `persist`ed: Spring Data `save` (merge) re-reads the row and selects
  native-query-only columns. `HearingRepository.save` keeps DeltaSpike's semantics.
- Two native court-list queries got typed casts (`… as id`, `:courtCentreId as court_centre_id`)
  because Hibernate 7 no longer coerces text to UUID. Guards: `HearingRepositoryTest`, court-list
  repository tests.

### 3.16 Rules state for a never-listed hearing

`mark-hearing-as-duplicate` for an unknown hearing produced rules state without a hearing id (null
PK). The row is keyed by the stream id, as the JEE stream was. Guard: `HearingStateStoreTest`.

### 3.17 Public message metadata

`EventOutbox.envelopeFor(command, …)` reproduces the framework's derivation (command metadata, new
id, `createdAt`, causation, `stream.id`). Only `_metadata.id` differs: the JEE public message reused
the private event's id, which no longer exists (breaking.md W1). Guards: `EventOutboxTest`,
`PublicMessageContractTest`.

### 3.18 Error shape for every status

Courtscheduler's catch-all turned 404/405/415/406 into 500 and the auth filter's `sendError` bodies
were Spring's default. Added handlers, `RequestRoutingGuardFilter` and `LegacyErrorAttributes`.
Every error response is written as `application/json` whatever the `Accept`, otherwise writing the
error body itself fails and a 406 becomes a 500. Guard: `EnvelopeAndErrorShapeIntegrationTest`.

### 3.19 Kept JEE bugs (parity, not fixed)

- `SeedHearingAggregate:300` removes by the wrong key (pinned by `RulesStateStoreTest`).
- Export outcome for a never-stored court list fails the step; a REST court-list export without
  `sendNotificationToParties` fails building `court-list-published` (ADR 015 P2.3.1).
- RAML response-schema defects (D20, listed in §10.3 P2.1).

---

## 4. Out of scope but worth flagging

- **Cut-over state (D30).** No backfill of `hearing_rules_state` / 034 tables from `event_log`.
  Hearings, cases and applications created on JEE start with no rules state on Spring. Decide before
  production (breaking.md O7).
- **Two bypasses of unclear intent.** `/sessionAvailabilityValidation` and `/courtScheduleDraftStatus`
  never ran access control on WildFly and still do not (ADR 005 open question).
- **usersgroups source (§15.4, §15.15b).** The auth filter reads `…/usersgroups-query-api/…/logged-in-user/permissions`;
  the framework read groups by user from `usersgroups-service`. Confirm group names match and the
  gateway serves the path.
- **cp-auth-rules-filter builds a KieBase per request** (D25, §15.14). Measure before production.
- **Advisory-lock deadlocks** (§3.8) give an HTTP 500 that JEE would have retried.
- **Dedup retention** 30 days: a manual replay older than that is processed again.
- **Audit** (`LISTING_AUDIT_ENABLED`, `LISTING_HTTP_AUDIT_ENABLED`) defaults to off; set per
  environment.
- **Deployment** (§15.7): Helm chart and pipeline for a jar; ADO vs GitHub Actions.
- **Jira epic** (§15.1) missing.

---

## 5. Reviewer checklist

- [ ] Every deleted file matches §2; every new module or area matches §1.
- [ ] `./gradlew build` green; `./runIntegrationTests.sh` green: Spring ITs and the legacy suite at
      335 run / 0 failed / 3 skipped. No legacy IT scenario, input or assertion edited (harness only).
- [ ] Liquibase: new changesets are additive only (create table / add nullable column), numbered
      after `034`, in `listing-viewstore-liquibase`; no edit to an existing changeset (checksums,
      `LiquibaseChangelogIdentityTest`).
- [ ] Every vendor media type in `listing-api.openapi.yml` has a DRL rule matching its **token**
      (path-qualified where tokens are shared) and every rule has an endpoint
      (`AccessControlRulesTest`). Group lists unchanged; the four bypasses unchanged (D18).
- [ ] A new endpoint keeps the generated parameter order (`ControllerParameterOrderTest`) and every
      command media type is served (`CommandOperationsCoverageTest`).
- [ ] Every `@ExceptionHandler` returns `{"error":…}` as `application/json`, except the four JEE
      mapper equivalents.
- [ ] Public messages: names, payloads and conditions as the JEE processor; written with
      `EventOutbox` inside the command's transaction (`MANDATORY`), never sent to JMS directly;
      schema-valid (`PublicMessageContractTest`); `CPPNAME` is the only JMS property.
- [ ] Inbound: one handler per subscribed name (`InboundHandlerRegistrationTest`); subscription
      name and selector string unchanged (`PublicEventSubscriptionTest`); dedup row in the same
      transaction as the handler's writes.
- [ ] No "try insert / catch constraint violation" in a shared transaction; use `ON CONFLICT` or
      `REQUIRES_NEW` (pitfall §11.2). Exceptions caught inside a transaction must not cross a
      `@Transactional` proxy (§3.6).
- [ ] Rules-state loads go through `HearingStateStore` / `RulesStateStore` (advisory lock first).
- [ ] Every ported aggregate guard has a unit test, including log-and-no-op guards.
- [ ] No `uk.gov.justice.services.*`, `javax.*`, DeltaSpike, CDI or WildFly artefacts on the runtime
      classpath (`./gradlew :listing-api:dependencies --configuration runtimeClasspath`); no new
      `META-INF` descriptors.
- [ ] JSON logging to stdout (`logback.xml`), MDC `correlationId`/`requestId` for HTTP and JMS.
- [ ] Every setting is `${LISTING_*:default}`; no connection strings, keys or passwords with
      non-local defaults.
- [ ] `Europe/London` test timezone respected; no mixing of `systemDefault()` with fixed zones.
- [ ] ADRs 011–015 and §14 match the code; anything new recorded in breaking.md.
