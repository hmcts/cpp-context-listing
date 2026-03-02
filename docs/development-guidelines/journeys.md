# Journeys & Touch Points

Reference document for end-to-end flows that touch the listing context.

---

## 1. Adhoc Hearing

Triggered when a user requests a new adhoc hearing from the prosecution casefile UI.

**UI origin:** `cpp-ui-prosecution-casefile` → `progression.service.ts` (commandSync, expects `public.listing.hearing-confirmed`)

### Flow

#### Step 1 — Progression: Command API
- **Endpoint:** `progression.list-new-hearing` (POST `/defendant/{defendantId}/courtdocument/{materialId}`)
- **Handler:** `ListNewHearingApi.handle()`
- **Sends command:** `progression.command.list-new-hearing`

#### Step 2 — Progression: Command Handler
- **Handler:** `ListNewHearingHandler.handle()`
- **Raises events:**
  - `progression.event.list-hearing-requested` → continues to **Step 3**
  - `progression.event.related-case-requested-for-adhoc-hearing` → see [Side Branch A](#side-branch-a--related-cases)

#### Step 3 — Progression: Event Processor
- **Handler:** `ListHearingRequestedProcessor.handle()`
- **Sends command (cross-context):** `listing.command.list-court-hearing` → enters **listing context**

#### Step 4 — Listing: Command API
- **Handler:** `ListingCommandApi.handleListCourtHearing()`
- **Sends command:** `listing.command.list-court-hearing-enriched`

#### Step 5 — Listing: Command Handler
- **Handler:** `ListingCommandHandler.listCourtHearing()`
- **Raises events:**
  - `listing.event.court-centre-details` (aggregate event, no downstream processor)
  - `listing.events.hearing-listed` → continues to **Step 6**

#### Step 6 — Listing: Event Processor + Listener (hearing-listed)

**Event Processor** — `ListingEventProcessor.handleHearingListedMessage()`:
- Sends commands:
  - `listing.command.add-hearing-to-case` → **Step 7**
  - `listing.command.add-court-application-to-hearing` → **Step 8**
- Publishes public events:
  - `public.listing.hearing-listed` → consumed by `cpp-context-businessprocesses` (`ListingHearingListedEventProcessor`)
  - `public.listing.court-application-added-for-hearing`

**Event Listener** — `HearingEventListener.hearingListed()`:
- Same commands as above (used for viewstore updates)

**External Listener** — `cpp-context-mi-reportdata` (`HearingListedEventListener.handleHearingListed()`)

#### Step 7 — Listing: Add Hearing to Case
- **Handler:** `ListingCommandHandler.addHearingToCase()`
- **Raises events:**
  - `listing.events.defendants-to-be-updated` → **Step 7a**
  - `listing.events.hearing-added-to-case` → **Step 7b**

##### Step 7a — Update Defendants
- **Event Processor:** `ListingEventProcessor.handleDefendantsToBeUpdatedMessage()`
- **Sends command:** `listing.command.update-defendants-for-hearing`
- **Handler:** `ListingCommandHandler.updateDefendantsForHearing()` (terminal)

##### Step 7b — Hearing Added to Case
- **Event Processor:** `ListingEventProcessor.handleHearingAddedToCase()`
- **Publishes:** `public.listing.hearing-added-to-case`

#### Step 8 — Listing: Add Court Application to Hearing
- **Handler:** `ListingCommandHandler.addCourtApplicationToHearing()`
- **Raises event:** `listing.event.court-application-added-to-hearing` (terminal)

### Side Branch A — Related Cases

Internal to progression. Handles related cases that need to be listed alongside the adhoc hearing.

1. `progression.event.related-case-requested-for-adhoc-hearing`
   → `RelatedHearingEventProcessor.processRelatedCaseRequestedForAdhocHearing()`
   → sends `progression.command.request-related-hearing-for-adhoc-hearing`
2. `ListNewHearingHandler.handleRequestRelatedHearingForAdhocHearing()`
   → raises `progression.event.related-hearing-requested-for-adhoc-hearing`
3. `RelatedHearingEventProcessor.processRelatedHearingRequestedForAdhocHearing()`
   → sends `progression.command.update-related-hearing-for-adhoc-hearing`
4. `RelatedHearingCommandHandler.handleUpdateRelatedHearingCommandForAdhocHearing()` (terminal)

### Side Branch B — Court Document

Also triggered by `ListNewHearingHandler.handle()` in progression (not part of the listing flow).

1. Sends `progression.command.add-court-document`
   → `AddCourtDocumentHandler.handle()` raises multiple document-related events
   → publishes `public.progression.event.court-document-shared`, `public.progression.events.court-document-updated`

### Summary of Public Events Emitted by Listing

| Event | Consumed By |
|---|---|
| `public.listing.hearing-listed` | `cpp-context-businessprocesses` |
| `public.listing.hearing-added-to-case` | — |
| `public.listing.court-application-added-for-hearing` | — |

---

## 2. Unallocated to Allocated Hearing

Triggered when a listing officer allocates an unallocated hearing to a courtroom/date. Uses the same endpoint as [Update Allocated Hearing](#3-update-allocated-hearing). The aggregate determines which events to raise based on the hearing's current state.

### Flow

#### Step 1 — Listing: Command API
- **Endpoint:** `listing.command.update-hearing-for-listing` (POST `/hearings/{hearingId}`)
- **Handler:** `ListingCommandApi.handleUpdateHearingForListing()`
- **Sends command:** `listing.command.update-hearing-for-listing-enriched`

#### Step 2 — Listing: Command Handler (Hearing aggregate)
- **Handler:** `ListingCommandHandler.updateHearingForListing()`
- **Raises aggregate events (conditional based on state):**
  - `listing.events.hearing-listed` → **Step 3** (the hearing is now allocated/listed)
  - `listing.event.cases-added-to-hearing` → **Step 4**
  - `listing.event.added-cases-for-hearing` → listener: `ExtendHearingForHearingListener.hearingAddedCasesForHearing()` (viewstore)
  - `listing.event.hearing-updated-to-case` (aggregate on Case, no downstream processor)
  - `listing.event.court-application-added-to-hearing` (aggregate-only)
  - `listing.event.master-case-updated-for-group` (aggregate-only)
  - `listing.event.defendants-to-be-updated-later` (aggregate-only)
  - `listing.event.case-resulted-defendant-proceedings-concluded` (aggregate-only)
  - `listing.event.sequences-reset-on-hearing-days` (aggregate-only)

#### Step 3 — Listing: Event Processor + Listener (hearing-listed)

Same as [Adhoc Hearing Step 6](#step-6--listing-event-processor--listener-hearing-listed):

**Event Processor** — `ListingEventProcessor.handleHearingListedMessage()`:
- Sends commands:
  - `listing.command.add-hearing-to-case` → see [Adhoc Hearing Step 7](#step-7--listing-add-hearing-to-case)
  - `listing.command.add-court-application-to-hearing` → see [Adhoc Hearing Step 8](#step-8--listing-add-court-application-to-hearing)
- Publishes public events:
  - `public.listing.hearing-listed` → consumed by `cpp-context-businessprocesses`
  - `public.listing.court-application-added-for-hearing`

**Event Listener** — `HearingEventListener.hearingListed()` (viewstore)

**External Listener** — `cpp-context-mi-reportdata`

#### Step 4 — Listing: Event Processor (cases-added-to-hearing)
- **Handler:** `UpdateExistingHearingEventProcessor.handleCasesAddedToHearingEvent()`
- **Sends command:** `listing.command.add-hearing-to-case` → see [Adhoc Hearing Step 7](#step-7--listing-add-hearing-to-case)
- **Publishes public events:**
  - `public.listing.cases-added-to-hearing` → consumed by `cpp-context-progression` (`ExtendedHearingProcessor.addCasesToUnAllocatedHearing()`)
  - `public.events.listing.cases-added-for-updated-related-hearing` → consumed by `cpp-context-progression` (`RelatedHearingEventProcessor`)

**Event Listener** — `ExtendHearingForHearingListener.handleCasesAddedToHearingEvent()` (viewstore)

**External Listener** — `cpp-context-mi-reportdata` (`CasesAddedToHearingEventListener`)

### Summary of Public Events Emitted by Listing

| Event | Consumed By |
|---|---|
| `public.listing.hearing-listed` | `cpp-context-businessprocesses` |
| `public.listing.court-application-added-for-hearing` | — |
| `public.listing.hearing-added-to-case` | — |
| `public.listing.cases-added-to-hearing` | `cpp-context-progression` |
| `public.events.listing.cases-added-for-updated-related-hearing` | `cpp-context-progression` |

---

## 3. Update Allocated Hearing

Triggered when a listing officer updates an already-allocated hearing (e.g. changing courtroom, date, judiciary, or hearing details). Uses the same endpoint as [Unallocated to Allocated](#2-unallocated-to-allocated-hearing).

### Flow

#### Step 1 — Listing: Command API
- **Endpoint:** `listing.command.update-hearing-for-listing` (POST `/hearings/{hearingId}`)
- **Handler:** `ListingCommandApi.handleUpdateHearingForListing()`
- **Sends command:** `listing.command.update-hearing-for-listing-enriched`

#### Step 2 — Listing: Command Handler (Hearing aggregate)
- **Handler:** `ListingCommandHandler.updateHearingForListing()`
- **Raises aggregate events (conditional based on state):**
  - `listing.events.allocated-hearing-deleted` → **Step 3** (the old allocated hearing is replaced)
  - `listing.events.hearing-listed` → **Step 4** (the updated hearing is re-listed)
  - `listing.event.cases-added-to-hearing` → **Step 5**
  - `listing.event.added-cases-for-hearing` → listener: `ExtendHearingForHearingListener` (viewstore)
  - `listing.event.hearing-updated-to-case` (aggregate on Case, no downstream processor)
  - `listing.event.hearing-listed-case-updated` (aggregate-only)
  - `listing.event.hearing-marked-as-duplicate-for-case` (aggregate-only)
  - `listing.event.court-application-added-to-hearing` (aggregate-only)
  - `listing.event.master-case-updated-for-group` (aggregate-only)
  - `listing.event.defendants-to-be-updated-later` (aggregate-only)
  - `listing.event.case-resulted-defendant-proceedings-concluded` (aggregate-only)
  - `listing.event.sequences-reset-on-hearing-days` (aggregate-only)
  - `listing.event.court-list-export-requested` → processor: `CourtListEventProcessor` (Xhibit court list side effect)

#### Step 3 — Listing: Event Processor (allocated-hearing-deleted)
- **Handler:** `NextHearingProcessor.handleAllocatedHearingDeleted()`
- **Sends command:** `listing.command.mark-hearing-as-duplicate-for-case`
- **Publishes:** `public.events.listing.allocated-hearing-deleted` → consumed by `cpp-context-hearing` (`HearingDeletedEventProcessor.handleHearingDeletedPublicEvent()` → `hearing.command.delete-hearing`)

**Event Listener** — `HearingMarkedAsDuplicateEventListener.handleAllocatedHearingDeleted()` (viewstore)

**External Listener** — `cpp-context-mi-reportdata` (`HearingDeletedEventListener`)

#### Step 4 — Listing: Event Processor + Listener (hearing-listed)

Same as [Adhoc Hearing Step 6](#step-6--listing-event-processor--listener-hearing-listed):

**Event Processor** — `ListingEventProcessor.handleHearingListedMessage()`:
- Sends commands:
  - `listing.command.add-hearing-to-case` → see [Adhoc Hearing Step 7](#step-7--listing-add-hearing-to-case)
  - `listing.command.add-court-application-to-hearing` → see [Adhoc Hearing Step 8](#step-8--listing-add-court-application-to-hearing)
- Publishes public events:
  - `public.listing.hearing-listed` → consumed by `cpp-context-businessprocesses`
  - `public.listing.court-application-added-for-hearing`

**Event Listener** — `HearingEventListener.hearingListed()` (viewstore)

#### Step 5 — Listing: Event Processor (cases-added-to-hearing)

Same as [Unallocated to Allocated Step 4](#step-4--listing-event-processor-cases-added-to-hearing):

- **Handler:** `UpdateExistingHearingEventProcessor.handleCasesAddedToHearingEvent()`
- **Sends command:** `listing.command.add-hearing-to-case`
- **Publishes:**
  - `public.listing.cases-added-to-hearing` → consumed by `cpp-context-progression`
  - `public.events.listing.cases-added-for-updated-related-hearing` → consumed by `cpp-context-progression`

### Summary of Public Events Emitted by Listing

| Event | Consumed By |
|---|---|
| `public.events.listing.allocated-hearing-deleted` | `cpp-context-hearing` |
| `public.listing.hearing-listed` | `cpp-context-businessprocesses` |
| `public.listing.court-application-added-for-hearing` | — |
| `public.listing.hearing-added-to-case` | — |
| `public.listing.cases-added-to-hearing` | `cpp-context-progression` |
| `public.events.listing.cases-added-for-updated-related-hearing` | `cpp-context-progression` |

---

## 4. Adjournment

Triggered when a judge adjourns a hearing and the results are shared. This is a complex cross-context flow; only the listing integration points are documented here.

**UI origin:** `cpp-ui-hearing` → `results.service.ts` (commandSync, expects `public.events.hearing.hearing-resulted-success`)

### Overview

The flow starts in hearing context with `hearing.share-results-v2`. Hearing processes results and publishes public events. These reach listing through **three separate paths**:

- **Path A** — `public.events.hearing.hearing-resulted` → listing directly (set resulted status)
- **Path B** — `public.events.hearing.hearing-resulted` → progression → `public.progression.hearing-resulted-case-updated` → listing (update defendant proceedings concluded)
- **Path C** — `public.hearing.adjourned` → progression → `listing.command.list-court-hearing` (list the adjourned/next hearing)

### Hearing Context (origin)

#### Step 1 — Hearing: Command API
- **Endpoint:** `hearing.share-results-v2` (POST `/hearings/{hearingId}/share-results`)
- **Handler:** `HearingCommandApi.shareResultsV2()`
- **Sends command:** `hearing.command.share-results-v2`

#### Step 2 — Hearing: Command Handler
- **Handler:** `ShareResultsCommandHandler.shareResultV2()`
- **Raises event:** `hearing.events.results-shared-v2`

#### Step 3 — Hearing: Event Processor
- **Handler:** `PublishResultsV2EventProcessor.resultsShared()`
- **Publishes:** `public.events.hearing.hearing-resulted-success` (UI acknowledgement)
- **Sends command:** `hearing.command.handler.update-offence-results`
- Internally also raises `hearing.event.hearing-adjourned` (for adjournment results) → publishes `public.hearing.adjourned`
- Eventually publishes `public.events.hearing.hearing-resulted` (full result payload)

### Path A — Listing: Set Hearing Resulted Status

Listing directly subscribes to the hearing resulted public event.

#### Step A1 — Listing: Event Processor
- **Subscribes to:** `public.events.hearing.hearing-resulted` (from `cpp-context-hearing`)
- **Handler:** `HearingResultedEventProcessor.handlePublicHearingResulted()`
- **Logic:** Skips SJP hearings. Sends `listing.command.set-hearing-resulted-status` with the hearingId.

#### Step A2 — Listing: Command Handler
- **Handler:** `HearingResultedCommandHandler.handleSetHearingResultStatus()`
- **Raises aggregate events on Hearing:**
  - `listing.event.cases-added-to-hearing` → processor: `UpdateExistingHearingEventProcessor` → publishes `public.listing.cases-added-to-hearing`, `public.events.listing.cases-added-for-updated-related-hearing`
  - `listing.event.added-cases-for-hearing` → listener: `ExtendHearingForHearingListener` (viewstore)
  - `listing.event.case-resulted-defendant-proceedings-concluded` (aggregate-only)

### Path B — Listing: Update Case Resulted Defendant Proceedings Concluded

This path goes hearing → progression → listing.

#### Step B1 — Progression: Event Processor
- **Subscribes to:** `public.events.hearing.hearing-resulted` (from `cpp-context-hearing`)
- **Handler:** `HearingResultedEventProcessor.handlePublicHearingResulted()`
- **Sends command:** `progression.command.process-hearing-results`

#### Step B2 — Progression: Internal Processing
- `HearingResultsCommandHandler.processHearingResults()` raises `progression.event.hearing-resulted`
- `HearingResultedEventProcessor.processEvent()` publishes `public.progression.hearing-resulted`
- Eventually `progression.command.hearing-resulted-update-case` → `UpdateCaseHandler` → raises `progression.event.hearing-resulted-case-updated`
- Published as `public.progression.hearing-resulted-case-updated` (via publications descriptor)

#### Step B3 — Listing: Event Processor
- **Subscribes to:** `public.progression.hearing-resulted-case-updated` (from `cpp-context-progression`)
- **Handler:** `ListingEventProcessor.handleHearingResultedAndCaseUpdated()`
- **Sends command:** `listing.command.update-case-resulted-defendant-proceedings-concluded`

#### Step B4 — Listing: Command Handler
- **Handler:** `ListingCommandHandler.updateDefendantHearingResultedAndCaseResulted()`
- **Raises aggregate events on Hearing:**
  - `listing.event.cases-added-to-hearing` → processor: `UpdateExistingHearingEventProcessor` → publishes `public.listing.cases-added-to-hearing`, `public.events.listing.cases-added-for-updated-related-hearing`
  - `listing.event.added-cases-for-hearing` → listener: `ExtendHearingForHearingListener` (viewstore)
  - `listing.event.case-resulted-defendant-proceedings-concluded` (aggregate-only)

### Path C — Listing: List the Adjourned/Next Hearing

When the result is an adjournment, progression creates the next hearing in listing.

#### Step C1 — Progression: Event Processor
- **Subscribes to:** `public.hearing.adjourned` (from `cpp-context-hearing`)
- **Handler:** `AdjournHearingEventProcessor.handleHearingAdjournedPublicEvent()`
- **Sends command (cross-context):** `listing.command.list-court-hearing`

#### Step C2 — Listing: Same as Adhoc Hearing Steps 4-8

The flow from `listing.command.list-court-hearing` is identical to the [Adhoc Hearing](#1-adhoc-hearing) flow starting at Step 4:
- `ListingCommandApi.handleListCourtHearing()` → enriched → `ListingCommandHandler.listCourtHearing()`
- Raises `listing.events.hearing-listed` → processor publishes `public.listing.hearing-listed`, adds hearing to case, etc.

### Summary of Listing Integration Points

| Direction | Event | Handler in Listing |
|---|---|---|
| **Inbound** | `public.events.hearing.hearing-resulted` | `HearingResultedEventProcessor.handlePublicHearingResulted()` |
| **Inbound** | `public.progression.hearing-resulted-case-updated` | `ListingEventProcessor.handleHearingResultedAndCaseUpdated()` |
| **Inbound** | `listing.command.list-court-hearing` (from progression) | `ListingCommandApi.handleListCourtHearing()` |
| **Outbound** | `public.listing.cases-added-to-hearing` | → `cpp-context-progression` |
| **Outbound** | `public.events.listing.cases-added-for-updated-related-hearing` | → `cpp-context-progression` |
| **Outbound** | `public.listing.hearing-listed` | → `cpp-context-businessprocesses` |
| **Outbound** | `public.listing.hearing-added-to-case` | — |
| **Outbound** | `public.listing.court-application-added-for-hearing` | — |
