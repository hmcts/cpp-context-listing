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
| `public.listing.hearing-listed` | `cpp-context-businessprocesses`, `cpp-context-notification` |
| `public.listing.hearing-added-to-case` | `cpp-context-notification` |
| `public.listing.court-application-added-for-hearing` | `cpp-context-notification` |

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
| `public.listing.hearing-listed` | `cpp-context-businessprocesses`, `cpp-context-notification` |
| `public.listing.court-application-added-for-hearing` | `cpp-context-notification` |
| `public.listing.hearing-added-to-case` | `cpp-context-notification` |
| `public.listing.cases-added-to-hearing` | `cpp-context-notification`, `cpp-context-progression` |
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
| `public.events.listing.allocated-hearing-deleted` | `cpp-context-hearing`, `cpp-context-progression` |
| `public.listing.hearing-listed` | `cpp-context-businessprocesses`, `cpp-context-notification` |
| `public.listing.court-application-added-for-hearing` | `cpp-context-notification` |
| `public.listing.hearing-added-to-case` | `cpp-context-notification` |
| `public.listing.cases-added-to-hearing` | `cpp-context-notification`, `cpp-context-progression` |
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
| **Outbound** | `public.listing.hearing-added-to-case` | → `cpp-context-notification` |
| **Outbound** | `public.listing.court-application-added-for-hearing` | → `cpp-context-notification` |

---

## Complete Integration Reference

Comprehensive view of all cross-context integration points for cpp-context-listing. Data sourced from microservice-analyzer and subscriptions-descriptor.yaml.

For a visual diagram of all integration points, see [integration-map.mmd](./integration-map.mmd).

### All Public Events Listing Publishes

| Public Event | Triggered By (internal event) | Subscribers |
|---|---|---|
| `public.listing.hearing-listed` | `listing.events.hearing-listed` | businessprocesses, notification |
| `public.listing.hearing-confirmed` | `listing.events.hearing-allocated-for-listing`, `listing.events.hearing-allocated-for-listing-v2`, `listing.events.allocated-hearing-extended-for-listing(-v2)` | notification, progression |
| `public.listing.hearing-updated` | `listing.events.allocated-hearing-updated-for-listing(-v2)` | notification, progression |
| `public.listing.hearing-changes-saved` | `listing.events.hearing-allocated-for-listing(-v2)`, `listing.events.hearing-partially-updated`, `listing.events.hearing-changes-saved`, `listing.events.hearing-days-changed-for-hearing` | notification |
| `public.listing.hearing-partially-updated` | `listing.events.hearing-partially-updated` | notification, progression |
| `public.listing.hearing-requested-for-listing` | `listing.events.hearing-requested-for-listing` | progression |
| `public.listing.hearing-added-to-case` | `listing.events.hearing-added-to-case` | notification |
| `public.listing.hearing-days-changed-for-hearing` | `listing.events.hearing-days-changed-for-hearing` | notification |
| `public.listing.hearing-days-sequenced` | `listing.events.hearing-days-sequenced` | notification |
| `public.listing.hearings-update-completed` | `listing.events.hearings-update-completed` | notification |
| `public.listing.court-application-added-for-hearing` | `listing.events.hearing-listed`, `listing.events.court-application-added-for-hearing` | notification |
| `public.listing.court-list-restricted` | `listing.events.court-list-restricted` | hearing, notification |
| `public.listing.court-list-published` | (court list publish flow) | progression |
| `public.listing.court-daily-list` | (court list export flow) | _(none)_ |
| `public.listing.cases-added-to-hearing` | `listing.event.cases-added-to-hearing` | notification, progression |
| `public.listing.new-defendant-added-for-court-proceedings` | `listing.events.new-defendant-added-for-court-proceedings` | progression |
| `public.listing.create-next-hearing-requested` | `listing.events.create-next-hearing-requested` | progression |
| `public.listing.judiciary-changed-for-hearings-status` | `listing.events.judiciary-changed-for-hearings-status` | notification |
| `public.listing.created-listing-note` | `listing.events.created-listing-note` | notification |
| `public.listing.note-edited` | `listing.events.listing-note-edited` | notification |
| `public.listing.deleted-listing-note` | `listing.events.deleted-listing-note` | notification |
| `public.listing.vacated-trial-updated` | `listing.events.trial-vacated`, `listing.events.hearing-rescheduled` | hearing, progression |
| `public.listing.offences-moved-to-next-hearing` | `listing.events.next-hearing-replaced` | progression |
| `public.listing.hearing-unallocated-courtroom-removed` | `listing.events.hearing-unallocated-courtroom-removed` | hearing, progression |
| `public.events.listing.allocated-hearing-deleted` | `listing.events.allocated-hearing-deleted` | hearing, progression |
| `public.events.listing.unallocated-hearing-deleted` | `listing.events.unallocated-hearing-deleted` | progression |
| `public.events.listing.hearing-deleted` | `listing.events.hearing-deleted` | progression |
| `public.events.listing.hearing-days-without-court-centre-corrected` | `listing.events.hearing-days-without-court-centre-corrected` | progression |
| `public.events.listing.cases-added-for-updated-related-hearing` | `listing.event.cases-added-to-hearing` | progression |
| `public.events.listing.next-hearing-day-changed` | `listing.events.next-hearing-day-changed` | hearing |
| `public.events.listing.hearing-unallocated` | `listing.events.hearing-unallocated-for-listing` | hearing, progression |
| `public.events.listing.offences-removed-from-allocated-hearing` | `listing.events.offences-removed-from-existing-allocated-hearing` | progression |
| `public.events.listing.offences-removed-from-existing-allocated-hearing` | `listing.events.offences-removed-from-hearing`, `listing.events.offences-removed-from-existing-allocated-hearing` | hearing, progression |
| `public.events.listing.offences-removed-from-existing-unallocated-hearing` | `listing.events.offences-removed-from-existing-unallocated-hearing` | progression |
| `public.events.listing.offences-removed-from-unallocated-hearing` | `listing.events.offences-removed-from-hearing` | progression |

### All Public Events Listing Subscribes To

| Public Event | Published By | Handler in Listing |
|---|---|---|
| `public.events.hearing.hearing-resulted` | hearing, sjp | `HearingResultedEventProcessor` |
| `public.events.hearing.marked-as-duplicate` | hearing | `MarkHearingAsDuplicateEventProcessor` |
| `public.hearing.trial-vacated` | hearing | `ListingEventProcessor` |
| `public.hearing.hearing-days-cancelled` | hearing | `CancelHearingDaysEventProcessor` |
| `public.hearing.defence-counsel-added` | hearing | `CounselModifiedEventProcessor` |
| `public.hearing.defence-counsel-updated` | hearing | `CounselModifiedEventProcessor` |
| `public.hearing.defence-counsel-removed` | hearing | `CounselModifiedEventProcessor` |
| `public.hearing.prosecution-counsel-added` | hearing | `CounselModifiedEventProcessor` |
| `public.hearing.prosecution-counsel-updated` | hearing | `CounselModifiedEventProcessor` |
| `public.hearing.prosecution-counsel-removed` | hearing | `CounselModifiedEventProcessor` |
| `public.hearing.selected-offences-removed-from-existing-hearing` | hearing | `RemoveSelectedOffencesEventProcessor` |
| `public.progression.defendant-offences-changed` | progression | `ListingEventProcessor` |
| `public.progression.case-defendant-changed` | progression | `ListingEventProcessor` |
| `public.progression.court-application-changed` | progression | `ListingEventProcessor` |
| `public.progression.defendants-added-to-court-proceedings` | progression | `ListingEventProcessor` |
| `public.progression.case-markers-updated` | progression | `ListingEventProcessor` |
| `public.progression.hearing-resulted-case-updated` | progression | `ListingEventProcessor` |
| `public.progression.defendant-legalaid-status-updated` | progression | `ListingEventProcessor` |
| `public.progression.case-linked` | progression | `ListingEventProcessor` |
| `public.progression.case-removed-from-group-cases` | progression | `CaseRemovedFromGroupCasesEventProcessor` |
| `public.progression.application-offences-updated` | progression | `ListingEventProcessor` |
| `public.progression.application-laa-reference-updated-for-application` | progression | `ListingEventProcessor` |
| `public.progression.events.case-or-application-ejected` | progression | `EjectCaseEventProcessor` |
| `public.progression.events.cps-prosecutor-updated` | progression | `CpsProsCounselEventProcessor` |
| `public.progression.events.hearing-extended` | progression | `ListingEventProcessor` |
| `public.progression.events.court-application-deleted` | progression | `CourtApplicationDeletedEventProcessor` |
| `public.progression.related-hearing-updated-for-adhoc-hearing` | progression | `ListingEventProcessor` |
| `public.referencedata.event.courtroom-added` | reference-data | `CacheRefDataCourtroomView` |
| `public.referencedata.event.courtroom-closed` | reference-data | `CacheRefDataCourtroomView` |

### Outbound REST API Calls (listing → other services)

| Target Service | Endpoint | Called From | Purpose |
|---|---|---|---|
| **courtscheduler** | `GET /courtschedule/search.court-schedules-by-id` | `CourtSchedulerService` | Fetch court schedules for enrichment |
| **courtscheduler** | `GET /hearingslots` | `HearingSlotsService`, `CourtSchedulerServiceAdapter` | Search hearing slots / judicial roles |
| **courtscheduler** | `GET /searchlist/hearingslots` | `HearingSlotsService` | Search and book hearing slots |
| **courtscheduler** | `GET /multidaysearchandbook/hearingslots` | `HearingSlotsService` | Multi-day search and book |
| **courtscheduler** | `PUT /list/hearingslots` | `HearingSlotsService` | List hearings in court sessions |
| **courtscheduler** | `POST /validate-session-availability` | `HearingSlotsService` | Validate session availability |
| **courtscheduler** | `GET /provisionalBooking` | `ProvisionalBookingService` | Get provisional booking slots |
| **courtscheduler** | `DELETE /provisionalBooking` | `ProvisionalBookingService` | Remove provisional booking |
| **progression** | `progression.query.prosecutioncase` | `ProgressionService` (Requester) | Get prosecution case details |
| **progression** | `progression.query.case-notes` | `ProgressionService` (Requester) | Get case notes |
| **progression** | `progression.query.application-notes` | `ProgressionService` (Requester) | Get application notes |
| **defence** | `defence.query.get-case-by-person-defendant` | `HearingQueryApi` (Requester) | Search cases by person defendant |
| **defence** | `defence.query.get-case-by-organisation-defendant` | `HearingQueryApi` (Requester) | Search cases by organisation defendant |
| **reference-data** | `referencedata.query.courtroom` | `ReferenceDataService` (Requester) | Get court centre details |
| **reference-data** | `referencedata.query.judiciaries` | `ReferenceDataService` (Requester) | Get judiciary names |
| **reference-data** | `referencedata.query.hearing-types` | `ReferenceDataService` (Requester) | Get hearing type descriptions |
| **reference-data** | `referencedata.query.courtrooms` | `CacheRefDataCourtroomLoader` (Requester) | Load all courtrooms for cache |
| **users-groups** | `usersgroups.is-logged-in-user-has-permission-for-action` | `UsersGroupsService` (Requester) | Check user permissions |
| **document-generator** | `generateDocument()` | `DocumentGeneratorClient` | Generate court list PDFs |
| **xhibit-gateway** | `WebDAV PUT` | `XhibitSession` | Send court list exports to Xhibit |

### Cross-Context Commands (other services → listing)

| Command | Sent By | Handler in Listing |
|---|---|---|
| `listing.command.list-court-hearing` | progression | `ListingCommandApi.handleListCourtHearing()` |
| `listing.command.list-unscheduled-court-hearing` | progression | `ListingCommandApi.handleListUnscheduledCourtHearing()` |
