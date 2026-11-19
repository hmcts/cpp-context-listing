# ADR 014 — P2.3.0: command-side infrastructure on Spring, legacy IT harness, listing notes

> **Superseded by ADR 015** (2026-11-19). The Spring app keeps no aggregates and no private
> events: `AggregateStore`, `EventRecorder`/`EventProjection(s)`, `ProcessorSender`, the
> `PrivateEventHandler` outbox route and `listing-domain-aggregate-spring` were removed in P2.3.1.
> The legacy IT harness (`listing-integration-test-legacy`) and `CommandRequests` described here
> are kept.

- **Status:** Superseded by ADR 015 (was: Proposed 2026-11-19, for review with P2.3.0)
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §4, §5, §9, §10.1 P2.3, D1, D5, D10, D13).
  Carries ADR 011 (aggregates as snapshots, listener in-process) into Phase 2; builds on ADR 002
  (outbox) and ADR 013 (relay, `PrivateEventHandler`).

## Context

Phase 1 kept the six aggregates as domain objects, Java-serialised in `aggregate_snapshot`, and
replaced the event store with `AggregateStore` + `EventRecorder` (listener in-VM, processor fed by
the outbox). P2.3 ports the 30 HTTP commands, the 74 handler commands, the listener projections and
the processor handlers to Spring, slice by slice. P2.3.0 sets up what every slice needs and ports
the first slice (listing notes). It also makes the 60 legacy `*IT` classes runnable against the
Spring app, so each later slice is measured by the legacy suite (§9, D1).

## Decision

### 1. Modules

| Module | Content | Sources |
|---|---|---|
| `listing-domain-event-spring` | the generated event / core-domain POJOs (`uk.gov.justice.listing.{events,event,commands,courts}`, `uk.gov.justice.core.courts`) | **vendored** under `src/generated/java` (298 classes) + the Maven module's 5 hand-written classes compiled from its directory |
| `listing-domain-aggregate-spring` | the six aggregates | **compiled from the Maven module's `src/main/java`** (`sourceSets`), unchanged |
| `listing-command-spring` | `AggregateStore`, `EventRecorder`, `EventProjection(s)`, command services (`command.<slice>`), ported processor handlers + `ProcessorSender` (`event.processor`) | new |
| `listing-integration-test-legacy` | the legacy `*IT` suite on the compose stack | Maven `src/test/{java,resources}` synced into `build/legacy-it/` minus the overridden harness classes; generated IT POJOs vendored |

`DomainEvent` moved from `listing-messaging` to `listing-domain-spring` (same package) so domain
services in `listing-common-spring` can return it. `ListingNoteService` is in
`listing-common-spring` (§8: domain services in `listing-common`); command orchestration that needs
the outbox lives in `listing-command-spring`, so `listing-common-spring` does not depend on
messaging.

**Why compile from the Maven directories instead of copying:** the JEE app is the parity baseline
until P2.5 (D13); one copy of the aggregate rules and the IT scenarios means a fix in one is a fix in
both, and the Gradle build writes nothing into the Maven modules. At P2.6 the sources move.

**Why vendor the generated POJOs:** they come from the framework's `pojo-generation-plugin` (a
Maven plugin, no Gradle port); jsonschema2pojo generates different classes. Compiling Maven's
`target/generated-sources` is not CI-safe. The aggregates and the snapshots depend on these exact
classes. `refresh-generated-sources.sh` / `refresh-legacy-inputs.sh` regenerate them offline through
Maven; review the diff (serialVersionUIDs).

`framework-api-domain` (7 classes: `Aggregate`, `EventSwitcher`, `@Event`, …; pure Java) stays a
dependency (resolved from the local Maven repository / HMCTS feed). It is the only framework jar on
the runtime classpath; vendor or replace it at P2.6 (§12).

### 2. AggregateStore — same table, same format as Phase 1

`SELECT … FOR UPDATE` on `aggregate_snapshot` (P2.1 `AggregateSnapshotJdbcRepository`), plain Java
serialisation, key (`aggregate_id`, aggregate class name), `@Transactional(MANDATORY)`;
`update(cause, id, type, aggregate -> aggregate.command(...))` = load, apply, save, record.

**Finding (snapshot compatibility).** The JEE WAR contains four generated copies of many POJOs
(listing-domain-event, listing-event-processor, listing-command-api, listing-command-handler), each
with its own serialVersionUIDs; WildFly loads the first jar in the expanded deployment's directory
listing, i.e. **file-system order** (local container: command-api, event-processor, domain-event,
command-handler). A Phase 1 snapshot can therefore hold any copy's UID (`Hearing.ser` from the
local JEE app: `uk.gov.justice.listing.events.Hearing` with the processor copy's UID). The Spring
`ObjectInputStream` accepts a generated POJO (`uk.gov.justice.listing.*`, `uk.gov.justice.core.courts.*`)
whose UID differs from the local class **only when the serialised field layout is identical**
(names, type codes, type strings); anything else fails as plain Java serialisation would.
`AggregateSnapshotCompatibilityTest` reads three snapshots written by the Phase 1 JEE app
(`phase1-snapshots/*.ser`, IT test data) and keeps applying commands to them.

### 3. EventRecorder

For each raised event, in the command transaction (`MANDATORY`): envelope with the framework
metadata convention (`EventOutbox.envelopeFor`: command metadata, new id, name, `createdAt`,
command id appended to `causation`, `stream.id`); then (1) the ported listener projection for the
name (`EventProjection`, registry `EventProjections`, one per name, start-up failure on duplicates)
runs inline — a projection failure rolls back the command, as ADR 011; (2) if a ported processor
handler (`PrivateEventHandler`, via `InProcessEventRoute`) subscribes, the event goes to the outbox
and is handled after commit in its own transaction (ADR 013). Events with neither are dropped (as
outside both Phase 1 subscriptions).

### 4. Processor output — `ProcessorSender`

Replaces the framework `Sender`. `sendPublic(event, "public.…", payload)` keeps the private
event's metadata (user, session, client correlation, causation, `stream.id`, `createdAt`) as
`metadataFrom(event.metadata()).withName(...)` did, and appends to the outbox (the relay publishes
`public.*`, ADR 013). **Difference, invisible on the wire:** the framework kept the private event's
`_metadata.id`; the private event's outbox row still holds that id (`event_outbox.event_id` is
unique) while its handler runs, so the public id is a name-based UUID of (private id, public name).
It is stable, and `EventOutbox.appendIfAbsent` (`INSERT … ON CONFLICT (event_id) DO NOTHING`) makes
a redelivered private event idempotent. Internal `listing.command.*` sends become direct calls to
the command services.

### 5. Commands (D5)

Controller implements the generated `*OpenApi` interface → `CommandRequests` builds what the
framework's `RestEnvelopeBuilder` built: metadata (new id, action name, `CJSCPPUID`, `CPPSID`,
`CPPCLIENTCORRELATIONID`, `CPPCAUSATION`; no `createdAt`) and the payload (body + path parameters,
path wins); edit/delete on one path dispatch on `Content-Type`. Command service method
`@Transactional`, returns nothing, controller answers `202` with no body.

### 6. Legacy IT harness (§9, harness-only changes)

Run with `./runSpringIntegrationTests.sh legacy [-- -PitClasses=A,B | --tests …]` (midnight guard
kept); task `:listing-integration-test-legacy:legacyIntegrationTest`, not part of `build`. Stack =
the Spring compose file + `docker/docker-compose.legacy.yml` (a `haproxy:2.0` gateway with
cpp-developers-docker's two rewrite rules — `/listing-query-api/…` → `/listing-service/…` — on
:8192; Artemis console on :8171). Overrides (same FQCN, the Maven copies are left out):
`BaseUriProvider` and `QueueUriProvider` (framework test-utils: fixed :8080 / :61616),
`TestJdbcConnectionProvider` (viewstore only; event store/system DB → error), `DatabaseCleaner` (no
event store; framework stream tables skipped; relay = `event_outbox`), `ArtemisQueuePurger`
(Jolokia URL/credentials/broker name; only `public.event` and `DLQ`), `WireMockStubUtils` (compose
WireMock; each usersgroups groups stub mirrored to the authz filter's
`/usersgroups-query-api/.../logged-in-user/permissions` for the same user). Plus
`LegacyHarnessExtension` (auto-detected; re-points the static WireMock client after the stub
classes' `configureFor(host, 8080)` initialisers) and the `base-uri` of `endpoint.properties`
filtered at build time. The test classpath is the Maven module's resolved test classpath
(`legacy-it-dependencies.txt`, non-transitive). No scenario, input or assertion changed.

## Consequences

- Every later slice follows the same pattern (checklist in `MBD_REFACTOR_PROMPT.md` §10.3, P2.3.0).
- The test-only `P20SkeletonApi` is deleted (the P2.0 ITs now drive the real note command).
- Parity fix found by the legacy run: a trailing slash (`/hearings/?…`) is ignored, as RESTEasy
  did (`TrailingSlashConfig`).
- Snapshots: if Phase 1 is ever released (§15.11), production snapshots carry whichever generated
  copy each WildFly node loaded; the layout-tolerant reader covers identical layouts only. Safer:
  re-run the backfill from `event_log` under the Spring build at cut-over.
- Legacy baseline on Spring after P2.3.0: 335 tests, 25 passed, 307 failed (305 unported command,
  1 unported split, 1 query parity bug in `HearingSlotsIT`), 3 skipped.
- Harness detail: the suite reads `src/test/resources/...` relative to the working directory, so the
  task runs in `build/legacy-it/` where the resources are synced.
- Two legacy stacks cannot run at once (fixed container names); the legacy suite is red until the
  remaining slices are ported.
