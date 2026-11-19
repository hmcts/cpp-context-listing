# ADR 013 — P2.2: framework JMS wire format, Spring outbox relay, inbound dedup and DLQ

- **Status:** Proposed (2026-11-19), for review with P2.2
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §4.3, §4.4, §5, §6, §7 ADR 2/3, §10.1 P2.2,
  §11 pitfalls 1–2, §15.3, §15.6). Builds on ADR 002 (outbox) and ADR 011.

## Context

Upstream (progression, hearing) and downstream contexts still run on the HMCTS framework, so the
Spring app must publish and consume `public.event` messages that framework code cannot tell apart
from the WildFly app's. Courtscheduler has no domain JMS to copy. The Phase 1 outbox delivered
in-VM to the event processor through an EJB timer; Phase 2 needs a Spring relay that publishes
public events itself, and an inbound listener that replaces the processor MDB, `processed_event`
and the stream buffer.

## Findings: the wire contract

Read with `javap -c -p` from the framework 17.104 jars in `listing-service/target/*.war`
(`messaging-jms`, `framework-api-messaging-jms`, `messaging-core`, `messaging-adapter-core`,
`event-publisher-jms`), from `cpp-developers-docker`, and **confirmed by capturing a real message**
from the running JEE app (2026-11-19, `public.listing.created-listing-note`, test data only;
stored as `listing-messaging/src/test/resources/legacy/`):

| Aspect | Framework behaviour | Evidence |
|---|---|---|
| Message type | `TextMessage` | `DefaultEnvelopeConverter.toMessage` → `session.createTextMessage(...)`; inbound `DefaultSubscriptionJmsProcessor` rejects anything else |
| Body | `JsonObjectEnvelopeConverter.asJsonString`: one compact JSON object, `_metadata` first, then every payload field at top level; written by Jackson with a JSR-353 module that omits `null` members | `DefaultJsonObjectEnvelopeConverter.fromEnvelope` |
| Metadata paths | `id`, `name`, `createdAt` (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` UTC), `causation[]`, `context.user`, `context.session`, `correlation.client`, `stream.id`, `stream.version`, `source`, `event.eventNumber`, `event.previousEventNumber`; `id` and non-empty `name` required | `JsonMetadata` constants, `DefaultJsonMetadata.metadataFrom` |
| Properties | **exactly one**: `CPPNAME` = `_metadata.name` (String). `JMSXDeliveryCount` is broker-set | `HeaderConstants.JMS_HEADER_CPPNAME`; captured message |
| Headers | producer defaults: `PERSISTENT`, priority 4, expiration 0; no `JMSType`, correlation id or reply-to | `JmsSender.send` (no QoS calls); captured message |
| Session | non-transacted, `AUTO_ACKNOWLEDGE`, connection per send | `JmsSender.send` → `createSession(false, 1)` |
| Destination | JNDI `public.event` (`ContextName.fromName("public.x")` + `.event`) = WildFly `external-jms-topic public.event` = **Artemis address `jms.topic.public.event`** (multicast) | `DefaultEventDestinationResolver`; `standalone.xml`; `broker.xml`; captured `JMSDestination=ActiveMQTopic[jms.topic.public.event]` |
| Processor subscription | durable, shared, no client id, name `listing.event.processor.public.event`, selector `CPPNAME in('…')` over the 28 subscribed names, `maxSession` 15 | generated `ListingEventProcessorPublicEventJmsListener` |
| Subscription queue | Artemis escapes the dots: queue `listing\.event\.processor\.public\.event` on `jms.topic.public.event` | `artemis queue stat` on the CPP broker; `_AMQ_ORIG_QUEUE` in IT (c) |
| Inbound schema check | `JsonSchemaValidationInterceptor` validates against the event schema but, for this subscription, only logs failures at DEBUG | generated MDB interceptors + `javap` |
| DLQ | broker-side: CPP brokers `dead-letter-address jms.queue.DLQ`, `redelivery-delay 0`, max-delivery-attempts Artemis default **10** | `cpp-developers-docker` `broker.xml` |

So §15.6 is answered for the producer side: downstream consumers get the envelope text and
`CPPNAME`, nothing else.

## Decision

1. **New Gradle module `listing-messaging`** (§8; no suffix, no Maven module has that name),
   framework-free (no Justice Services jar):
   - `envelope.Metadata` wraps the `_metadata` JSON object itself, so unknown fields and field
     order survive a round trip; a builder writes the framework paths. `Envelope(metadata,
     payload)`; `EnvelopeCodec` parses/serialises with the rules above. The captured legacy message
     re-serialises **byte for byte** (unit test).
   - `DomainEvent` is copied (same package as Phase 1) for P2.3.
2. **Outbox (ADR 002, Phase 2 part).** Same table (028/029), same semantics as Phase 1:
   - `EventOutbox` (writer): `write(causedBy, List<DomainEvent>)` / `append(Envelope)`,
     `@Transactional(MANDATORY)` — rows commit with the caller's changes. Metadata convention
     unchanged from Phase 1 (cause's metadata, new id, name, `createdAt`, cause id appended to
     `causation`, `stream.id`). A plain `INSERT`; nothing is caught inside the caller's
     transaction (pitfall 2).
   - `OutboxDeliverer`: one entry per `REQUIRES_NEW` transaction, `pg_try_advisory_xact_lock` with
     the Phase 1 key (single deliverer across nodes), oldest `id` first, delete by exact id,
     `attempts` + give-up after `max-attempts` (default 10, logged ERROR, dropped), at-least-once.
     The delivery exception is caught inside the deliverer's own transaction and never crosses a
     transactional proxy, so the attempt count commits (pitfall 2).
   - `OutboxRelay`: `@Scheduled(fixedDelay=25ms)`; drains until idle; **a failed entry is retried on
     the next run** (Phase 1 retried in the same loop, i.e. 10 attempts back to back).
   - Routes (`OutboxRoute`): `public.*` → `PublicEventPublisher` (JMS, wire format above, via a
     session-caching `JmsTemplate`); names with a `PrivateEventHandler` bean → `InProcessEventRoute`
     (handler in its own `REQUIRES_NEW` transaction — the P2.3 extension point for processor
     logic). No route → failed attempt. No private handler exists yet.
3. **Inbound (§7 ADR 3).** `@JmsListener` on `jms.topic.public.event`, **the same durable shared
   subscription and selector string** as the legacy MDB (so the cut-over keeps the broker queue and
   its backlog; a different filter on an existing shared subscription would be rejected or would
   recreate the queue), concurrency `1-15`, transacted session. `InboundEventProcessor`
   (`@Transactional`): parse → `INSERT INTO inbound_event_dedup … ON CONFLICT (event_id) DO NOTHING`
   (duplicate → acknowledged, no action) → `InboundEventDispatcher` by `_metadata.name`. The DB
   transaction commits before the JMS session; a failure after the DB commit causes a redelivery
   that the dedup row absorbs. A handler failure rolls back both (dedup row included) and the
   broker redelivers. Handlers for unsubscribed names, or two handlers for one name, fail start-up.
   A subscribed name with no handler is consumed with a WARN (none have handlers before P2.4; since P2.4 every name has one).
   Non-`TextMessage`s and bodies that are not envelopes throw, as in the framework (→ DLQ).
   The framework's inbound schema-validation interceptor (DEBUG-only) and MDC interceptor are not
   ported.
4. **Dedup table** `inbound_event_dedup(event_id UUID PK, event_name, received_at)` — changeset
   `031`, additive, in the shared Liquibase directory (the JEE app creates but never uses it).
   **Retention: unbounded for now** (§15.3 default); `received_at` is indexed for a later purge.
   *Update 2026-11-19 (D26):* 30 days. `InboundDedupPurge` deletes older rows on a daily cron in
   bounded batches, one node at a time under a Postgres advisory lock (settings in §15.3).
   Ordering per hearing (ADR 3's `@Version` / `FOR UPDATE`) is left to the P2.4 handlers, which own
   the rows they lock. *(Done in P2.4 with advisory locks in the shared load path; see the P2.4
   notes.)*
5. **DLQ / poison messages:** no application-level DLQ. Broker redelivery (transacted rollback) and
   the broker's dead-letter address, as before: CPP brokers `jms.queue.DLQ` after 10 attempts (not
   changed by this port). The IT broker states the same values explicitly in
   `listing-integration-test-spring/docker/artemis/broker.properties`, with the image's default DLQ
   address `DLQ` (§15.3 default).
6. **Config.** `application-messaging.yml` → `listing.messaging.*` (`MessagingProperties`), keys and
   `LISTING_ARTEMIS_*`/`LISTING_JMS_*` variables shared with `cp.audit.*` (hosts list, port, HA,
   SSL + key/trust stores, reconnect/retry tuning). The app builds its own
   `listingConnectionFactory` (same URL scheme as `cp-audit-filter-springboot`), so Boot's Artemis
   auto-configuration and `spring.artemis.*` are no longer used; `LISTING_ARTEMIS_BROKER_URL` is
   gone (compose uses `LISTING_ARTEMIS_HOST_PRIMARY`). `listing.messaging.enabled` (default true)
   switches the listener, the relay and **`management.health.jms.enabled`** together — the P2.0
   open item: health is no longer DOWN just because messaging is off.
   `listing.messaging.public-event.auto-startup` can keep the listener stopped (cut-over).
   The integration profile does not exclude JMS auto-configuration (§9).
7. **Test-only:** `P22MessagingProbeHandler` (listing-api, `listing.p22-messaging-probe.enabled`,
   integration profile only) handles `public.progression.case-linked` by writing
   `public.listing.p22-messaging-probe-echo` to the outbox, or throws when the payload has
   `p22AlwaysFail`. **Delete in P2.4** (when the real handler for that event is ported).
   *Update 2026-11-19 (P2.4): deleted, with `listing.p22-messaging-probe.enabled`; the messaging ITs
   use real handlers (see "Implementation notes (P2.4)").*

## Consequences

- Downstream framework consumers see the same messages; the ITs check this against the captured
  legacy message (structure, JSON types, `CPPNAME` as the only property, QoS headers).
- At-least-once on both sides: a relay crash after the JMS send republishes the event (as Phase 1
  and, likely, the framework's event-store publisher); inbound duplicates are absorbed by dedup.
- Byte-compatibility caveat: numbers are written as `BigInteger`/`BigDecimal` text and strings with
  Jackson's escaping, as the framework's Jackson mapper did; payloads built from POJOs go through
  the framework-equivalent `ObjectMapperProducer` (P2.1).
- The relay publishes one event per transaction (as Phase 1); throughput is bounded by the 25 ms
  poll only when the outbox is empty.
- `inbound_event_dedup` keeps 30 days of ids (D26): a redelivery older than that would be handled
  again. Broker redelivery happens within seconds, so this only matters for a manual replay.
- A message dead-lettered from the subscription has `_AMQ_ORIG_QUEUE =
  listing\.event\.processor\.public\.event`, so existing DLQ tooling keeps working.

## Implementation notes (P2.4, 2026-11-19)

**Handlers.** All 28 subscribed names have exactly one handler, in `listing-command-spring`
`command.inbound` (one class per JEE processor class, `InboundProcessor` base: name → method).
Each is the JEE `@Handles` method with its logic unchanged: the same payload transformation and the
same internal command(s), with the event's metadata under the command name
(`metadataFrom(event.metadata()).withName(…)`, so the command keeps the event's id, user, client
correlation and causation, as in JEE). The commands are direct calls of the P2.3 command services
(`ProcessorSender` → `InternalCommands`), in the listener's transaction, so the viewstore changes,
the outbox rows and the dedup row commit together. The whole handler runs as one `ProcessorQueue`
command: the decisions of every command it sends first, then their processor steps in raised order
(the Phase 1 relay order). None of the 28 JEE handlers published a public event itself.
`InboundHandlerRegistrationTest` checks: handled names = the selector list (28, no duplicates), the
dispatcher accepts them, the test list equals the `InboundEventHandler` classes on the classpath,
and every command sent has a command service.

**Ordering per hearing (ADR 3; the open point above).** Every load of rules state takes a
transaction-scoped Postgres advisory lock first (`pg_advisory_xact_lock(namespace, hash(id))`,
namespaces hearing / case / application / seeding hearing; `HearingStateStore.load`,
`RulesStateStore.load*`). Commands (HTTP) and inbound events changing the same instance therefore
serialise and each decides on what the previous one committed — what the JEE stream version check
gave. Chosen over `SELECT … FOR UPDATE` / `@Version` because the first command on an instance has no
row to lock (two hearings of one case listed at once both create `case_rules_state`: lost update),
and because locking must not create rows for rejected commands. Consequences: transactions that lock
the same instances in opposite orders (case → hearing vs hearing → case fan-outs) can deadlock;
Postgres aborts one (an inbound message is redelivered; an HTTP command fails with 500 and is not
retried — JEE's framework retried on a version conflict). Reads made before the first lock in a
transaction may be stale in the JPA persistence context: the CPS-prosecutor handler, the only
inbound handler that reads hearings before its command, clears the context after its search.

**MDC (D24).** `PublicEventListener` puts `correlationId` (`_metadata.correlation.client`, else
`_metadata.id`, else the JMS message id, else a new UUID; plain tokens only) and a new `requestId`
in the MDC for each delivery and removes them afterwards (`InboundTracing`). Outbound REST calls
made while handling propagate the correlation id (`CppRestClientConfig`).

**Probe removed.** `P22MessagingProbeHandler` and its property are gone. `MessagingIntegrationTest`
(b)–(d) now use real handlers: `public.events.hearing.marked-as-duplicate` (observed through the
courtscheduler session-release `DELETE` it makes, counted in WireMock's request journal; dedup keeps
the count at one for a redelivered id) and `public.events.hearing.hearing-resulted` without a
`hearing` (fails in its handler on every delivery → DLQ). The listing-messaging unit tests keep
their test-scoped handlers. *Not run in P2.4* (no IT runs in this phase, by instruction).

**P2.3 fix found on the way.** Marking a hearing listing never listed as duplicate (the
marked-as-duplicate event for an unknown hearing) gave rules state without a hearing id, so the
state upsert failed on the primary key; the row is now keyed by the stream id
(`HearingStateStore.save(hearingId, rules)`), as the JEE event stream was.

