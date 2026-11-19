# ADR 011 — Phase 1: aggregates as snapshotted domain objects; listener run in-VM

- **Status:** Accepted (2026-11-19)
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §5, §10.1, §15.2)

## Context

The §15.2 inventory (2026-11-19) found that the aggregates hold state the viewstore does not:
about 60 guards read `duplicate`/`deleted` (the viewstore deletes the row), plus offence/defendant
membership, seeded-hearing state, all Case/Application/SeedHearing state, and
`PublishCourtListRequestAggregate.weekCommencing`. Several values are set **outside** `apply()`
(`Hearing.sequenceHearingDays`, `Case.updateDefendant`, `PublishCourtListRequestAggregate`), so
even a replay of the event stream does not reproduce the live state. Several listeners
deliberately diverge from aggregate state (unallocation, cancelled hearing days, split hearings).
Re-implementing the ~4.2k-line `Hearing` rules against viewstore tables would put parity at risk
everywhere.

The aggregate classes depend only on `framework-api-domain` (`Aggregate`, which is
`Serializable`, and the `EventSwitcher` DSL): no CDI, JMS or event store.

## Decision

1. **Aggregates stay, as domain objects.** Their classes and rules are unchanged. Their state is
   stored Java-serialised in `aggregate_snapshot` (viewstore, changeset `030`) and loaded with
   `SELECT … FOR UPDATE`, which serialises commands on the same aggregate.
   `AggregateStore.update(command, id, type, aggregate -> aggregate.method(...))` replaces
   `aggregateService.get(eventSource.getStreamById(id), type)` + `eventStream.append(...)`.
2. **Events are recorded in-process** by `EventRecorder`, in the command's transaction:
   - the event listener's existing handlers run in-VM through the framework's `EVENT_LISTENER`
     dispatcher (`DispatcherCache.dispatcherFor("EVENT_LISTENER", LOCAL)`). Not through the
     `EVENT_LISTENER` interceptor chain: it contains `SubscriptionEventInterceptor`, which tracks
     event-store event numbers in `processed_event` and rejects events without them (IT run
     2026-11-19 08:12: 287 failures, "Missing previous event number"). The listener code is
     unchanged; the recorder applies the subscription filter itself;
   - events the processor subscribes to go to `event_outbox` (ADR 002).
3. **Handlers stay the entry points** in Phase 1 (API → `listing.handler.command` → handler, and
   processor `Sender` → handler). Only their persistence calls change. Removing those JMS hops is
   P1g.
4. **Slices are cut per aggregate**, not per feature: once any command of an aggregate runs on the
   snapshot, every command of that aggregate must, or the snapshot and the event stream diverge.
5. **Backfill** (P1h): for each stream in `event_log`, replay its events through the aggregate's
   `apply()` and save the snapshot. Values set outside `apply()` cannot be recovered this way; the
   affected fields are listed in `inv-aggregate-state.md` §2 and are accepted as a known gap
   (production is out of scope for Phase 1).

## Deviation from §5

§5 asks for framework-free domain services. In Phase 1 the handler orchestration code (command
parsing, enrichment, reference data) stays in the handler classes, and the listener code stays
framework-bound. Both become Phase 2 work. The aggregates themselves are already framework-free
apart from `framework-api-domain`.

## Consequences

- Guard and listener behaviour is preserved byte for byte; risk is concentrated in the plumbing
  (`AggregateStore`, `EventRecorder`), which is shared and unit-tested.
- Snapshot compatibility: changing an aggregate's fields needs a `serialVersionUID`-compatible
  change or a re-backfill.
- The listener's handlers now run in the command's transaction, so a listener failure rolls back
  the command (before, the event was stored and the listener retried later).
