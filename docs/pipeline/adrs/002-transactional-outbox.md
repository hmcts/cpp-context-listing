# ADR 002 — Reliable event publishing with a transactional outbox

- **Status:** Accepted (D3, 2026-10-16); Phase 1 design recorded 2026-11-19
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §7.2, §10.1 P1.0)

## Context

Today the command handler appends events to the event store, and the framework's
`EventPublishingTimerBean` publishes them from `publish_queue` to the `listing.event` topic. That
is already an outbox. Removing the event store (Phase 1) removes it, so domain services need
another way to make "viewstore change" and "event published" atomic. Sending to JMS directly
after the database write is two writes that can fail independently.

## Decision

1. **Table.** `event_outbox` in the viewstore database (changeset `028-create-event-outbox.xml`):
   `id BIGSERIAL`, `event_id UUID UNIQUE`, `event_name`, `stream_id`, `envelope TEXT`,
   `created_at`. Rows are deleted once published.
2. **Write.** Domain services return `DomainEvent(name, streamId, payload)`. The JEE adapter
   (`EventOutboxWriter`) builds the framework envelope and appends it in the same JTA transaction
   as the viewstore changes. Metadata follows the framework convention for events appended from a
   command: the command's metadata with a new id, the event name, the stream id, a fresh
   `createdAt`, and the command id added to the causation chain.
3. **Relay.** `EventOutboxRelay` (startup singleton) polls with a fixed delay (default 25 ms,
   `listing.outbox.relay.delayMillis`), so runs never overlap. Each event is handled by
   `EventOutboxPublisher.deliverNext()` in its own transaction: take a transaction-scoped Postgres
   advisory lock (one deliverer across all nodes), read the oldest row, deliver it, delete it.
4. **Delivery (Phase 1): in-VM, not JMS.** `EventProcessorInvoker` passes the event to the
   processor's existing `SubscriptionManager` (`@SubscriptionName("event processor subscription")`,
   the bean its `listing.event` MDB calls) in a `REQUIRES_NEW` transaction, as the MDB's
   container-managed delivery did. Public events are still published by the processor.
   - *Why not publish to `listing.event`:* tried first (IT run 2026-11-19 01:26). The processor
     handled the events, but the event listener's MDB has no message selector, so it received
     them too and rejected each one ("No position found in event") into the DLQ after 10
     redeliveries. A new topic would avoid that but needs platform config in every environment,
     which is not worth it for an intermediate step (production is out of scope for Phase 1).
   - *Retries:* a failure rolls back the processor's transaction and increments `attempts`
     (changeset `029`); the entry is retried on the next poll. After 10 attempts
     (`listing.outbox.relay.maxAttempts`) it is logged at ERROR and dropped, where JMS would have
     used the DLQ.
5. **Phase 2.** The table stays; the relay is re-implemented in Spring and publishes public events
   to `public.event` directly.

## Consequences

- At-least-once delivery. If the relay's transaction fails after the processor's transaction
  committed, the entry is delivered again, so the processor can see a duplicate. Each slice checks that its processor handlers tolerate a
  redelivered event (for P1a they only republish a public event, so a duplicate means a duplicate
  public event). Whether the event-store publisher gives the same guarantee today is not yet
  verified.
- Ordering is `id` order with a single deliverer. Two transactions can take ids and commit in the
  opposite order; those are concurrent, unrelated commands, so no causal order is broken. Deletes
  are by exact id so a late-committing lower id is never lost.
- Latency: up to one relay delay per hop (25 ms by default), comparable to the event-store
  publisher (10 ms timers on the IT stack).
- The processor handles migrated events one at a time (the MDB allowed up to 15 concurrent
  sessions), and a failing event holds back later ones until it succeeds or is dropped.
  Acceptable for Phase 1, which is not released to production.
- Migrated private events no longer appear on `listing.event`, so IT waits on them
  (`createPrivateConsumer`) must switch to an observable effect in the same slice (§9). P1a has
  none.
- The IT harness truncates `event_outbox` and includes it in the relay-drain wait (§9).
