# Prompt — Refactor `cpp-context-listing` (JEE / CQRS-ES framework) → Spring Boot

> Status: DRAFT v0.3. Core decisions are in §14 and the remaining open items in §15.
> v0.3 adds the reference-project blueprint (§2, §6, §11), based on a survey of
> `cpp-context-listing-courtscheduler` done on 2026-11-17.
> Branch: `mbd-refactor`

---

## 1. Role & pipeline

You are refactoring `cpp-context-listing` from a WildFly WAR built on the HMCTS/CJS microservice
framework (CQRS + event sourcing, RAML, Drools access control, DeltaSpike, private JMS topics) into a
**Spring Boot application**. Follow the conventions of the reference project
`/Users/hasanshaik/cpp/github/cpp-context-listing-courtscheduler` (see §2).

Follow the HMCTS SDLC pipeline in `CLAUDE.md`, halting at every human gate. The stages are
Requirements → Architecture → Stories → Test Specs → Code → Review → Build & Test → Deploy.
Load the context files listed in `CLAUDE.md`. Write an ADR for every non-trivial decision in §7.
Before writing any code, export an implementation-plan HTML artifact via
`skills/export-design-artifact/`.

**Reference project vs HMCTS template.** `CLAUDE.md` makes `service-hmcts-crime-springboot-template`
(via `skills/springboot-service-from-template/`) the master source for build files, Dockerfile and
logback config. Courtscheduler is the proven pattern for this kind of port. Where the two differ,
follow courtscheduler and record each difference in an ADR (§7, ADR 7). Do not hand-scaffold
anything that neither of them provides.

## 2. Reference project — `cpp-context-listing-courtscheduler`

Courtscheduler is the **same kind of migration, already done and in production**. It is a sibling
CPP Listing service that was ported from WildFly + Justice Services + Maven WAR to
Spring Boot 4 / Java 25 / Gradle, under the same "drop-in replacement for clients" rule.
Treat it as the blueprint. When this prompt and courtscheduler disagree on a detail, this prompt
wins. Otherwise, copy courtscheduler.

**Read these before Stage 2 (Architecture):**

| File (relative to courtscheduler root) | Why |
|---|---|
| `CLAUDE.md` | Commands, module architecture, request flow, persistence gotchas. It says outbound calls use `RestTemplate`, but the code uses `RestClient`, so trust the code. |
| `doc/REVIEW.md` | Per-file rationale for every file added or deleted in the port. §3 lists the non-obvious fixes and §5 is the reviewer checklist. **Mandatory.** |
| `doc/breaking.md` | The contract decisions (error shape, `Accept` → 406, schema validation) and the evidence for each. |
| `doc/MIGRATION.md` | Endpoint → controller → service table and how auth and audit are wired (no Spring Security). |
| `doc/COURT_SCHEDULER_GUIDE.md` | Overall architecture. |
| `build.gradle`, `settings.gradle`, `gradle.properties`, `listingcourtscheduler-api/build.gradle`, `listingcourtscheduler-integration-test/build.gradle` | The Gradle build to copy. |
| `listingcourtscheduler-api/src/main/resources/application*.y*ml` | Config layout and env var conventions. |
| `listingcourtscheduler-api/src/main/java/uk/gov/moj/cpp/courtscheduler/{config,envelope,validation,controllers}/` | The web plumbing that replaces the framework. Port these classes, don't reinvent them. |
| `listingcourtscheduler-integration-test/docker/docker-compose.integration.yml` | The IT stack. |

**What courtscheduler does not cover.** Listing has to design these from scratch:
- Domain JMS. Courtscheduler has no inbound or outbound domain messaging. Its only Artemis use is
  `cp-audit-filter-springboot`, and its integration profile excludes `JmsAutoConfiguration`.
- Event sourcing and aggregates. Courtscheduler was already a CRUD-on-viewstore service.
- The outbox and inbound dedup.
- XHIBIT WebDAV.
- Calls to the progression, defence, hearing and other services. Courtscheduler only calls
  referencedata and usersgroups.

## 3. Goal (one sentence)

A **drop-in replacement**: every upstream caller, every downstream consumer, and the existing
integration-test suite must be unable to tell that the implementation changed.

## 4. Non-negotiable invariants

1. **Command contract preserved.** All 30 HTTP command media types in
   `listing-command/listing-command-api/src/raml/listing-command-api.raml` keep the same URL path,
   HTTP method, `Content-Type` media type, request JSON schema, response status (e.g. `202 Accepted`)
   and error behaviour. Context root stays **`/listing-service/command/api/rest/listing`**.
2. **Query contract preserved.** All 18 query names / 17 paths in
   `listing-query/listing-query-api/src/raml/listing-query-api.raml` keep the same path, query params,
   `Accept` media type, response JSON shape (validated against the 8 response schemas), `text/csv`
   for the CSV report, and status codes. Context root stays **`/listing-service/query/api/rest/listing`**.
3. **Inbound public events preserved.** Consume the same 26 `public.*` events from Artemis topic
   **`public.event`**: progression ×16, hearing ×9 and `public.events.hearing.*` ×2 (see
   `listing-event/listing-event-processor/src/yaml/subscriptions-descriptor.yaml`). Parse the
   framework JSON envelope (`_metadata`: `id`, `name`, `createdAt`, `context.user`, `causation`,
   `correlation`, `stream` …).
4. **Outbound public events preserved.** Publish the same 34 events to topic `public.event`:
   24 `public.listing.*` and 10 `public.events.listing.*` (see `public-publications-descriptor.yaml`).
   Keep identical names, payloads (validated against the existing public schemas in
   `listing-event/listing-event-processor/src/yaml/json/schema/public*.json`), envelope metadata
   format and the JMS message properties that downstream framework consumers rely on (e.g. `CPPNAME`).
   Keep the same triggering conditions and the same ordering per hearing.
5. **Access control preserved.** The 27 command rules and 17 query rules (group-membership based,
   `listing-command-api.drl`, `listing-query-api.drl`) give the same allow/deny for the same
   `CJSCPPUID` user. Existing bypasses (hearing-slots, CSV report) stay as they are unless §14 says
   otherwise.
6. **Outbound integrations preserved.** Calls to referencedata, progression, defence, usersgroups,
   hearing, listingcourtscheduler, system-id-mapper, systemdocgenerator and XHIBIT WebDAV keep the
   same paths, media types and headers.
7. **Error and negotiation behaviour preserved.** Courtscheduler established these as the WildFly
   wire contract (`doc/breaking.md`), so verify each one against a listing IT before relying on it:
   - every 4xx/5xx body is `{"error":"<msg>"}`, with multiple messages joined by `"; "`;
   - an `Accept` that matches nothing → **406**, not 500;
   - a request body that fails its schema → 400.
8. **Integration tests.** `listing-integration-test` scenarios are kept and must pass against the
   new app. §9 and §14 D1 set out which harness changes are allowed.

## 5. Target internal architecture (mirror courtscheduler)

- **None of the following survive:** aggregates, private events, the event store, the event
  listener/processor split, the command-handler queue (`listing.handler.command`), the
  `listing.event` topic, `Sender`/`Requester`, the stream buffer, catch-up/replay, `processed_event`.
- **Request flow (as in courtscheduler):** `CorsConfig` → `RequestSchemaValidationFilter`
  (vendor `+json` bodies only) → `cp-auth-rules-filter` (`Ordered.HIGHEST_PRECEDENCE + 30`) →
  `cp-audit-filter-springboot` → `ActionMetricsInterceptor` → controller (`*Api` implementing the
  generated `*OpenApi` interface) → service (`@Transactional`) → Spring Data repositories →
  [outbox write]. On the way out, `EnvelopeResponseBodyAdvice` adds the `_metadata` wrapper and
  `GlobalExceptionHandler` renders `{"error":…}`.
- **Inbound public event flow (new, not in courtscheduler):** `@JmsListener(public.event, durable
  subscription)` → envelope parser → dispatcher by `_metadata.name` → dedup check → the same
  service layer.
- **Aggregate business rules move into domain services** that load and change JPA state directly.
  This covers `Hearing` (~3.9k lines), `Case`, `Application`, `ListingNote`,
  `PublishCourtListRequestAggregate` and `SeedHearingAggregate`. **Every invariant or guard that an
  aggregate `apply` or command method enforces today must be ported and covered by a unit test.**
- **The "enrich then resend `-enriched`" command pattern** becomes a single in-process call.
  The service calls the enrichment services (`HearingEnrichmentOrchestrator`,
  `CourtScheduleEnrichmentService`, …) directly.
- **The private-event → public-event fan-out** in `ListingEventProcessor` (50 handlers) becomes
  explicit outbox writes at the end of the relevant service transaction.
- **The in-VM `@Handles("listing.courtlist")`-style query-view calls** used by the processor become
  direct service calls.
- **The XHIBIT court-list export saga** (`docs/XHIBIT_COURT_LISTS_MESSAGE_FLOW.md`) becomes a
  service pipeline: record request → build list → JAXB/XSD render → Sardine WebDAV PUT → record
  outcome → publish `public.listing.court-daily-list` / `court-list-published`. Use `@Async` where
  the original was asynchronous, as courtscheduler did with its `@Asynchronous` rota pipeline.

## 6. Tech stack and build (copy courtscheduler unless an ADR says otherwise)

**Build: Gradle multi-project, in place in this repo on `mbd-refactor`.**
- Root `build.gradle`:
  - plugins `org.springframework.boot` 4.1.0, `io.spring.dependency-management` 1.1.7,
    `org.openapi.generator`, `org.cyclonedx.bom`, `com.gorylenko.gradle-git-properties`,
    `com.avast.gradle.docker-compose`, `com.github.ben-manes.versions`, `pmd`;
  - Java 25 toolchain;
  - BOMs: `spring-boot-dependencies`, `testcontainers-bom`, `junit-bom` 6.x;
  - Lombok on every subproject;
  - tests pinned to `user.timezone=Europe/London`, plus `jdk.httpclient.allowRestrictedHeaders=content-length`;
  - reproducible archives;
  - PMD runs only when named explicitly.
- `allprojects { group = 'uk.gov.moj.cpp.listing'; version = System.getProperty('ARTEFACT_VERSION') ?: '0.0.999' }`.
- Repositories: Maven Central plus the HMCTS Azure Artifacts feed
  `https://pkgs.dev.azure.com/hmcts/Artifacts/_packaging/hmcts-lib/maven/v1`, with credentials from
  `AZURE_DEVOPS_ARTIFACT_USERNAME`/`_TOKEN`. The feed is needed for `cp-auth-rules-filter` and
  `cp-audit-filter-springboot`.
- `gradle.properties` pins `cpAuthFilterVersion` (courtscheduler: 1.0.9), `cpAuditFilterVersion`
  (1.0.5), `lombokVersion`, `testcontainersBomVersion`, `everitJsonSchemaVersion` (1.14.6) and
  `openApiGeneratorVersion`. Port the Sonar `coverage`/`cpd` exclusions from the root `pom.xml`
  so coverage stays comparable.
- The api module produces `bootJar` → `build/libs/cpp-context-listing-<version>.jar`, plus a `plain`
  classifier jar. It also generates `buildInfo`, git properties and a CycloneDX SBOM, and publishes
  with `maven-publish` to GitHub Packages and Azure Artifacts as `uk.gov.hmcts.cp:<repo-name>`.
  Copy the idempotent-publish guard.

**Stack:**
- **API.** OpenAPI 3 spec-first, ported 1:1 from the two RAML files. Generator config, as in
  courtscheduler:
  - `generatorName 'spring'`;
  - `interfaceOnly`, `useSpringBoot3`, `useTags`, `useJakartaEe`, `skipDefaultInterface`,
    `useResponseEntity`, `openApiNullable=false`, `dateLibrary=java8`;
  - `apiNameSuffix 'OpenApi'`;
  - output to `build/generated/openapi`, with `openApiGenerate` running before `compileJava`.
  Spec location: `listing-api/src/main/resources/openapi/listing-api.openapi.yml`. Listing has two
  context roots (command and query), so ADR 8 decides between one spec and two.
- **Request validation.** `RequestSchemaValidationFilter` + `CachedBodyHttpServletRequest`, using
  everit draft-04 (the library the framework used). It reuses the existing RAML JSON schemas
  unchanged; `processResources` copies them to `request-schemas/` on the classpath. Only vendor
  `+json` media types are schema-gated.
- **Envelope.** `EnvelopeResponseBodyAdvice` + `JsonEnvelopeWrapper` + `@SkipEnvelope` (for example
  on the `text/csv` report), toggled by `listing.envelope.enabled`.
- **JSON.** `JacksonObjectMapperConfig` + `JakartaJsonModule` + `JsonValueConverter`, so that
  `jakarta.json.JsonObject` serialises natively instead of as a bean. Runtime: `jakarta.json-api`
  plus `parsson`.
- **Web plumbing.**
  - `PrimaryHandlerMappingConfig`: makes Spring's handler mapping win over the generator's.
  - `CorsConfig`: `allowedOriginPatterns("*")`, matching the legacy default.
  - `ActionMetricsInterceptor`: the `cpp.action.total` counter and the `cpp.action.individual`
    timer, replacing the framework metrics interceptors.
- **Authorization.** `cp-auth-rules-filter` with ported DRL under
  `src/main/resources/uk.gov.moj.cpp.listing.api.accesscontrol.drl/`. There is no Spring Security.
  The `UserAndGroupProvider` global provides **both** `hasPermission(...)` and
  `isMemberOfAnyOfTheSuppliedGroups(...)`, checked in `cp-auth-rules-filter` 1.0.9, so listing's
  group-based rules port directly.
- **Audit.** `cp-audit-filter-springboot` via `AuditFilterConfig`. It needs the OpenAPI spec path
  even when disabled.
- **Persistence.** Spring Data JPA + Liquibase, **reusing the existing viewstore schema**
  (`listing-viewstore-liquibase`, 10 tables) as the system of record. New changesets are additive
  only. Liquibase is a `runtimeOnly` dependency of the api.
  - Set `spring.liquibase.change-log` explicitly; courtscheduler found a legacy
    `liquibase.properties` confuses Boot 4.
  - Use `hibernate-jpamodelgen` for the metamodel.
  - Postgres 16.
- **Messaging (new).** Spring JMS + Artemis (`spring-boot-starter-artemis`), durable topic
  subscriptions, transacted sessions. Model the broker config keys on courtscheduler's `cp.audit.*`
  block: HA hosts, port, SSL, keystore/truststore and reconnect/retry tuning.
- **Outbound HTTP.** Spring `RestClient` on the JDK `HttpClient` (`JdkClientHttpRequestFactory`),
  following `CommonPlatformQueryClient`:
  - the configured system user UUID goes in `CJSCPPUID`, replacing `Requester#requestAsAdmin`;
  - the vendor `Accept` header is set per call;
  - connect and read timeouts come from config.
  Don't use `RestTemplate`: it adds `Content-Length: 0`, which the JDK client rejects.
- **Cache.** If refdata caching is needed, copy courtscheduler's `cache` module: Redis via Lettuce,
  configured under `redis.common-cache.*` with a key prefix and TTLs, and off by default.
- **Observability.**
  - Actuator `health,info,prometheus,metrics` under `/actuator`, with liveness and readiness probes.
  - Micrometer Prometheus and `spring-boot-starter-opentelemetry`.
  - JSON logging to stdout (`context/logging-standards.md`). Courtscheduler has the
    `logstash-logback-encoder` dependency but **no logback or structured-logging config**, so this
    is a gap to close here (§15).
  - Log levels: root `${LOG_LEVEL_ROOT:WARN}` and app `${LOG_LEVEL_LISTING:INFO}`.
- **Azure.** Managed Identity only; no connection strings or keys. Courtscheduler used a
  connection-string bridge only in its integration profile, for Azurite.
- **Virtual threads** are behind `${VIRTUAL_THREADS:false}`.

**Config layout (as in courtscheduler):**
- `application.yaml` pulls in the other files through `spring.config.import`:
  `optional:configtree:/mnt/secrets/rpe/` and `optional:classpath:application-{datasource,server-management,authz,audit}.yml`
  (plus `-messaging`, `-xhibit` as needed).
- `application-integration.yaml` is the IT profile.
- Every setting is `${ENV_VAR:default}`. The env var prefix is `LISTING_*`, in place of
  courtscheduler's `CSCHED_*`; confirm this in §15.
- Datasource:
  - Hikari `auto-commit: true`. **Keep it.** Courtscheduler lost connections that sat idle in
    transaction when it was false.
  - `open-in-view: false`, `ddl-auto: none`, `hibernate.jdbc.time_zone: UTC`, batch size 50 with
    ordered inserts and updates.
- `server.servlet.context-path`: **the command and query context roots in §4 must both be served.**
  This needs a decision (ADR 8): a shared `/listing-service` context path with full controller
  paths, or two dispatcher paths.

**Packaging and runtime:**
- `Dockerfile` at the repo root. `ARG BASE_IMAGE` defaults to `eclipse-temurin:25-jre`; CI uses
  `hmcts/apm-services:25-jre`.
- The container runs as a non-root `app` user, with `docker/startup.sh` as the entrypoint. The
  script picks the non-`plain` jar and runs `exec java $JAVA_OPTS -jar …`.
- A separate IT Dockerfile goes under the api module's `docker/`.

## 7. Design decisions to record as ADRs

1. Removal of event sourcing. The viewstore becomes the system of record. Record how state that
   today lives **only** in the event stream or aggregate (not in viewstore columns or the
   `hearing.payload` jsonb) is migrated.
2. Reliable publishing: **transactional outbox** (decided). The outbox table is written in the
   service transaction and a relay publishes to Artemis `public.event`, at-least-once and ordered
   per hearing.
3. Inbound idempotency and ordering without `processed_event` or the stream buffer. Use a dedup
   table keyed on `_metadata.id`, with concurrency control by `@Version` optimistic locking or
   `SELECT … FOR UPDATE` per hearing.
4. Command execution is **synchronous inside the HTTP request transaction**, still returning `202`
   (decided). Document the latency and timeout impact for heavy commands such as court-list publish.
5. Mapping the Drools group rules onto `cp-auth-rules-filter`, including how `Action.name` is
   resolved (§11.3).
6. Cut-over: **big-bang** (decided). Stop the JEE app, run the backfill migration, start the Spring
   Boot app. No parallel run and no dual-write. Rollback is restore the DB snapshot and redeploy
   the WAR.
7. Places where this port follows courtscheduler instead of `service-hmcts-crime-springboot-template`:
   build layout, Dockerfile/`startup.sh`, config fragments and logging.
8. Serving two context roots (command and query) from one Boot app: one OpenAPI spec or two, and the
   context-path strategy.

## 8. Module plan (proposed; naming follows courtscheduler)

Package root: `uk.gov.moj.cpp.listing`. Generated code goes in `uk.gov.moj.cpp.listing.openapi.{api,model}`.

The dependency flow mirrors courtscheduler: `domain` ← `viewstore-persistence`; `common` → domain,
cache, persistence; `api` → all.

| Module | Contents | Courtscheduler equivalent |
|---|---|---|
| `listing-domain` | POJOs + Jackson, generated from the existing schemas. Exposes `testFixtures` (`FileUtil#payloadToObject`). | `listingcourtscheduler-domain` |
| `listing-viewstore:listing-viewstore-persistence` | Entities and Spring Data repositories | same |
| `listing-viewstore:listing-viewstore-liquibase` | Existing changelogs plus additive changesets (outbox, dedup, backfill columns) | same |
| `listing-cache` (if needed) | Redis/Lettuce refdata cache | `listingcourtscheduler-cache` |
| `listing-common` | Domain services, REST clients, refdata | `listingcourtscheduler-common` |
| `listing-messaging` | Envelope model, `@JmsListener` + dispatcher, publisher, outbox relay, dedup | *(none, new)* |
| `listing-xhibit` | JAXB from the existing XSDs, Sardine WebDAV | *(none, new)* |
| `listing-api` | Boot app (`uk.gov.moj.cpp.listing.Application`), `*Api` controllers, config, envelope, validation, DRL | `listingcourtscheduler-api` |
| `listing-integration-test` | Kept ITs; Gradle docker-compose runner | `listingcourtscheduler-integration-test` |

Delete after parity (courtscheduler's `doc/REVIEW.md` §2 is the model for documenting each deletion):
- `listing-command`, `listing-event`, `listing-event-sources`, `listing-healthchecks`;
- `listing-service` (the WAR) and `listing-selective-catchup`;
- the framework descriptors (`beans.xml`, `persistence.xml`, `META-INF/*`, `apache-deltaspike.properties`)
  and the Maven poms;
- `listing-integration-test-persistence` and `listing-performance-test`, which are **dropped**
  (decided).

## 9. Integration tests

- Keep every `*IT` scenario, its inputs and its public-contract assertions.
- **Only the harness changes** (decided). Scenarios, inputs and public-contract assertions are
  untouched. Permitted changes:
  - Replace the 32 `createPrivateConsumer("listing.events.*")` waits with an equivalent observable
    wait that proves the same state change: the matching outbound public event, or polling the
    query API or viewstore. List each replacement in a mapping table in the PR.
  - `DatabaseCleaner` / `AbstractIT.setUp()` truncate the new tables (viewstore, outbox, dedup)
    instead of the event-store and stream tables. Replace `ListingEventStoreUtility` (an `event_log`
    count) with an equivalent viewstore assertion.
  - Base URI and ports, WireMock moved to its own container, and a docker-compose stack replacing
    WildFly + `cpp-developers-docker`.
  - Any other IT edit needs explicit approval.
- **Harness layout (copy courtscheduler's `listingcourtscheduler-integration-test/build.gradle`):**
  - The module is tests only (`jar { enabled = false }`).
  - Plugin `com.avast.gradle.docker-compose`. The `test` task `dependsOn composeUp` and is
    `finalizedBy composeDown`, and `composeBuild` depends on `:listing-api:bootJar`.
  - `upAdditionalArgs ['--wait','--wait-timeout','180']`, with a compose v2 project name.
  - System properties `app.baseUrl` and `wiremock.baseUrl`.
- **The compose file** is `listing-integration-test/docker/docker-compose.integration.yml`, with
  these services:
  - `db`: `postgres:16-alpine` with a healthcheck;
  - `wiremock`: `wiremock/wiremock:3.x`, mounting `src/test/resources/wiremock/{mappings,__files}`;
  - `artemis`: **new, not in courtscheduler**;
  - `app`: `SPRING_PROFILES_ACTIVE=integration`, healthcheck on `/actuator/health`.
  Authz and HTTP audit are **on** in ITs to prove the filter wiring; the audit JMS publish is off.
  The usersgroups permissions stub is served from WireMock.
- **Unlike courtscheduler,** the integration profile must **not** exclude `JmsAutoConfiguration`,
  because listing's ITs depend on JMS.
- `./gradlew clean build` runs everything, including the IT stack, as in courtscheduler.
  `runIntegrationTests.sh` is rewritten as a thin wrapper around
  `./gradlew :listing-integration-test:test` (keeping its midnight-UTC guard), so the `CLAUDE.md`
  rule (`./runIntegrationTests.sh` green locally) still holds. Never skip, `@Disabled` or weaken an
  assertion to go green.
- Add unit tests for every ported aggregate rule, plus a contract test per command, query and
  public event that validates payloads against the existing JSON schemas.
- Repository tests are `@DataJpaTest` against Testcontainers Postgres (courtscheduler's
  `AbstractRepositoryTest`).
- New-style `*IntegrationTest` classes (`AbstractIntegrationTest`) may be added for the envelope,
  error shape, authz, schema-validation and smoke checks. Courtscheduler's
  `EnvelopeAndErrorShapeIntegrationTest`, `AuthorizationIntegrationTest`,
  `RequestSchemaValidationEndToEndIT` and `SmokeIntegrationTest` are the models.

## 10. Delivery approach

Incremental, vertical slices, each green on the IT suite before the next:
1. Skeleton app, copied from courtscheduler's api module: health, logging, config fragments,
   OpenAPI from RAML, schema filter, authz, audit, envelope, error handler, metrics.
2. Persistence (existing Liquibase, entities, repositories) and the query endpoints. These are
   read-only and the easiest parity win, so run the query ITs.
3. Messaging infrastructure: envelope, inbound listener and dispatcher, publisher and outbox, dedup.
4. Command slices by feature:
   - listing notes;
   - list court hearing / unscheduled;
   - update / allocate;
   - offences / defendants / cases;
   - delete / next-hearing;
   - judiciary / sequence / vacate;
   - court-list publish & XHIBIT;
   - migrate / duplicate / HMI.
5. Inbound public-event handlers by source (progression, hearing).
6. Cut-over artefacts: Dockerfile + `startup.sh`, Helm, pipeline, data-migration script, runbook,
   deploy notes. Also write `docs/REVIEW.md` and `docs/breaking.md` in courtscheduler's format.

## 11. Pitfalls already hit in courtscheduler (avoid them up front)

1. **Transactions.** DeltaSpike repositories were implicitly transactional; Spring Data custom
   `…Impl` fragments are not.
   - Put class-level `@Transactional(readOnly = true)` on any custom `Impl`.
   - Put method-level `@Transactional` on every mutating method, including helpers that are called
     outside the proxy.
   - Making every read transactional read-write took courtscheduler's IT run from 6 to 22 minutes
     (REVIEW §3.1).
2. **Rollback-only contamination.** An upsert done as "try insert, catch the constraint violation,
   then update" inside the caller's transaction marks it rollback-only at flush time, which gives
   `UnexpectedRollbackException` → 500.
   - Use `@Transactional(REQUIRES_NEW)` for the insert or a real `INSERT … ON CONFLICT`
     (REVIEW §3.12).
   - This matters directly for the outbox and the dedup table.
3. **The DRL `Action.name` must match the vendor token.** The filter lowercases the name and
   resolves it in this order:
   1. the `CPP-ACTION` header;
   2. the vendor token from `Content-Type`;
   3. the vendor token from `Accept`;
   4. the `"<METHOD> <PATH>"` fallback, used for plain `application/json` endpoints.

   With `deny-when-no-rules: true`, a rule that doesn't match gives **403**. Cross-walk every
   media type to a rule (REVIEW §3.3).
4. **Permission and group casing.** Match what production usersgroups returns. Don't normalise it,
   and keep the WireMock stubs, including the dynamic `/__admin/mappings` payloads, consistent
   (REVIEW §3.2).
5. **The error key is `error`, not `errorMessage`.** The framework's internal `errorMessage` key is
   not what goes on the wire (breaking.md).
6. **No `Accept` match → 406.** Add a `HttpMediaTypeNotAcceptableException` handler, not the
   catch-all 500.
7. **Repository style.** Keep the DeltaSpike-style declarative form: method names plus `@Query` on a
   `JpaRepository` interface, with `default` aliases (`findBy`, `merge`, `remove`). Don't use
   hand-written `EntityManager` boilerplate (REVIEW §3.7).
8. **Circular dependencies.** Courtscheduler needed `spring.main.allow-circular-references: true` for
   a legacy CDI cycle. Prefer breaking the cycle. If you can't, document it.
9. **`META-INF/beans.xml` duplicates.** Set `processResources { duplicatesStrategy = EXCLUDE }` while
   legacy modules are still present. Add no new `META-INF` descriptors and no Justice Services
   dependencies.
10. **Timezone.** Pin `Europe/London` for tests. Code that mixes `systemDefault()` with hard-coded
    London passes locally and fails on UTC CI runners.

## 12. Definition of done

- All ITs green; all unit tests green; the review checklist passes, plus courtscheduler's
  `doc/REVIEW.md` §5 checklist adapted for listing.
- Contract diff: the OpenAPI generated from RAML ≡ the served spec; declared and actual status codes
  match the RAML for every endpoint; every outbound public event is schema-valid.
- No framework artefacts on the runtime classpath: `uk.gov.justice.services.*`, `javax.*`,
  DeltaSpike or WildFly descriptors.
- ADRs + implementation-plan artifact in `docs/pipeline/`. `docs/REVIEW.md` and `docs/breaking.md`
  written.

## 13. Facts gathered (from scan, for reference)

| Item | Count / location |
|---|---|
| HTTP command media types | 30 (`ListingCommandApi`, 26 `@Handles`) |
| Internal handler commands | 74 across 10 handler classes |
| Aggregates | 6 (`listing-domain-aggregate`) |
| Private events | ~129 (`listing.events.*`), 79 listener handlers, ~85 processor handlers |
| Queries | 18 names / 17 paths, 8 response schemas, 3 hand-written JAX-RS adapters |
| Viewstore | 10 tables, 31 Liquibase changesets, 13 entities |
| Public events in / out | 26 / 34 |
| ITs | 51 `*IT` classes, 54 public events produced, 23 public listing events consumed |
| IT ↔ framework couplings | 32 private `listing.event` consumers. ITs truncate `event_log`, `event_stream`, `publish_queue`, `pre_publish_queue`, `published_event`, `stream_status`, `processed_event` and `stream_buffer`. `ListingEventStoreUtility` counts `event_log`. WireMock is served from WildFly on :8080. |
| Current build | Maven, Java 17, parent `uk.gov.moj.cpp.common:service-parent-pom:17.104.4`, `azure-pipelines.yaml` |
| Reference app | Spring Boot 4.1.0, Java 25, Gradle, `cp-auth-rules-filter` 1.0.9, `cp-audit-filter-springboot` 1.0.5, everit 1.14.6, Postgres 16, WireMock 3.9.1, JUnit 6, Testcontainers |
| Reference app messaging | **none.** Courtscheduler has no domain JMS, so the messaging patterns have to be designed fresh |
| Reference app CI | GitHub Actions (`.github/workflows/ci-build-publish.yml` etc.) publishes the jar, and the ADO `cp-gh-artifact-to-acr` pipeline builds the image. Listing currently uses ADO `azure-pipelines.yaml` |

## 14. Decisions taken

| # | Date | Topic | Decision |
|---|---|---|---|
| D1 | 2026-10-16 | IT coupling to private events / event store | Change the harness only; keep scenarios and public-contract assertions (§9) |
| D2 | 2026-10-16 | Existing data | Reuse the existing viewstore DB/schema as the system of record, plus a one-off backfill of aggregate-only state |
| D3 | 2026-10-16 | Outbound publishing | Transactional outbox → Artemis `public.event` |
| D4 | 2026-10-16 | Build / repo | Gradle, in place in this repo on branch `mbd-refactor`; JEE modules deleted at parity |
| D5 | 2026-10-16 | Command execution | Synchronous in the request transaction, still responding `202 Accepted` |
| D6 | 2026-10-16 | Cut-over | Big-bang; no parallel run |
| D7 | 2026-10-16 | Persistence ITs & JMeter perf tests | Dropped |
| D8 | 2026-11-17 | Reference implementation | `cpp-context-listing-courtscheduler` is the blueprint for build, config, web plumbing, persistence and IT harness (§2, §6) |

## 15. Still open (resolve during Stage 1)

1. **Jira ticket/epic** for this refactor. It is required before Stage 4 (Test Specs).
2. **Inventory of aggregate-only state.** List exactly which `Hearing`, `Case`, `Application`,
   `PublishCourtListRequestAggregate` and `SeedHearingAggregate` fields are not in the viewstore and
   need new columns plus a backfill. Producing the inventory is a Stage 1 task.
3. **Inbound idempotency.** Confirm that dedup on `_metadata.id` with a retention window is
   acceptable, and how poison messages are handled (DLQ name, retry count).
4. ~~**Access-control filter**: does `cp-auth-rules-filter` support group-membership checks?~~
   **Resolved:** `UserAndGroupProvider` in 1.0.9 exposes `isMemberOfAnyOfTheSuppliedGroups(Action, String...)`.
   What's left is to confirm that the usersgroups permissions endpoint the filter calls returns the
   group data the listing rules need.
5. **Known bypasses.** Keep the hearing-slots and CSV-report endpoints bypassing access control
   (exact parity), or fix them? Fixing them is a behaviour change and needs an ADR. In the new
   filter, a bypass means `exclude-path-prefixes`.
6. **JMS message properties / envelope.** Confirm that downstream consumers only need the framework
   envelope and the `CPPNAME` property, or list any other headers.
7. **Deployment.**
   - The Helm chart and pipeline repo need updating for a jar instead of the WAR.
   - Confirm the env var prefix (`LISTING_*`).
   - Stay on ADO `azure-pipelines.yaml`, or move to courtscheduler's GitHub Actions +
     `cp-gh-artifact-to-acr` flow?
8. **System commands.** `runSystemCommand.sh` (JMX CATCHUP etc.) has no equivalent. Confirm it can
   be retired.
9. **JSON logging config.** Courtscheduler has no logback or structured-logging config. Decide
   between Boot's built-in `logging.structured.format.console` and a `logback-spring.xml` with
   `LogstashEncoder`, per `context/logging-standards.md` and the HMCTS template.
10. **Context roots.** One Boot app serving both `/listing-service/command/api/rest/listing` and
    `/listing-service/query/api/rest/listing` (ADR 8).
