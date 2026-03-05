# CROWN Court Schedule Enrichment — Design Decisions

Date: 2026-03-02
Status: Draft — Pending implementation

---

## 1. Why extend CourtScheduleEnrichmentService vs creating a new service?

**Decision:** Extend the existing `CourtScheduleEnrichmentService` with CROWN-specific methods.

**Rationale:**
- CROWN single-day enrichment closely parallels MAGS "direct listing" (Case 1: all hearingDays have courtScheduleId)
- Reuses existing infrastructure: `listHearingSessionsAndExtractData()`, `combineSearchAndBookResponseAndListResponse()`, `populateJudiciaryInfoFromSlots()`, `SlotsToJsonStringConverter`
- Keeps the enrichment pipeline pattern consistent: HearingDays → Duration → CourtSchedule
- A separate service would duplicate HTTP client wiring, JSON parsing, and `EnrichmentResult` handling

**Trade-off:** The class grows larger. Mitigated by clean method separation — CROWN methods are self-contained and don't share mutable state with MAGS methods.

---

## 2. How is isDraft used for allocation without adding it to the aggregate domain?

**Decision:** Use the existing indirect mechanism — when sessions are draft, skip `listHearingInCourtSessions`, so hearingDays lack courtScheduleIds → `canAllocateForCrown()` returns false.

**Rationale:**
- `isDraft` is an attribute of the court schedule session, not the hearing domain
- Adding `isDraft` to `HearingDay` in the aggregate would require event schema changes, Liquibase migrations, and would couple listing's domain to court-scheduler internals
- The MAGS pattern already works this way: if `searchAndBook` fails, hearingDays have no courtScheduleId → hearing stays unallocated
- For CROWN, the same pattern: if any session isDraft=true, we don't call `listHearingInCourtSessions` → no confirmed courtScheduleIds → `hasCourtScheduleIds` returns false → unallocated

**Exception case:** All sessions under one hearing MUST have the same isDraft value. If mixed (some draft, some not), the hearing is treated as unallocated. This is enforced in the enrichment service, not the aggregate.

---

## 3. Why a new `multidaysearchandbook` endpoint vs extending existing endpoints?

**Decision:** Create a new dedicated endpoint rather than extending `search.book.hearing.slots` or `get.hearing.slots`.

**Rationale:**
- `search.book.hearing.slots` handles single-session search-and-book with police/non-police business type matching. Multi-day CROWN requires finding consecutive sessions across multiple dates — fundamentally different logic
- `get.hearing.slots` is read-only (no booking). Multi-day CROWN needs atomic search + book
- A dedicated endpoint keeps the responsibility clear: "given a starting courtScheduleId and total duration, find and book N consecutive weekday sessions"
- The consecutive-weekday validation and multi-session atomic booking are complex enough to warrant isolation

**Alternative considered:** Calling `get.hearing.slots` N times with `consecutiveDays` filter. Rejected because: (1) it doesn't atomically book, (2) race conditions between search and book, (3) the caller would need to orchestrate N individual bookings.

---

## 4. How does courtScheduleId arrive in CROWN payloads?

**Decision:** Via `nonDefaultDays` with `courtScheduleId` field, using the existing payload structure.

**Rationale:**
- The `HearingListingNeeds` (list-court-hearing) already supports `nonDefaultDays` and the enrichment code (`enrichHearingDaysForCrown`) already calls `enrichByNonDefaultDaysIfPresent()` → `convertCoreNonDefaultDaysToHearingDays()` which preserves courtScheduleId
- The `UpdateHearingForListing` already has `nonDefaultDays` in CROWN payloads and `convertNonDefaultDaysToHearingDays()` already preserves courtScheduleId
- No schema changes needed — `courtScheduleId` is already an optional field on `nonDefaultDay` in the JSON schema
- For multi-day: a single nonDefaultDay with courtScheduleId + total duration (e.g. 1080 for 3 days). The `multidaysearchandbook` endpoint expands this into N sessions

**Dependency:** The calling context (progression) and UI must start including courtScheduleId on nonDefaultDays. This is an external dependency — no listing-side schema changes required.

---

## 5. Single-day vs multi-day determination

**Decision:** Use `estimatedMinutes > 360` as the threshold (360 = `HearingDurationEnrichmentService.MINUTES_IN_DAY`).

**Rationale:**
- Aligns with the existing constant already used for MAGS duration calculations
- For list-court-hearing: the payload carries a single nonDefaultDay. If its duration/estimatedMinutes > 360, it's multi-day
- For update-hearing-for-listing: the enriched hearing has total estimated minutes. If > 360, it's multi-day
- Matching the user's specification: "if duration <= 360 its single day otherwise its multiday"

---

## 6. Consecutive weekday enforcement in multidaysearchandbook

**Decision:** Strictly consecutive weekdays (Mon-Fri), no gaps. If any day cannot be found, return empty array.

**Rationale:**
- Crown Court trials span consecutive working days — a 3-day trial starting Monday must be Mon/Tue/Wed
- Weekends (Sat/Sun) are excluded from "consecutive" — a Friday trial continues on Monday
- If any weekday between the first and last date has no available session (or the session doesn't match criteria), the entire allocation fails
- An empty response tells listing "this hearing cannot be allocated as multi-day at this time" → hearing stays unallocated
- This prevents partial allocations where day 1 and day 3 are booked but day 2 is missing

**Search criteria for consecutive sessions:**
- Same `courtRoomId` as anchor session
- Same `rota_business_type` as anchor session
- `court_session = 'AD'` (all-day, as specified)
- `active = true`
- Session dates are consecutive weekdays starting from anchor's `session_start`

---

## 7. Availability check in multidaysearchandbook

**Decision:** Reuse the same dynamic calculation logic from `courtscheduler.get.hearing.slots`.

**Rationale:**
- The `get.hearing.slots` endpoint already computes availability dynamically via SQL: `max_duration_mins - SUM(al.duration)` with AD-split support
- The `overbookingFilter` in `SlotsSearchService` provides the Java-side validation
- Reusing this logic ensures consistency — a session that shows as "available" in the search UI will also be bookable via multidaysearchandbook
- The duration per day for availability check = `totalDuration / daysNeeded` (integer division)

---

## 8. Slot payback — extending both vacate-trial paths

**Decision:** Both vacate-trial paths extended for CROWN.

- **Path A (ListingCommandApi.handleVacateTrial):** Already calls `hearingSlotsService.delete(hearingId)` unconditionally — no change needed
- **Path B (Hearing.hearingVacateTrial):** Extended to emit `availableSlotsForHearingFreed` for CROWN when `hasCourtScheduleIds` is true

**Rationale:** Both paths exist and are used. Path A handles the synchronous API-level vacate, Path B handles the aggregate-level vacate via event. CROWN hearings may use either path depending on the calling context. The `hasCourtScheduleIds` guard ensures we only attempt to free slots when the hearing actually has booked sessions.

---

## 9. Hearing move detection

**Decision:** No explicit change detection needed in listing. The existing `listHearingInCourtSessions` mechanism handles it.

**Rationale:**
- When `listHearingInCourtSessions` is called with new courtScheduleIds, the court-scheduler's `updateListHearingSlots()` already calls `releaseOldListingsFromAllocatedListings(hearingId)` before creating new bookings
- For multi-day CROWN: first call `multidaysearchandbook` to get new courtScheduleIds, then `listHearingInCourtSessions` with those IDs (releases old sessions automatically)
- For single-day CROWN: `listHearingInCourtSessions` directly (releases old sessions)
- This mirrors the MAGS pattern — listing doesn't detect changes, it always re-books, and the court-scheduler handles the release

---

## 10. Sanity checks — logging vs failing

**Decision:** Use court-scheduler values when conflicts exist, log as errors.

**Rationale:**
- The court-scheduler is the source of truth for session dates, times, and courtroom assignments
- If the calling context sends startDate=2026-03-02 but the court scheduler says the session is on 2026-03-03, we use 2026-03-03
- Descriptive error logs help debugging without failing the entire request
- This is a defensive approach — the calling context may have stale data, but the hearing can still be processed
- Log format: `"CROWN sanity: received startDate {} but sessionDate {}. Using court scheduler value."`
