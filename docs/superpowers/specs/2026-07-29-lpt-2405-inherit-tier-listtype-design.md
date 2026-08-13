# LPT-2405 — Inherit tier and list type from the seeding hearing

**Date:** 2026-07-29
**Service:** `cpp-context-listing`
**Related:** LPT-2400–2404 (hearing context, implemented), LPT-2406 (court-calendar endpoints, open)

## Problem

When listing creates a next hearing from a seeding hearing, the tier and list type
captured against the seeding hearing (LPT-2400–2404, in the hearing context) do not
travel with the new hearing. The court calendar therefore shows nothing for a trial that
inherited its listing decision from a PTPH.

## Scope

Listing fetches the seeding hearing's tier / list type / key reason from the hearing
context and stores them against the newly created hearing in the **listing view store**.

Explicitly out of scope:

- No change to `criminal-court-public-model` (coredomain). Nothing is added to
  `public.listing.hearing-confirmed`, so the hearing context is **not** told about the
  new hearing's tier. A future story can propagate it once coredomain carries the fields.
- No change to the court-calendar-facing query endpoints — that is LPT-2406, which reads
  what this story writes.

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Destination | Listing view store only | No coredomain release needed; makes LPT-2406 a straightforward read |
| Fields inherited | `tier`, `listType`, `keyReason` | All three come from one query response; `keyReason` is useful listing context |
| Finalised gate | Inherit **only** when `finalised = true` | Provisional values must not reach the calendar |
| Trial gate | Reference data `trialTypeFlag = true` | The platform's own definition, already fetched in this path, stays correct as trial types are added |
| Fetch location | Command-API enrichment service | Matches the existing enrichment services; values enter the event stream before the aggregate, so replay is deterministic |
| Field types in listing | Opaque strings, not enums | The hearing context is the single validator; constrained enums in listing would silently reject a future `TIER_8` |
| Query failure | Propagate and fail the command | See "Failure behaviour" |

## Current flow

```
progression ──listing.list-next-hearings-v2──▶ ListingCommandApi.listNextHearings
  (seedingHearing{seedingHearingId, sittingDay, jurisdictionType} + hearings[] with NEW ids)
      │  HearingEnrichmentOrchestrator: hearing days → duration → court schedule
      ▼
listing.command.list-next-hearings-enriched-v2 ──▶ SeedHearingAggregate.requestNextHearings
      ▼  listing.events.next-hearing-requested (per hearing)
NextHearingProcessor ──listing.command.list-next-hearing──▶ Hearing aggregate .list() + allocation
      ▼  listing.events.hearing-listed
HearingEventListener.hearingListed  →  hearing.properties (jsonb)
```

## Flow coverage

| Flow | Carrier for the new hearing | Work required |
|---|---|---|
| `listing.list-next-hearings-v2` | `listing/commands/hearingListingNeeds.json` — in-repo; `next-hearing-requested` and `list-next-hearing` both `$ref` it | One schema change carries the fields to the aggregate |
| `listing.list-unscheduled-next-hearings` | `core/courts/hearingUnscheduledListingNeeds.json` — coredomain, not editable | Sibling `ptphDetails[]` on the in-repo wrapper schemas |
| `listing.delete-previous-hearings-and-create-next-hearing` | Does not create the hearing in listing — publishes `public.listing.create-next-hearing-requested` → progression → re-enters listing as a normal listing request | No plumbing; inherits the other flows. Needs a test |

## Components

### `PtphDetail` value object (listing-domain-common, new)

A single type (`tier`, `listType`, `keyReason`) used throughout: as the
`PtphDetailService` query result, as the enrichment input, and as the domain/aggregate
parameter. `listing-command-api` already depends on `listing-domain-common` (for example
`ReferenceDataService` uses `domain.referencedata.OrganisationUnit`), so one type serves
every layer and there is no duplicate `PtphDetail` per module.

### `PtphDetailService` (listing-command-api, new)

Cross-context query, following the `ReferenceDataService` pattern
(`@ServiceComponent(COMMAND_API) Requester`, `requestAsAdmin`):

```java
Optional<PtphDetail> getFinalisedPtphDetail(UUID seedingHearingId, JsonEnvelope envelope)
// → hearing.get-ptph-detail, payload { hearingId: <seedingHearingId> }
// → Optional.empty() when the response has finalised != true
// → exceptions propagate (see Failure behaviour)
```

### `PtphDetailEnrichmentService` (listing-command-api, new)

The two flows carry different hearing types — `listing.commands.HearingListingNeeds`
(in-repo, so the fields sit on the hearing itself) versus the coredomain
`HearingUnscheduledListingNeeds` (not editable, so the values travel as a sibling list).
The service therefore exposes two entry points over one shared rule:

```java
// Scheduled flow — stamps the fields onto each trial hearing
List<HearingListingNeeds> enrichWithPtphDetail(List<HearingListingNeeds> hearings,
                                               SeedingHearing seedingHearing,
                                               JsonEnvelope envelope)

// Unscheduled flow — returns sibling entries keyed by hearing id, empty when nothing applies
List<HearingPtphDetail> resolvePtphDetails(List<HearingUnscheduledListingNeeds> hearings,
                                           SeedingHearing seedingHearing,
                                           JsonEnvelope envelope)
```

Both delegate to one private method holding the rule below; only the way the result is
attached differs.

1. No seeding hearing id → return unchanged / empty.
2. Resolve trial hearing type ids via `HearingTypeFactory.getTrialHearingTypeIds(envelope)`.
3. **If no hearing in the command is a trial type, return without calling the hearing
   context.** This is the core rule: the hearing context is queried only when a trial is
   being listed.
4. Otherwise fetch once per command (one seeding hearing per command) and apply
   `tier` / `listType` / `keyReason` to the trial hearings only.
5. Enrichment always overwrites, so values present in the inbound payload cannot spoof
   the hearing context.

On the unscheduled path, `UnscheduledListingCommandHandler` resolves each hearing's entry
from `ptphDetails[]` by hearing id before calling the aggregate, so the aggregate receives
the same single `PtphDetail` parameter as the scheduled path.

Called from `ListingCommandApi.listNextHearings` and
`ListingCommandApi.listUnscheduledNextHearings`, directly after
`hearingEnrichmentOrchestrator.enrich…`. Deliberately not folded into
`HearingEnrichmentOrchestrator`: that class's signature has no seeding hearing, and this
concern is seeding-specific.

### `HearingTypeFactory.getTrialHearingTypeIds` (existing class, new method)

Reads `trialTypeFlag` from the same `referencedata.query.all-hearing-types` response the
class already fetches for durations. No additional reference-data call. Parses raw JSON,
matching the existing `getHearingTypesIdDurationMap` style.

### Schema changes

- `listing-json/.../commands/listing.command.hearingListingNeeds.json` — add optional
  `tier`, `listType`, `keyReason` (plain strings). Inherited by
  `listing.events.next-hearing-requested` and `listing.command.list-next-hearing` via
  `$ref`.
- `listing-json/.../hearing.json` (listing events) — add the same three fields, so they
  reach the view store.
- Unscheduled wrappers — sibling `ptphDetails: [{ hearingId, tier, listType, keyReason }]`
  on `listing.command.list-unscheduled-next-hearings-enriched` (command-api and
  command-handler copies), `listing.events.unscheduled-next-hearing-requested`
  (event-processor and event-listener copies) and
  `listing.command.list-unscheduled-next-hearing`. Per-hearing entries, because one
  command can carry several hearings and only the trials are stamped.

All these schemas are `additionalProperties: false`, so each addition is explicit. All
new fields are optional, so existing callers are unaffected.

### Domain and aggregate

- New `PtphDetail` value object in `listing-domain-common` (`tier`, `listType`,
  `keyReason`).
- `CommandToDomainConverter` maps it onto the domain `Hearing`.
- Threaded onto `HearingListed` via `NewDomainToEventConverter` on both the scheduled
  (`Hearing.java:490`) and unscheduled (`Hearing.java:604`) paths.
- `Hearing.list(...)` already takes ~28 parameters, so this is added as **one**
  value-object parameter rather than three loose strings.

### Persistence — no migration

`HearingEventListener.hearingListed` serialises the event's whole hearing object into the
`hearing.properties` jsonb column. Adding the fields to listing's `hearing.json` puts them
in the view store with no Liquibase changeset. LPT-2406 reads `properties`, and may
promote the fields to real columns later if the calendar needs indexed filtering.

## Failure behaviour

| Situation | Result |
|---|---|
| Next hearing is not a trial type | No hearing-context call; nothing inherited |
| Trial, `finalised = true` | `tier` + `listType` + `keyReason` inherited |
| Trial, successful response with `finalised = false` | Continue; nothing inherited. Covers both "no record" and "draft" |
| Trial, query throws or times out | Propagate — the command fails |
| Mixed trial and non-trial hearings in one command | Only the trial hearings stamped |

There is no "not found" error case to handle: `HearingService.getPtphDetail` in the
hearing context returns `new PtphDetailResponse(null, null, null, false)` when no row
exists, so a missing record arrives as a **successful** response with `finalised = false`
and is already covered by the finalised gate.

Failures therefore propagate rather than being swallowed, for three reasons:

1. An exception can only mean a genuine fault, so catching it would mask real faults and
   nothing else.
2. A swallowed failure produces a blank trial hearing indistinguishable from a
   legitimately blank one — an invisible data-quality defect.
3. It matches every neighbouring enrichment call: `CourtScheduleEnrichmentService`,
   `ReferenceDataService` and `HearingTypeFactory` contain no `try`/`catch`, so a
   reference-data or court-scheduler outage already fails these commands.

Operationally, `standalone-listing.xml` configures redelivery with `jms.queue.DLQ`, so a
transient timeout is absorbed by redelivery and only a sustained hearing-context outage
reaches the DLQ for replay. Accepted trade-off: a sustained outage blocks trial
next-hearing listings rather than degrading them.

## Testing

| Test | Asserts |
|---|---|
| `PtphDetailServiceTest` | Query name and payload; `finalised = true` → value; `finalised = false` → empty; exception propagates |
| `PtphDetailEnrichmentServiceTest` | No trial in command → `verifyNoInteractions` on the requester; trial + finalised → stamped; trial + not finalised → untouched; mixed → only trials stamped; inbound values overwritten |
| `HearingTypeFactoryTest` | `trialTypeFlag` true / false / absent |
| Aggregate tests | `HearingListed` carries the values on the scheduled and unscheduled paths |
| Listener test | `hearing.properties` contains `tier`, `listType`, `keyReason` after `hearing-listed` |
| Integration test | Off the existing `list-next-hearings-v2` fixtures; plus one proving the delete-previous round-trip inherits |

## Blocker discovered during implementation (2026-08-11)

The hearing-context call cannot be dispatched at runtime yet, and this was not visible at
design time.

`PtphDetailService` uses the framework `Requester`, which routes an action name to a REST
client **generated from the remote context's RAML** by `rest-client-generator-plugin`.
`listing-command/listing-command-api/pom.xml` lists only `referencedata-query-api` as a
plugin dependency, so listing has `RemoteCommandApi2ReferencedataQueryApi` with an
`@Handles` per referencedata action and **nothing that handles `hearing.get-ptph-detail`**.

Adding the hearing artefact does not resolve it today: no published `hearing-query-api`
RAML contains `/hearings/{hearingId}/ptph-detail` — the endpoint is implemented in
cpp-context-hearing but still unmerged on `feature/pd`, so it has never been released.

**To unblock, in order:**

1. Merge and release cpp-context-hearing including the `hearing.get-ptph-detail` query.
2. Add that artefact (classifier `raml`) to the `rest-client-generator-plugin` dependencies
   in `listing-command/listing-command-api/pom.xml`, alongside `referencedata-query-api`.
3. Remove `@Disabled` from `PtphDetailOnNextHearingIT`.

Everything either side of that hop is built and unit-tested: the trial and finalised gates,
the stamping, both command flows, the aggregate threading and the view-store serialisation.

## Open item

Flow 3 re-enters listing via progression. If that re-entry uses
`listing.command.list-court-hearing` rather than `listing.list-next-hearings-v2`, there is
no top-level `seedingHearing` on that command — seeding information sits per-offence
(`CommandToDomainConverter.buildSeedingHearing`). To be confirmed during implementation.
If confirmed, fall back to the offence-level `seedingHearing.seedingHearingId` so flow 3
does not silently fail to inherit.
