# ADR 009 — Two-step migration: remove CQRS/ES on JEE, then move to Spring Boot

- **Status:** Accepted (D9, 2026-11-19)
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §7.9, §10)

## Context

v0.5 of the plan moved straight to Spring Boot (P0–P6): build a new platform and IT harness, then
port the domain logic onto it. Courtscheduler is a blueprint for the Spring Boot plumbing, but not
for the hardest part of this migration: aggregates → services, removing the event store, and the
messaging rework. Courtscheduler never had CQRS/ES or domain JMS.

## Decision

- **Phase 1** removes CQRS/ES while the service is still the WildFly WAR, slice by slice
  (P1.0 foundation, P1a–P1h, P1.z clean-up), checked after every slice by the existing IT suite
  (`mvn clean && ./runIntegrationTests.sh`, baseline 335 / 0 failures / 3 skipped).
- **Phase 2** moves the result to Spring Boot, copying courtscheduler.
- Domain services written in Phase 1 are framework-free (POJOs in and out, constructor
  injection, no `JsonEnvelope` / `javax.json` / DeltaSpike types in signatures), so Phase 2
  replaces adapters, not logic.
- P1.0 is a spike. If the outbox relay cannot feed the event processor without fighting the
  framework, fall back to the direct plan.

## Consequences

- A failing IT has one likely cause per phase: business logic in Phase 1, platform in Phase 2.
- Some JEE code is thrown away in Phase 2: the relay singleton/EJB, the CDI adapters
  (`ListingNoteCommands`, …) and the outbox writer's framework envelope code.
- The IT harness changes twice (Phase 1: event-store tables and private-event waits as slices
  stop producing them; Phase 2: docker-compose, Gradle, ports).
- Phase 1 slices could be released to production one by one (open item §15.11, affects D6).

## P1.0 outcome (2026-11-19)

The event processor turned out not to depend on the event store: the framework gives
`EVENT_PROCESSOR` a `BackwardsCompatibleSubscriptionManager`, which only runs the interceptor
chain. The relay therefore hands outbox events to that same subscription manager in-VM
(ADR 002), with no new topic or processor entry point. Publishing to `listing.event` was tried
and rejected because the event listener's selector-less MDB dead-letters events without an
event-store position.
