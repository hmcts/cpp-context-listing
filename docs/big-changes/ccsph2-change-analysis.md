# CCSP H2 Change Analysis: Court Schedule Integration for CROWN

Comparison of `team/ccsph2` vs `team/hmiremoval` (baseline).
Only substantive changes are listed — trivial refactors (pom version bumps, `JsonObjects` → `javax.json.Json` static import migrations, whitespace) are excluded from analysis.

---

## Legend

- **Journey**: Which journey from `journeys.md` is affected
- **Verdict**: NEEDED / QUESTIONABLE / BUG / CLEANUP
- **Status**: PENDING APPROVAL

---

## Change 1: `CourtScheduleEnrichmentService` — `needsCourtScheduleEnrichment()` now includes CROWN

**File:** `listing-command/listing-command-api/src/main/java/.../CourtScheduleEnrichmentService.java`

**What changed:** Previously only returned `true` for MAGISTRATES. Now also returns `true` for CROWN when hearing days are present or the hearing is a candidate for allocation.

**Journey:** Adhoc Hearing (1), Unallocated→Allocated (2), Update Allocated (3), Adjournment Path C (4)

**Analysis:** This is the core gate that enables court schedule enrichment for CROWN. Without this, no CROWN hearing would go through court schedule enrichment. This is **required** for the feature.

**Potential issue:** For CROWN, the condition is `!isEmpty(hearing.getHearingDays()) || isCandidateForAllocation(hearing)`. This means even CROWN hearings that are week-commencing (which you said should be excluded) could enter enrichment if they have hearing days populated. The exclusion of week-commencing must happen inside the enrichment logic (and it does — see Change 2). This is acceptable but could be made explicit here for clarity.

**Verdict:** NEEDED
**Status:** APPROVED

---

## Change 2: `CourtScheduleEnrichmentService` — New CROWN enrichment methods

**File:** `listing-command/listing-command-api/src/main/java/.../CourtScheduleEnrichmentService.java`

**What changed:** Added ~350 lines of new methods:
- `isListNextHearingsV2Scenario()` / `handleListNextHearingsV2Enrichment()` — handles enrichment for list-next-hearings-v2 (Adjournment path C)
- `handleAllocationCandidateWithAssignedSessions()` — searches for assigned sessions (SHRT/LNG business types) for allocated hearings
- `handleUnallocatedWithDraftSessions()` — searches for draft sessions for unallocated hearings
- `handleDirectListingCase()` — uses supplied courtScheduleIds directly
- `handleCrownMultiDaySlotSearch()` — searches slots per hearing day for multi-day hearings
- `applyCourtScheduleRulesForChangeJudiciary()` — applies court schedule rules when judiciary changes
- `getCourtScheduleDraftStatus()` — queries draft/assigned status of court schedule IDs
- Null-safety improvements in `combineSearchAndBookResponseAndListResponse()` for hearing days without courtScheduleId

**Journey:** All journeys (1-4) depending on path

**Analysis:** This is the main feature implementation. The logic correctly:
- Distinguishes between ALLOCATED (assigned sessions) and UNALLOCATED (draft sessions)
- Handles the case where courtScheduleIds are supplied by the front end vs needing to search
- Excludes week-commencing hearings from court schedule integration
- Handles multi-day hearings

**Potential issues:**
1. `handleUnallocatedWithDraftSessions()` calls `searchAndBookSlotsWithDraftStatus()` — ensure this method exists and works correctly with the court scheduler service. If the court scheduler doesn't support draft filtering, this will silently fail.
2. The `applyCourtScheduleRulesForChangeJudiciary()` method creates a new list-in-court-sessions payload and calls `hearingSlotsService.listHearingInCourtSessions()` — verify the court scheduler accepts this for CROWN jurisdiction.

**Verdict:** NEEDED
**Status:** PENDING APPROVAL

---

## Change 3: `CourtScheduleEnrichmentService` — Null-safety for `combineSearchAndBookResponseAndListResponse()`

**File:** `listing-command/listing-command-api/src/main/java/.../CourtScheduleEnrichmentService.java`

**What changed:** Added null-checks for `requestedHearingDay.getCourtScheduleId()` before calling `.toString()`. Hearing days without courtScheduleId are now added as-is instead of throwing NPE.

**Journey:** All journeys — this method is used by both MAGS and CROWN enrichment flows.

**Analysis:** This is a **bug fix**. Previously, if a hearing day didn't have a courtScheduleId, the code would NPE on `requestedHearingDay.getCourtScheduleId().toString()`. Since CROWN hearings may have some hearing days without courtScheduleIds (especially during transition), this fix is critical.

**Verdict:** NEEDED (bug fix)
**Status:** PENDING APPROVAL

---

## Change 4: `HearingEnrichmentOrchestrator` — CROWN enrichment now includes court schedules

**File:** `listing-command/listing-command-api/src/main/java/.../HearingEnrichmentOrchestrator.java`

**What changed:**
- `enrichListCourtHearing()` CROWN path: now adds `courtScheduleEnrichmentService.enrichWithCourtSchedules()` after duration enrichment
- `enrichUpdateHearing()` CROWN path: now adds court schedule enrichment + `enrichCrownWithSessionStatus()` which checks if sessions are draft and clears courtroom info accordingly
- `enrichUpdateHearingWithCourtCentre()` CROWN path: same as above
- New `enrichCrownWithSessionStatus()` method: checks draft status of court schedule IDs, clears courtRoomId if all sessions are draft (making the hearing unallocated)
- New `checkIfSessionsAreAssigned()` method: queries `getCourtScheduleDraftStatus()` and returns true if no sessions are draft

**Journey:**
- Adhoc Hearing (1): `enrichListCourtHearing()` path — now enriches CROWN with court schedules
- Unallocated→Allocated (2): `enrichUpdateHearing()` path — enriches + checks session status
- Update Allocated (3): `enrichUpdateHearingWithCourtCentre()` path — enriches + checks session status

**Analysis:** This is the orchestration layer that wires court schedule enrichment into the CROWN flow. The logic of clearing courtRoomId for draft sessions is how you achieve the "unallocated = draft, allocated = assigned" semantics. This is **correct and needed**.

**Potential issue:** In `enrichCrownWithSessionStatus()`, if `getCourtScheduleDraftStatus()` returns an empty map (e.g., court scheduler is down), the code defaults to assuming sessions are assigned (`return true`). This means a network failure could cause a hearing to be incorrectly allocated. Consider whether this should fail loudly instead.

**Verdict:** NEEDED — but consider whether silent fallback to "assigned" on network error is safe
**Status:** PENDING APPROVAL

---

## Change 5: `ListingCommandApi` — CROWN validation for `update-hearing-for-listing`

**File:** `listing-command/listing-command-api/src/main/java/.../ListingCommandApi.java`

**What changed:**
- New `validateCrownJurisdictionRequirements()` method: for CROWN, validates that all hearing days have a courtScheduleId and that all courtScheduleIds have the same state (all draft or all assigned)
- New `validateCrownListCourtHearingRequirements()` method: for CROWN list-court-hearing with multiple hearing days, validates consistency of courtScheduleId presence and state
- New `validateCourtScheduleIdsHaveSameState()` helper
- Validation called before enrichment in `handleUpdateHearingForListing()` and `handleListCourtHearing()`

**Journey:**
- Unallocated→Allocated (2): `handleUpdateHearingForListing()` validates before enrichment
- Update Allocated (3): same
- Adhoc Hearing (1): `handleListCourtHearing()` validates per hearing

**Analysis:** These validations enforce your business rules:
- All CROWN hearing days must have courtScheduleId (except single-day or no hearing days in list-court-hearing)
- All courtScheduleIds must be in the same state (all draft OR all assigned — no mix)

**Potential issue in `validateCrownListCourtHearingRequirements()`:** The validation skips hearings with ≤1 hearing day. This is fine for adhoc hearings, but for list-court-hearing with multiple cases, each `HearingListingNeeds` is validated independently. If a single-day hearing doesn't have a courtScheduleId, it passes validation silently — is this intended for CROWN? Given that front end will always supply courtScheduleIds, this may be acceptable as a safety net.

**Verdict:** NEEDED
**Status:** PENDING APPROVAL

---

## Change 6: `ListingCommandApi` — `handleChangeJudiciaryForHearings()` now resolves hearing details

**File:** `listing-command/listing-command-api/src/main/java/.../ListingCommandApi.java`

**What changed:** Previously just forwarded the envelope. Now:
1. Deserializes the `ChangeJudiciaryForHearings` payload
2. For each hearing ID, fetches the hearing from viewstore via `HearingByIdProvider`
3. Converts to `HearingListingNeeds` via `ViewstoreHearingToHearingListingNeedsConverter`
4. Calls `courtScheduleEnrichmentService.applyCourtScheduleRulesForChangeJudiciary()` to update court sessions with new judiciary
5. Still sends the original envelope to the handler

**Journey:** Not directly in our journeys.md, but `listing.command.change-judiciary-for-hearings` is a separate user action that affects CROWN hearings with court schedules.

**Analysis:** This is needed so that when judiciary changes on a CROWN hearing, the court scheduler is notified to update the sessions with the new judiciary. Without this, the court schedule sessions would have stale judiciary information.

**Potential issues:**
1. **Performance:** For each hearing, it queries the viewstore, converts, and calls the court scheduler. If a user changes judiciary for many hearings at once, this could be slow.
2. **Dependency on viewstore in command API:** `HearingByIdProvider` injects `HearingQueryView` (a query component) into the command API. This creates a read dependency in the command path, which is architecturally unusual in CQRS. However, this is pragmatic — the alternative would be replaying the aggregate for each hearing which is also expensive.
3. **The original envelope is still sent unchanged** — so the handler processes the original command regardless. The court scheduler call is a side effect. If the court scheduler call fails silently, the hearing aggregate still gets the judiciary update but the sessions are stale.

**Verdict:** NEEDED — but the architectural coupling (command API querying viewstore) and silent failure mode should be noted
**Status:** PENDING APPROVAL

---

## Change 7: `HearingByIdProvider` (new file)

**File:** `listing-command/listing-command-api/src/main/java/.../HearingByIdProvider.java`

**What changed:** New class that fetches hearing data from the viewstore by ID. Used by `handleChangeJudiciaryForHearings()`.

**Analysis:** Supporting class for Change 6. Returns `Optional.empty()` on `NotFoundException`.

**Verdict:** NEEDED (supports Change 6)
**Status:** PENDING APPROVAL

---

## Change 8: `ViewstoreHearingToHearingListingNeedsConverter` (new file)

**File:** `listing-command/listing-command-api/src/main/java/.../ViewstoreHearingToHearingListingNeedsConverter.java`

**What changed:** New class that converts viewstore hearing JSON to `HearingListingNeeds` DTO. Handles hearing days, court centre, court room, court schedule IDs, and skips cancelled hearing days.

**Analysis:** Supporting class for Change 6. Clean converter with proper null handling.

**Verdict:** NEEDED (supports Change 6)
**Status:** PENDING APPROVAL

---

## Change 9: `Hearing` aggregate — `ListingStatus` enum and `determineListingStatus()`

**File:** `listing-domain/listing-domain-aggregate/src/main/java/.../Hearing.java`

**What changed:**
- New `ListingStatus` enum: `ALLOCATED`, `UNALLOCATED`, `WEEK_COMMENCING`, `UNSCHEDULED`
- New `determineListingStatus()` static method: classifies a hearing based on its properties
- New `hasAssignedSession` field: tracks whether court schedule sessions are assigned (not draft) for CROWN

**Journey:** All journeys — this affects aggregate behavior

**Analysis:** The `ListingStatus` enum is used by `CourtScheduleEnrichmentService` to determine how to handle enrichment. The `hasAssignedSession` field is used by `canAllocateForCrown()` to determine if the hearing can be allocated.

**Verdict:** NEEDED
**Status:** PENDING APPROVAL

---

## Change 10: `Hearing` aggregate — `canAllocateForCrown()` now requires `hasAssignedSession`

**File:** `listing-domain/listing-domain-aggregate/src/main/java/.../Hearing.java`

**What changed:** `canAllocateForCrown()` now requires `Boolean.TRUE.equals(this.hasAssignedSession)` in addition to existing conditions (language, jurisdiction, courtRoom, dates).

**Journey:** Unallocated→Allocated (2), Update Allocated (3), Adjournment (4)

**Analysis:** This is a **critical change**. It means a CROWN hearing can only be allocated if the court schedule sessions are confirmed as assigned (not draft). This implements your rule that allocated hearings must have `draft=false` sessions.

**Potential issue:** If `hasAssignedSession` is never set (e.g., for hearings created before this feature), the hearing can never be allocated. This could break existing CROWN hearings. Ensure that:
1. Existing allocated CROWN hearings have their `hasAssignedSession` field set during replay/catchup
2. The `onHearingAllocatedForListingV2()` event handler sets `hasAssignedSession = true` (it does — see below)

**Verdict:** NEEDED — but verify backward compatibility with existing CROWN hearings
**Status:** PENDING APPROVAL

---

## Change 11: `Hearing` aggregate — `assignCourtRoom()` / `removeCourtRoom()` changes

**File:** `listing-domain/listing-domain-aggregate/src/main/java/.../Hearing.java`

**What changed:**
- `assignCourtRoom()`: For CROWN, sets `hasAssignedSession = true`. Now also calls `updateCourtRoomId()` immediately before generating events, and calls `checkAndTriggerAllocationStatus()` after courtroom events.
- `removeCourtRoom()`: For CROWN, sets `hasAssignedSession = false`. Same immediate state update and allocation check pattern.
- New `checkAndTriggerAllocationStatus()`: checks if hearing should be allocated/unallocated and triggers appropriate events.

**Journey:** Unallocated→Allocated (2), Update Allocated (3)

**Analysis:** This ensures that when a courtroom is assigned to a CROWN hearing (meaning sessions are assigned/not draft), the allocation status is re-evaluated. And when a courtroom is removed (meaning sessions became draft), the hearing can become unallocated.

**Potential issue — STATE MUTATION BEFORE EVENTS:** The code calls `updateCourtRoomId(courtRoomId)` before generating the `CourtRoomAssignedToHearing` event. This changes the aggregate's internal state before the event is applied. In event-sourced aggregates, state should typically change only in response to events. However, this appears to be necessary so that `checkAndTriggerAllocationStatus()` can see the updated courtRoomId. This is a pragmatic trade-off but **could cause issues during replay** if the event handler also updates courtRoomId (double update). Verify that the event handler for `CourtRoomAssignedToHearing` is idempotent with respect to courtRoomId.

**Verdict:** NEEDED — but verify event replay safety
**Status:** PENDING APPROVAL

---

## Change 12: `Hearing` aggregate — CROWN now triggers `availableSlotsForHearingFreed` on vacate and eject

**File:** `listing-domain/listing-domain-aggregate/src/main/java/.../Hearing.java`

**What changed:**
- `vacateTrial()`: Previously only MAGISTRATES fired `availableSlotsForHearingFreed`. Now CROWN also fires it when a vacating reason is provided.
- `deleteAllocatedHearing()`: Previously only MAGISTRATES fired `availableSlotsForHearingFreed`. Now CROWN also fires it.
- `ejectCase()` (via `listCasesForEviction`): Previously only MAGISTRATES fired `availableSlotsForHearingFreed`. Now CROWN also fires it.

**Journey:** Update Allocated (3) for vacate/delete, plus eject-case flows (not yet in journeys.md)

**Analysis:** This ensures that when a CROWN hearing is vacated, deleted, or a case is ejected, the court scheduler is notified to free the slots. Without this, court schedule sessions would remain booked even though the hearing is gone. This is **required** for proper court schedule lifecycle management.

**Verdict:** NEEDED
**Status:** PENDING APPROVAL

---

## Change 13: `Hearing` aggregate — `onHearingAllocatedForListingV2()` sets `hasAssignedSession`

**File:** `listing-domain/listing-domain-aggregate/src/main/java/.../Hearing.java`

**What changed:** When replaying a `HearingAllocatedForListingV2` event, if jurisdiction is CROWN and courtRoomId is present, sets `hasAssignedSession = true`. Also updates courtRoomId from the event.

**Journey:** All — this is during aggregate replay

**Analysis:** This is needed for backward compatibility. When replaying events for existing CROWN hearings that were allocated, this ensures `hasAssignedSession` is set correctly so that `canAllocateForCrown()` works.

**Verdict:** NEEDED
**Status:** PENDING APPROVAL

---

## Change 14: `ListingCommandHandler` — `changeJudiciaryForHearings()` CROWN-specific logic

**File:** `listing-command/listing-command-handler/src/main/java/.../ListingCommandHandler.java`

**What changed:**
- New `CrownHearingType` enum: `UNSCHEDULED`, `WEEK_COMMENCING`, `ALLOCATED`, `UNALLOCATED`
- New `classifyCrownHearingType()` method
- `changeJudiciaryForHearings()`: For CROWN UNSCHEDULED and WEEK_COMMENCING hearings, only applies judicial events (skips allocation rules). For ALLOCATED and UNALLOCATED, applies both judicial events and allocation rules as before.

**Journey:** Not in journeys.md, but `listing.command.change-judiciary-for-hearings` is a user action

**Analysis:** This prevents unnecessary allocation rule processing for CROWN hearings that can't be allocated (unscheduled, week-commencing). For allocated/unallocated CROWN hearings, allocation rules still run which may trigger court schedule updates.

**Verdict:** NEEDED
**Status:** PENDING APPROVAL

---

## Change 15: `HearingDaysEnrichmentService` — Null-safety for `enrichCandidate()`

**File:** `listing-command/listing-command-api/src/main/java/.../HearingDaysEnrichmentService.java`

**What changed:** Added null check for `startTime` in `enrichCandidate()`. If both `listedStartDateTime` and `earliestStartDateTime` are null, logs a warning and returns early instead of NPE.

**Journey:** Adhoc Hearing (1), Adjournment Path C (4)

**Analysis:** This is a **bug fix**. CROWN hearings may not have either start time set in some scenarios, and this would cause an NPE during hearing days enrichment.

**Verdict:** NEEDED (bug fix)
**Status:** PENDING APPROVAL

---

## Change 16: `HearingDaysEnrichmentService` — Removed dead code `enrichNonSittingDaysForCrown(HearingListingNeeds)`

**File:** `listing-command/listing-command-api/src/main/java/.../HearingDaysEnrichmentService.java`

**What changed:** Removed unused overload `enrichNonSittingDaysForCrown(HearingListingNeeds)` that always returned null.

**Verdict:** CLEANUP — harmless
**Status:** PENDING APPROVAL

---

## Change 17: `DefaultQueryApiHearingSlotsResource` — New query parameters

**File:** `listing-query/listing-query-api/src/main/java/.../DefaultQueryApiHearingSlotsResource.java`

**What changed:**
- Added `status`, `consecutiveDays`, `isWeekCommencing` query parameters
- If `isWeekCommencing=true`, returns empty result immediately (week-commencing excluded from slot search)
- Parameters forwarded to court scheduler service

**Journey:** Not directly in journeys — this is the hearing slots search query used by the listing UI

**Analysis:** These new parameters support CROWN-specific slot searching:
- `status`: filter by draft/assigned session status
- `consecutiveDays`: for multi-day trial slot searching
- `isWeekCommencing`: immediately returns empty if true (week-commencing hearings excluded from court schedule)

**Verdict:** NEEDED
**Status:** PENDING APPROVAL

---

## Change 18: `HearingQueryView` — `searchHearingsWithAnyAllocationState()` simplified

**File:** `listing-query/listing-query-view/src/main/java/.../HearingQueryView.java`

**What changed:** Removed `startDate` parameter. Now searches by caseUrn only without date filtering.

**Journey:** Not directly in journeys — this is a query used by the split/merge hearing UI

**Analysis:** This broadens the search to find all hearings regardless of start date. This could return more results than before. The corresponding `HearingRepository` query was also changed.

**Potential issue:** Removing the start date filter could return very old hearings. Verify this is intentional.

**Verdict:** QUESTIONABLE — may be unrelated to court schedule integration. Confirm if this is needed for the CCSP H2 feature.
**Status:** PENDING APPROVAL

---

## Change 19: `HearingRepository` — `findHearingsByCaseUrnAndAnyAllocationState()` query change

**File:** `listing-viewstore/listing-viewstore-persistence/src/main/java/.../HearingRepository.java`

**What changed:**
- Removed `startDate` parameter and its filter conditions
- Changed from subquery EXISTS pattern to LEFT JOIN pattern
- Added `distinct` to prevent duplicates from joins
- Changed case reference matching to use `UPPER(cast(...))` pattern

**Analysis:** Same as Change 18 — broadens the search. The LEFT JOIN + WHERE pattern is functionally equivalent but may have different performance characteristics.

**Potential issue:** Using `UPPER(cast(lc.case_reference as varchar))` is different from the previous exact match. The previous code used `toUpperCase()` in Java then matched exactly in SQL. The new code does UPPER in SQL. Functionally equivalent but verify performance.

**Verdict:** QUESTIONABLE — same concern as Change 18
**Status:** PENDING APPROVAL

---

## Change 20: `HearingSearchSyncService` — Removed `.toUpperCase()` on case/application references

**File:** `listing-event/listing-event-listener/src/main/java/.../HearingSearchSyncService.java`

**What changed:** No longer uppercases `caseReference` and `applicationReference` when indexing into the search/viewstore.

**Analysis:** This changes the data stored in the viewstore. Previously references were uppercased before storage. Now they're stored as-is. This aligns with Change 19 which does `UPPER()` in SQL at query time.

**Potential issue:** If there are existing records with uppercased references and new records without, queries might need to handle both. The SQL `UPPER()` in Change 19 handles this, but other queries that don't use `UPPER()` might break.

**Verdict:** QUESTIONABLE — appears to be a separate data quality change, not directly related to court schedule integration
**Status:** PENDING APPROVAL

---

## Change 21: `HearingDaysUpdateEventListener` — Simplified `correctNonDefaultDaysWithoutCourtCentre()`

**File:** `listing-event/listing-event-listener/src/main/java/.../HearingDaysUpdateEventListener.java`

**What changed:** Replaced `determineRoomId()` and `determineCourtCentreId()` helper methods with inline null checks. Removed null-safe handling that the helpers provided — the new code will NPE if `courtRoomId` is null (calls `courtRoomId.toString()` without null check).

**Journey:** Unallocated→Allocated (2), Update Allocated (3) — viewstore update path

**Analysis:** This is a **potential regression/bug**. The previous `determineRoomId()` method returned `nonNull(courtRoomId) ? courtRoomId.toString() : null` when the nonDefaultDay's roomId was null. The new code does `null == nonDefaultDay.getRoomId() ? courtRoomId.toString() : nonDefaultDay.getRoomId()` — if `courtRoomId` is null AND `nonDefaultDay.getRoomId()` is null, this will NPE on `courtRoomId.toString()`. For CROWN unallocated hearings where courtRoomId may be null, this could be a problem.

**Verdict:** BUG — potential NPE when courtRoomId is null for non-default days on unallocated CROWN hearings
**Status:** PENDING APPROVAL — recommend reverting to the null-safe version from hmiremoval

---

## Change 22: Liquibase — Deleted `027-add-hearing-ref-indexes.xml`

**File:** `listing-viewstore/listing-viewstore-liquibase/src/main/resources/liquibase/listing-view-store-db-changesets/027-add-hearing-ref-indexes.xml`

**What changed:** Removed indexes on `listed_cases.case_reference` and `court_applications.application_reference`.

**Analysis:** These indexes were added in hmiremoval. Removing them means:
- `findHearingsByCaseUrnAndAnyAllocationState()` (Change 19) may be slower without these indexes
- The UPPER() SQL function in Change 19 wouldn't use these indexes anyway (would need functional indexes)

**Verdict:** QUESTIONABLE — the indexes were useful for the original query. If Change 19 is kept, these indexes are less useful (UPPER() won't use them). But if Change 19 is reverted, they should be kept.
**Status:** PENDING APPROVAL

---

## Summary of Verdicts

| # | File/Area | Verdict | Notes |
|---|-----------|---------|-------|
| 1 | `needsCourtScheduleEnrichment()` CROWN gate | NEEDED | Core enabler |
| 2 | New CROWN enrichment methods | NEEDED | Main feature |
| 3 | Null-safety in `combineSearchAndBookResponseAndListResponse` | NEEDED | Bug fix |
| 4 | `HearingEnrichmentOrchestrator` CROWN flow | NEEDED | Orchestration |
| 5 | `ListingCommandApi` CROWN validation | NEEDED | Business rules |
| 6 | `handleChangeJudiciaryForHearings()` enrichment | NEEDED | Court scheduler sync |
| 7 | `HearingByIdProvider` (new) | NEEDED | Supports #6 |
| 8 | `ViewstoreHearingToHearingListingNeedsConverter` (new) | NEEDED | Supports #6 |
| 9 | `Hearing` aggregate — `ListingStatus` enum | NEEDED | Classification |
| 10 | `canAllocateForCrown()` requires `hasAssignedSession` | NEEDED | Core rule |
| 11 | `assignCourtRoom()`/`removeCourtRoom()` changes | NEEDED | Allocation lifecycle |
| 12 | CROWN `availableSlotsForHearingFreed` on vacate/eject | NEEDED | Slot lifecycle |
| 13 | `onHearingAllocatedForListingV2` replay fix | NEEDED | Backward compat |
| 14 | `changeJudiciaryForHearings` handler CROWN logic | NEEDED | Skip allocation for WC/unscheduled |
| 15 | Null-safety in `enrichCandidate()` | NEEDED | Bug fix |
| 16 | Dead code removal | CLEANUP | Harmless |
| 17 | Hearing slots query — new params | NEEDED | UI support |
| 18 | `searchHearingsWithAnyAllocationState` no startDate | QUESTIONABLE | May be unrelated |
| 19 | `HearingRepository` query rewrite | QUESTIONABLE | May be unrelated |
| 20 | Remove `.toUpperCase()` on references | QUESTIONABLE | Data quality change |
| 21 | `correctNonDefaultDaysWithoutCourtCentre` simplification | **BUG** | Potential NPE |
| 22 | Liquibase index removal | QUESTIONABLE | Tied to #19 |

## Recommendations

1. **Changes 18-20, 22** appear unrelated to CROWN court schedule integration. Consider whether they should be in this branch or handled separately. If they're fixing issues found during development, that's fine, but they should be tested independently.

2. **Change 21 is a bug** — the null-safe helpers from hmiremoval should be preserved. The simplified version will NPE for unallocated CROWN hearings where courtRoomId is null.

3. **Change 10-11** — verify that existing CROWN hearings in production have their `hasAssignedSession` field correctly populated during replay. If there are CROWN hearings that were allocated before this feature, they need Change 13 to set `hasAssignedSession = true` on replay.

4. **Change 4** — consider whether silent fallback to "assigned" when the court scheduler is unreachable is the right behavior. A loud failure might be safer.
