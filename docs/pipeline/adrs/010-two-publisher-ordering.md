# ADR 010 — Event ordering while two paths feed the event processor (Phase 1)

- **Status:** Proposed (2026-11-19)
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §7.10, §10.1)

## Context

During Phase 1, migrated slices deliver their private events to the event processor through the
outbox relay, in-VM (ADR 002), and unmigrated slices still publish through the event store's
publisher to the `listing.event` topic. Each path keeps its own order, but nothing orders events
across the two. If one flow emits events from both a migrated and an unmigrated slice, the processor
could see them out of order.

## Decision

1. **Slice by complete flows.** A command, the events it raises, and any follow-up commands the
   processor sends for them (including the "enrich then resend `-enriched`" chains) move to the
   outbox together, in the same slice.
2. **Migrate coupled slices together** when a flow cannot be split cleanly. Record each such case
   below.
3. **Run the full IT suite after every slice,** not only the slice's own ITs, to catch ordering
   faults in flows that cross slices.
4. When the processor's `Sender` calls are switched to direct domain-service calls in a slice,
   check whether the called command belongs to an unmigrated slice; if so, either migrate it in the
   same slice or keep the `Sender` call until it is migrated.

## Slices and cross-slice flows

| Slice | Cross-slice flows | Decision |
|---|---|---|
| P1a listing notes | None. No other handler, processor or aggregate consumes the note events; the query side only reads `listing_notes`. | Migrated alone |

## Consequences

- Slice boundaries are set by event flows, not by feature names; the P1a–P1h order may change.
- The risk ends at P1.z, when the event-store publisher stops.
