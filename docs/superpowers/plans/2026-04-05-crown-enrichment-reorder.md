# CROWN Enrichment Pipeline Reorder

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorder the CROWN enrichment pipeline so CourtSchedule enrichment runs FIRST, then HearingDays, then Duration — because the court scheduler is the source of truth for session dates, times, rooms, and judiciary.

**Architecture:** The current pipeline is `HearingDays → Duration → CourtSchedule` for all jurisdictions. For CROWN only, we reverse this to `CourtSchedule → HearingDays → Duration`. The CourtSchedule step determines the case (no courtScheduleId → skip, multi-day → multiDaySearchAndBook, single-day → listHearingInCourtSessions) and returns enriched hearing data. HearingDays and Duration enrichment then derive from those results. MAGISTRATES remains unchanged.

**Tech Stack:** Java 17, CDI (`@Inject`), JAX-RS `Response`, existing `HearingSlotsService`, `CourtScheduleEnrichmentService`, `HearingDaysEnrichmentService`, `HearingDurationEnrichmentService`.

---

## Background: Current vs New Flow

### Current CROWN pipeline (all 3 entry points):
```
Input → HearingDaysEnrichment → DurationEnrichment → CourtScheduleEnrichment → Output
```

### New CROWN pipeline:
```
Input → CourtScheduleEnrichment (first!) → HearingDaysEnrichment → DurationEnrichment → Output
```

### Three Cases (inside CourtScheduleEnrichment for CROWN):

| Case | Condition | Action | Endpoint |
|------|-----------|--------|----------|
| 1 | No courtScheduleId in hearingDays/nonDefaultDays/bookedSlots | No court schedule enrichment; check weekCommencing, pass through | N/A |
| 2 | Has courtScheduleId + aggregated duration > 360 | Multi-day: call multiDaySearchAndBook with first courtScheduleId + total duration | `vnd.courtscheduler.multiday.searchandbook.hearing.slots+json` |
| 3 | Has courtScheduleId + aggregated duration ≤ 360 | Single-day: call listHearingInCourtSessions with courtScheduleId(s) | `vnd.courtscheduler.list.hearings-in-court-sessions+json` |

### Duration calculation for the >360 / ≤360 decision:
1. If hearingDays exist → sum `durationMinutes` across all hearingDays
2. Else if nonDefaultDays exist → sum `duration` across all nonDefaultDays
3. Else → fall back to `estimatedMinutes` from the hearing

### Three entry points that need the reorder:
1. `HearingEnrichmentOrchestrator.enrichListCourtHearing(List<HearingListingNeeds>, JsonEnvelope)` — called by `listing.command.list-court-hearing` and `listing.list-next-hearings-v2`
2. `HearingEnrichmentOrchestrator.enrichUpdateHearingForListing(UpdateHearingForListing, JsonEnvelope)` — called by `listing.command.update-related-hearing`
3. `HearingEnrichmentOrchestrator.enrichUpdateHearingForListing(UpdateHearingForListing, JsonEnvelope, CourtCentreDetails)` — called by `listing.command.update-hearing-for-listing` and `listing.command.update-hearings-for-listing`

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/HearingEnrichmentOrchestrator.java` | Modify | Reorder CROWN pipeline: CourtSchedule → HearingDays → Duration in all 3 methods |
| `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentService.java` | Modify | Add new public method `enrichCrownCourtScheduleFirst(HearingListingNeeds)` that determines the case (1/2/3) and calls the right endpoint; reuse existing `multiDaySearchAndBook`, `listHearingSessionsAndExtractData`, `fetchCourtSchedulesByIds`, `sanityCheckAndEnrichCrown` |
| `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/HearingEnrichmentOrchestratorTest.java` | Modify | Update CROWN test cases to verify new call order |
| `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentServiceTest.java` | Modify | Add tests for `enrichCrownCourtScheduleFirst` covering all 3 cases |

---

## Task 1: Add duration-aggregation utility method to CourtScheduleEnrichmentService

This method determines the total duration from the hearing for the >360 / ≤360 decision. It checks hearingDays first, then nonDefaultDays, then falls back to estimatedMinutes.

**Files:**
- Modify: `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentService.java`
- Test: `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentServiceTest.java`

- [ ] **Step 1: Write the failing tests for `calculateAggregatedDuration`**

Add these tests to `CourtScheduleEnrichmentServiceTest.java`:

```java
// ─── calculateAggregatedDuration ───────────────────────────────────

@Test
void shouldCalculateDurationFromHearingDays() {
    HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
            .withHearingDays(List.of(
                    HearingDay.hearingDay().withDurationMinutes(360).build(),
                    HearingDay.hearingDay().withDurationMinutes(360).build()
            ))
            .withEstimatedMinutes(100)
            .build();

    int result = CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing);

    assertThat(result, is(720));
}

@Test
void shouldCalculateDurationFromNonDefaultDaysWhenNoHearingDays() {
    HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
            .withNonDefaultDays(List.of(
                    NonDefaultDay.nonDefaultDay().withDuration(180).build(),
                    NonDefaultDay.nonDefaultDay().withDuration(180).build()
            ))
            .withEstimatedMinutes(100)
            .build();

    int result = CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing);

    assertThat(result, is(360));
}

@Test
void shouldFallbackToEstimatedMinutesWhenNoHearingDaysOrNonDefaultDays() {
    HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
            .withEstimatedMinutes(240)
            .build();

    int result = CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing);

    assertThat(result, is(240));
}

@Test
void shouldReturnZeroWhenNoDurationInfoAvailable() {
    HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds().build();

    int result = CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing);

    assertThat(result, is(0));
}
```

Also add the same tests for `UpdateHearingForListing`:

```java
@Test
void shouldCalculateAggregatedDurationForUpdateFromHearingDays() {
    UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
            .withHearingDays(List.of(
                    HearingDay.hearingDay().withDurationMinutes(360).build(),
                    HearingDay.hearingDay().withDurationMinutes(360).build()
            ))
            .build();

    int result = CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing);

    assertThat(result, is(720));
}

@Test
void shouldCalculateAggregatedDurationForUpdateFromNonDefaultDays() {
    UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
            .withNonDefaultDays(List.of(
                    uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay().withDuration(200).build()
            ))
            .build();

    int result = CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing);

    assertThat(result, is(200));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=CourtScheduleEnrichmentServiceTest#shouldCalculateDurationFromHearingDays+shouldCalculateDurationFromNonDefaultDaysWhenNoHearingDays+shouldFallbackToEstimatedMinutesWhenNoHearingDaysOrNonDefaultDays+shouldReturnZeroWhenNoDurationInfoAvailable+shouldCalculateAggregatedDurationForUpdateFromHearingDays+shouldCalculateAggregatedDurationForUpdateFromNonDefaultDays -am`
Expected: FAIL — method does not exist yet

- [ ] **Step 3: Implement `calculateAggregatedDuration` in `CourtScheduleEnrichmentService.java`**

Add two static overloads — one for `HearingListingNeeds`, one for `UpdateHearingForListing`:

```java
/**
 * Calculates the total duration for the CROWN multi-day vs single-day decision.
 * Priority: hearingDays durationMinutes → nonDefaultDays duration → estimatedMinutes → 0
 */
static int calculateAggregatedDuration(final HearingListingNeeds hearing) {
    if (isNotEmpty(hearing.getHearingDays())) {
        return hearing.getHearingDays().stream()
                .mapToInt(d -> d.getDurationMinutes() != null ? d.getDurationMinutes() : 0)
                .sum();
    }
    if (isNotEmpty(hearing.getNonDefaultDays())) {
        return hearing.getNonDefaultDays().stream()
                .mapToInt(d -> d.getDuration() != null ? d.getDuration() : 0)
                .sum();
    }
    return hearing.getEstimatedMinutes() != null ? hearing.getEstimatedMinutes() : 0;
}

static int calculateAggregatedDuration(final UpdateHearingForListing hearing) {
    if (isNotEmpty(hearing.getHearingDays())) {
        return hearing.getHearingDays().stream()
                .mapToInt(d -> d.getDurationMinutes() != null ? d.getDurationMinutes() : 0)
                .sum();
    }
    if (isNotEmpty(hearing.getNonDefaultDays())) {
        return hearing.getNonDefaultDays().stream()
                .mapToInt(d -> d.getDuration() != null ? d.getDuration() : 0)
                .sum();
    }
    return 0;
}
```

Note: `NonDefaultDay` in `HearingListingNeeds` uses `uk.gov.justice.core.courts.NonDefaultDay` which has `getDuration()`. `NonDefaultDay` in `UpdateHearingForListing` uses `uk.gov.justice.listing.commands.NonDefaultDay` which also has `getDuration()`. Verify the correct types when implementing.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=CourtScheduleEnrichmentServiceTest#shouldCalculateDurationFromHearingDays+shouldCalculateDurationFromNonDefaultDaysWhenNoHearingDays+shouldFallbackToEstimatedMinutesWhenNoHearingDaysOrNonDefaultDays+shouldReturnZeroWhenNoDurationInfoAvailable+shouldCalculateAggregatedDurationForUpdateFromHearingDays+shouldCalculateAggregatedDurationForUpdateFromNonDefaultDays -am`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentService.java
git add listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentServiceTest.java
git commit -m "Add calculateAggregatedDuration for CROWN multi-day decision"
```

---

## Task 2: Add `enrichCrownCourtScheduleFirst` method for `HearingListingNeeds`

This is the new public entry point for CROWN court-schedule enrichment when called first in the pipeline. It determines which case applies and calls the appropriate endpoint.

**Files:**
- Modify: `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentService.java`
- Test: `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentServiceTest.java`

- [ ] **Step 1: Write the failing tests for Case 1 (no courtScheduleId)**

```java
// ─── enrichCrownCourtScheduleFirst (HearingListingNeeds) ───────────

@Test
void enrichCrownCourtScheduleFirst_shouldReturnUnchanged_whenNoCourtScheduleIdAnywhere() {
    // Case 1: No courtScheduleId in hearingDays, nonDefaultDays, or bookedSlots
    HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
            .withId(HEARING_ID)
            .withJurisdictionType(JurisdictionType.CROWN)
            .withHearingDays(List.of(
                    HearingDay.hearingDay()
                            .withHearingDate(LocalDate.of(2026, 4, 10))
                            .withDurationMinutes(120)
                            .build()
            ))
            .build();

    HearingListingNeeds result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

    assertThat(result, is(hearing));
    verifyNoInteractions(hearingSlotsService);
}
```

- [ ] **Step 2: Write the failing test for Case 3 (single-day, duration ≤ 360)**

```java
@Test
void enrichCrownCourtScheduleFirst_shouldCallListHearingInCourtSessions_whenSingleDay() {
    // Case 3: Has courtScheduleId + total duration ≤ 360
    UUID courtScheduleId = UUID.randomUUID();
    HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
            .withId(HEARING_ID)
            .withJurisdictionType(JurisdictionType.CROWN)
            .withEstimatedMinutes(120)
            .withCourtCentre(CourtCentre.courtCentre().withId(COURT_CENTRE_ID).withRoomId(COURT_ROOM_ID).build())
            .withHearingDays(List.of(
                    HearingDay.hearingDay()
                            .withHearingDate(LocalDate.of(2026, 4, 10))
                            .withCourtScheduleId(courtScheduleId)
                            .withCourtCentreId(COURT_CENTRE_ID)
                            .withCourtRoomId(COURT_ROOM_ID)
                            .withDurationMinutes(120)
                            .build()
            ))
            .build();

    // Mock fetchCourtSchedulesByIds
    CourtSchedule session = new CourtSchedule();
    session.setCourtScheduleId(courtScheduleId.toString());
    session.setSessionDate(LocalDate.of(2026, 4, 10));
    session.setCourtRoomId(COURT_ROOM_ID.toString());
    session.setCourtHouseId(COURT_CENTRE_ID.toString());
    session.setHearingStartTime("2026-04-10T10:00:00Z");
    session.setDraft(false);

    // Mock getCourtSchedulesById — returns sessions
    Response fetchResponse = mock(Response.class);
    // ... set up mock chain for fetchCourtSchedulesByIds returning `session`
    // Mock listHearingInCourtSessions — returns enriched data
    Response listResponse = mock(Response.class);
    // ... set up mock chain for listHearingInCourtSessions

    when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
    // ... verify result has enriched hearing days
}
```

Note: The actual mock setup will follow the existing patterns in `CourtScheduleEnrichmentServiceTest.java` — look at `shouldEnrichCrownSingleDay_whenCourtScheduleIdPresent` and similar tests for reference.

- [ ] **Step 3: Write the failing test for Case 2 (multi-day, duration > 360)**

```java
@Test
void enrichCrownCourtScheduleFirst_shouldCallMultiDaySearchAndBook_whenMultiDay() {
    // Case 2: Has courtScheduleId + total duration > 360
    UUID courtScheduleId = UUID.randomUUID();
    HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
            .withId(HEARING_ID)
            .withJurisdictionType(JurisdictionType.CROWN)
            .withEstimatedMinutes(720)
            .withCourtCentre(CourtCentre.courtCentre().withId(COURT_CENTRE_ID).withRoomId(COURT_ROOM_ID).build())
            .withHearingDays(List.of(
                    HearingDay.hearingDay()
                            .withHearingDate(LocalDate.of(2026, 4, 10))
                            .withCourtScheduleId(courtScheduleId)
                            .withCourtCentreId(COURT_CENTRE_ID)
                            .withCourtRoomId(COURT_ROOM_ID)
                            .withDurationMinutes(720)
                            .build()
            ))
            .build();

    // Mock multiDaySearchAndBook — returns 2 sessions
    // ... follow existing pattern from shouldEnrichCrownMultiDay tests
    when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);

    HearingListingNeeds result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

    verify(hearingSlotsService).multiDaySearchAndBook(anyMap());
    assertThat(result.getHearingDays().size(), is(2));
}
```

- [ ] **Step 4: Run tests to verify they fail**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=CourtScheduleEnrichmentServiceTest#enrichCrownCourtScheduleFirst_shouldReturnUnchanged_whenNoCourtScheduleIdAnywhere+enrichCrownCourtScheduleFirst_shouldCallListHearingInCourtSessions_whenSingleDay+enrichCrownCourtScheduleFirst_shouldCallMultiDaySearchAndBook_whenMultiDay -am`
Expected: FAIL — method does not exist

- [ ] **Step 5: Implement `enrichCrownCourtScheduleFirst` for `HearingListingNeeds`**

Add to `CourtScheduleEnrichmentService.java`:

```java
/**
 * CROWN-first enrichment: determines case and calls appropriate court scheduler endpoint.
 * Called BEFORE HearingDays and Duration enrichment for CROWN hearings.
 *
 * Case 1: No courtScheduleId anywhere → return unchanged (weekCommencing or no schedule data)
 * Case 2: Has courtScheduleId + aggregated duration > 360 → multiDaySearchAndBook
 * Case 3: Has courtScheduleId + aggregated duration ≤ 360 → listHearingInCourtSessions
 */
public HearingListingNeeds enrichCrownCourtScheduleFirst(final HearingListingNeeds hearing) {
    LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Starting for hearingId: {}", hearing.getId());

    final boolean hasCourtScheduleId = hasAnyCourtScheduleId(hearing);

    if (!hasCourtScheduleId) {
        LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Case 1: No courtScheduleId found. Returning unchanged for hearingId: {}", hearing.getId());
        return hearing;
    }

    final int aggregatedDuration = calculateAggregatedDuration(hearing);
    final boolean isMultiDay = aggregatedDuration > HearingDurationEnrichmentService.MINUTES_IN_DAY;

    LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] hearingId: {}, aggregatedDuration={}, isMultiDay={} (threshold={})",
            hearing.getId(), aggregatedDuration, isMultiDay, HearingDurationEnrichmentService.MINUTES_IN_DAY);

    EnrichmentResult enrichmentResult;
    if (isMultiDay) {
        LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Case 2: Multi-day → multiDaySearchAndBook for hearingId: {}", hearing.getId());
        enrichmentResult = handleCrownMultiDayEnrichment(hearing);
    } else {
        LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Case 3: Single-day → listHearingInCourtSessions for hearingId: {}", hearing.getId());
        enrichmentResult = handleCrownSingleDayEnrichment(hearing);
    }

    final List<HearingDay> enrichedHearingDays = enrichmentResult.getHearingDays();
    final List<JudicialRole> enrichedJudiciaries = enrichmentResult.getJudiciaries();

    LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Result: enrichedHearingDays={}, judiciaries={} for hearingId: {}",
            enrichedHearingDays.size(), enrichedJudiciaries.size(), hearing.getId());

    HearingListingNeeds.Builder hearingBuilder = HearingListingNeeds.hearingListingNeeds()
            .withValuesFrom(hearing)
            .withHearingDays(enrichedHearingDays);

    if (isNotEmpty(enrichedJudiciaries)) {
        hearingBuilder.withJudiciary(convertJudicialRoleDomainToCore(enrichedJudiciaries));
    }

    // Adjust court centre if scheduler returned a different room
    if (isNotEmpty(enrichedHearingDays) && nonNull(hearing.getCourtCentre())) {
        final CourtCentre adjustedCourtCentre = CourtCentre.courtCentre()
                .withValuesFrom(hearing.getCourtCentre())
                .withRoomId(enrichedHearingDays.get(0).getCourtRoomId())
                .build();
        hearingBuilder.withCourtCentre(adjustedCourtCentre);
    }

    return hearingBuilder.build();
}

/**
 * Checks if any structure (hearingDays, nonDefaultDays, bookedSlots) contains a courtScheduleId.
 */
private static boolean hasAnyCourtScheduleId(final HearingListingNeeds hearing) {
    if (isNotEmpty(hearing.getHearingDays())
            && hearing.getHearingDays().stream().anyMatch(d -> nonNull(d.getCourtScheduleId()))) {
        return true;
    }
    // bookedSlots have courtScheduleId as String
    if (isNotEmpty(hearing.getBookedSlots())
            && hearing.getBookedSlots().stream().anyMatch(s -> !isBlank(s.getCourtScheduleId()))) {
        return true;
    }
    return false;
}
```

Note: This method reuses the existing `handleCrownMultiDayEnrichment` and `handleCrownSingleDayEnrichment` which already implement the multi-day and single-day logic. The key difference is the entry point and the duration calculation.

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=CourtScheduleEnrichmentServiceTest#enrichCrownCourtScheduleFirst_shouldReturnUnchanged_whenNoCourtScheduleIdAnywhere+enrichCrownCourtScheduleFirst_shouldCallListHearingInCourtSessions_whenSingleDay+enrichCrownCourtScheduleFirst_shouldCallMultiDaySearchAndBook_whenMultiDay -am`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentService.java
git add listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentServiceTest.java
git commit -m "Add enrichCrownCourtScheduleFirst for HearingListingNeeds"
```

---

## Task 3: Add `enrichCrownCourtScheduleFirst` method for `UpdateHearingForListing`

Same logic but for the update path. The existing `enrichCrownUpdateHearing` already handles the CROWN update flow — we need a parallel entry point that can be called FIRST.

**Files:**
- Modify: `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentService.java`
- Test: `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Write 3 tests mirroring Task 2 but using `UpdateHearingForListing`:

```java
@Test
void enrichCrownCourtScheduleFirst_update_shouldReturnUnchanged_whenNoCourtScheduleId() {
    UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
            .withHearingId(HEARING_ID)
            .withJurisdictionType(JurisdictionType.CROWN)
            .withStartDate(LocalDate.of(2026, 4, 10))
            .withEndDate(LocalDate.of(2026, 4, 10))
            .withHearingDays(List.of(
                    HearingDay.hearingDay()
                            .withHearingDate(LocalDate.of(2026, 4, 10))
                            .withDurationMinutes(120)
                            .build()
            ))
            .build();

    UpdateHearingForListing result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

    assertThat(result, is(hearing));
    verifyNoInteractions(hearingSlotsService);
}

@Test
void enrichCrownCourtScheduleFirst_update_shouldCallListHearingInCourtSessions_whenSingleDay() {
    // Case 3 for UpdateHearingForListing
    // ... mock setup similar to existing enrichCrownUpdateHearing tests
}

@Test
void enrichCrownCourtScheduleFirst_update_shouldCallMultiDaySearchAndBook_whenMultiDay() {
    // Case 2 for UpdateHearingForListing
    // ... mock setup similar to existing enrichCrownUpdateHearing multi-day tests
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=CourtScheduleEnrichmentServiceTest#enrichCrownCourtScheduleFirst_update_shouldReturnUnchanged_whenNoCourtScheduleId+enrichCrownCourtScheduleFirst_update_shouldCallListHearingInCourtSessions_whenSingleDay+enrichCrownCourtScheduleFirst_update_shouldCallMultiDaySearchAndBook_whenMultiDay -am`
Expected: FAIL

- [ ] **Step 3: Implement `enrichCrownCourtScheduleFirst` for `UpdateHearingForListing`**

```java
/**
 * CROWN-first enrichment for update path.
 * Same logic as the HearingListingNeeds overload but operates on UpdateHearingForListing.
 */
public UpdateHearingForListing enrichCrownCourtScheduleFirst(final UpdateHearingForListing hearing) {
    LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Update path starting for hearingId: {}", hearing.getHearingId());

    final boolean hasCourtScheduleId = hasAnyCourtScheduleIdOnUpdate(hearing);

    if (!hasCourtScheduleId) {
        LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Update Case 1: No courtScheduleId. Returning unchanged for hearingId: {}", hearing.getHearingId());
        return hearing;
    }

    final int aggregatedDuration = calculateAggregatedDuration(hearing);
    final boolean isMultiDay = aggregatedDuration > HearingDurationEnrichmentService.MINUTES_IN_DAY;

    LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Update hearingId: {}, aggregatedDuration={}, isMultiDay={}",
            hearing.getHearingId(), aggregatedDuration, isMultiDay);

    // Delegate to existing enrichCrownUpdateHearing which already handles the 3 cases
    // The existing method checks for courtScheduleId presence internally
    return enrichCrownUpdateHearing(hearing);
}

private static boolean hasAnyCourtScheduleIdOnUpdate(final UpdateHearingForListing hearing) {
    return isNotEmpty(hearing.getHearingDays())
            && hearing.getHearingDays().stream().anyMatch(d -> nonNull(d.getCourtScheduleId()));
}
```

Important: The existing `enrichCrownUpdateHearing` already implements the multi-day vs single-day decision and the no-courtScheduleId fallback. Review whether we can reuse it directly or need to extract the duration-based decision into it. The key difference is the duration calculation — `enrichCrownUpdateHearing` currently uses `hearing.getHearingDays().stream().mapToInt(...)` inline, which aligns with our `calculateAggregatedDuration`. Verify this during implementation.

- [ ] **Step 4: Run tests to verify they pass**

Run: same test command as Step 2
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentService.java
git add listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/CourtScheduleEnrichmentServiceTest.java
git commit -m "Add enrichCrownCourtScheduleFirst for UpdateHearingForListing"
```

---

## Task 4: Reorder CROWN pipeline in `enrichListCourtHearing`

Change the CROWN branch from `HearingDays → Duration → CourtSchedule` to `CourtSchedule → HearingDays → Duration`.

**Files:**
- Modify: `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/HearingEnrichmentOrchestrator.java`
- Test: `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/HearingEnrichmentOrchestratorTest.java`

- [ ] **Step 1: Write/update the failing test for new CROWN order in `enrichListCourtHearing`**

Update or add a test in `HearingEnrichmentOrchestratorTest.java` that verifies the call order. Use `InOrder` from Mockito:

```java
@Test
void enrichListCourtHearing_crown_shouldCallCourtScheduleFirst_thenHearingDays_thenDuration() {
    HearingListingNeeds crownHearing = HearingListingNeeds.hearingListingNeeds()
            .withId(HEARING_ID)
            .withJurisdictionType(JurisdictionType.CROWN)
            // ... set up with courtScheduleId on hearingDays
            .build();

    when(courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(any(HearingListingNeeds.class)))
            .thenReturn(crownHearing);
    when(hearingDaysEnrichmentService.enrichHearings(any(HearingListingNeeds.class), any()))
            .thenReturn(crownHearing);
    when(hearingDurationEnrichmentService.enrichWithDurations(any(HearingListingNeeds.class), any()))
            .thenReturn(crownHearing);

    hearingEnrichmentOrchestrator.enrichListCourtHearing(List.of(crownHearing), envelope);

    InOrder inOrder = inOrder(courtScheduleEnrichmentService, hearingDaysEnrichmentService, hearingDurationEnrichmentService);
    inOrder.verify(courtScheduleEnrichmentService).enrichCrownCourtScheduleFirst(any(HearingListingNeeds.class));
    inOrder.verify(hearingDaysEnrichmentService).enrichHearings(any(HearingListingNeeds.class), any());
    inOrder.verify(hearingDurationEnrichmentService).enrichWithDurations(any(HearingListingNeeds.class), any());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=HearingEnrichmentOrchestratorTest#enrichListCourtHearing_crown_shouldCallCourtScheduleFirst_thenHearingDays_thenDuration -am`
Expected: FAIL — still calling in old order

- [ ] **Step 3: Reorder the CROWN branch in `enrichListCourtHearing`**

In `HearingEnrichmentOrchestrator.java`, change the CROWN branch from:

```java
// OLD: HearingDays -> Duration -> CourtSchedule
HearingListingNeeds withHearingDays = hearingDaysEnrichmentService.enrichHearings(hearing, envelope);
HearingListingNeeds withDurations = hearingDurationEnrichmentService.enrichWithDurations(withHearingDays, envelope);
HearingListingNeeds withCourtSchedules = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDurations, envelope);
enrichedHearings.add(withCourtSchedules);
```

To:

```java
// NEW: CourtSchedule (first!) -> HearingDays -> Duration
LOGGER.info("[CROWN-ENRICH] Step 1: CourtSchedule enrichment STARTED for hearingId: {}", hearing.getId());
HearingListingNeeds withCourtSchedules = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);
LOGGER.info("[CROWN-ENRICH] Step 1: CourtSchedule enrichment COMPLETED for hearingId: {}", hearing.getId());

LOGGER.info("[CROWN-ENRICH] Step 2: HearingDays enrichment STARTED for hearingId: {}", hearing.getId());
HearingListingNeeds withHearingDays = hearingDaysEnrichmentService.enrichHearings(withCourtSchedules, envelope);
LOGGER.info("[CROWN-ENRICH] Step 2: HearingDays enrichment COMPLETED for hearingId: {}", hearing.getId());

LOGGER.info("[CROWN-ENRICH] Step 3: Duration enrichment STARTED for hearingId: {}", hearing.getId());
HearingListingNeeds withDurations = hearingDurationEnrichmentService.enrichWithDurations(withHearingDays, envelope);
LOGGER.info("[CROWN-ENRICH] Step 3: Duration enrichment COMPLETED for hearingId: {}", hearing.getId());

enrichedHearings.add(withDurations);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=HearingEnrichmentOrchestratorTest#enrichListCourtHearing_crown_shouldCallCourtScheduleFirst_thenHearingDays_thenDuration -am`
Expected: PASS

- [ ] **Step 5: Run ALL orchestrator tests to check for regressions**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=HearingEnrichmentOrchestratorTest -am`
Expected: PASS (existing MAGS tests should be unaffected)

- [ ] **Step 6: Commit**

```bash
git add listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/HearingEnrichmentOrchestrator.java
git add listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/HearingEnrichmentOrchestratorTest.java
git commit -m "Reorder CROWN enrichListCourtHearing: CourtSchedule first"
```

---

## Task 5: Reorder CROWN pipeline in both `enrichUpdateHearingForListing` overloads

Same reorder for the update path.

**Files:**
- Modify: `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/HearingEnrichmentOrchestrator.java`
- Test: `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/HearingEnrichmentOrchestratorTest.java`

- [ ] **Step 1: Write/update failing tests for new CROWN order in both `enrichUpdateHearingForListing` overloads**

Add two tests — one for the 2-param overload (without `CourtCentreDetails`), one for the 3-param overload:

```java
@Test
void enrichUpdateHearingForListing_crown_shouldCallCourtScheduleFirst() {
    // 2-param overload
    UpdateHearingForListing crownHearing = UpdateHearingForListing.updateHearingForListing()
            .withHearingId(HEARING_ID)
            .withJurisdictionType(JurisdictionType.CROWN)
            .withStartDate(LocalDate.of(2026, 4, 10))
            .withEndDate(LocalDate.of(2026, 4, 10))
            .build();

    when(courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(any(UpdateHearingForListing.class)))
            .thenReturn(crownHearing);
    when(hearingDaysEnrichmentService.enrichHearing(any(UpdateHearingForListing.class), any()))
            .thenReturn(crownHearing);
    when(hearingDurationEnrichmentService.enrichWithDurationForUpdate(any(UpdateHearingForListing.class), any()))
            .thenReturn(crownHearing);

    hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(crownHearing, envelope);

    InOrder inOrder = inOrder(courtScheduleEnrichmentService, hearingDaysEnrichmentService, hearingDurationEnrichmentService);
    inOrder.verify(courtScheduleEnrichmentService).enrichCrownCourtScheduleFirst(any(UpdateHearingForListing.class));
    inOrder.verify(hearingDaysEnrichmentService).enrichHearing(any(UpdateHearingForListing.class), any());
    inOrder.verify(hearingDurationEnrichmentService).enrichWithDurationForUpdate(any(UpdateHearingForListing.class), any());
}

@Test
void enrichUpdateHearingForListing_crown_withCourtCentreDetails_shouldCallCourtScheduleFirst() {
    // 3-param overload
    // Same test structure but calls the 3-param overload
    // ...
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=HearingEnrichmentOrchestratorTest#enrichUpdateHearingForListing_crown_shouldCallCourtScheduleFirst+enrichUpdateHearingForListing_crown_withCourtCentreDetails_shouldCallCourtScheduleFirst -am`
Expected: FAIL

- [ ] **Step 3: Reorder CROWN branch in `enrichUpdateHearingForListing(hearing, envelope)` (2-param)**

Change from:
```java
UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(hearing, envelope);
UpdateHearingForListing withDuration = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
enrichedHearing = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration, envelope);
```

To:
```java
// CROWN: CourtSchedule (first!) -> HearingDays -> Duration
UpdateHearingForListing withCourtSchedules = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);
UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(withCourtSchedules, envelope);
enrichedHearing = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
```

- [ ] **Step 4: Reorder CROWN branch in `enrichUpdateHearingForListing(hearing, envelope, courtCentreDetails)` (3-param)**

Same change:
```java
// CROWN: CourtSchedule (first!) -> HearingDays -> Duration
UpdateHearingForListing withCourtSchedules = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);
UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(withCourtSchedules, envelope, courtCentreDetails);
enrichedHearing = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=HearingEnrichmentOrchestratorTest -am`
Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/HearingEnrichmentOrchestrator.java
git add listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/HearingEnrichmentOrchestratorTest.java
git commit -m "Reorder CROWN enrichUpdateHearingForListing: CourtSchedule first"
```

---

## Task 6: Full build + integration test verification

**Files:** None changed — verification only.

- [ ] **Step 1: Run full module build**

Run: `mvn clean install -pl listing-command/listing-command-api -am`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all CourtScheduleEnrichmentService tests**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=CourtScheduleEnrichmentServiceTest -am`
Expected: ALL PASS

- [ ] **Step 3: Run all HearingEnrichmentOrchestrator tests**

Run: `mvn test -pl listing-command/listing-command-api -Dtest=HearingEnrichmentOrchestratorTest -am`
Expected: ALL PASS

- [ ] **Step 4: Run full project build**

Run: `mvn clean install -DskipTests && mvn test -pl listing-command/listing-command-api -am`
Expected: BUILD SUCCESS, ALL TESTS PASS

- [ ] **Step 5: Run integration tests**

Run: `./runIntegrationTests.sh`
Expected: ALL PASS

- [ ] **Step 6: Final commit if any test fixes were needed**

If any tests needed fixing during this task, commit those fixes.

---

## Notes for the Implementer

### What NOT to change
- **MAGISTRATES paths** — the original `HearingDays → Duration → CourtSchedule` order remains for MAGS in all methods.
- **The existing `enrichWithCourtSchedules(HearingListingNeeds, JsonEnvelope)`** method — keep it, as it's still used by MAGS. The CROWN path in `checkAndUpdateListingCourtScheduler` can remain for now; it won't be called by the orchestrator for CROWN anymore, but other code may reference it.
- **`checkAndUpdateListingCourtScheduler`** — keep the existing logic intact. The new `enrichCrownCourtScheduleFirst` methods are additive.

### Key reuse
- `handleCrownSingleDayEnrichment(HearingListingNeeds)` — already implements single-day logic with `fetchCourtSchedulesByIds` + `sanityCheckAndEnrichCrown` + `listHearingSessionsAndExtractData`
- `handleCrownMultiDayEnrichment(HearingListingNeeds)` — already implements multi-day logic with `multiDaySearchAndBook` + `buildHearingDaysFromMultiDaySessions` + `listHearingSessionsAndExtractData`
- `enrichCrownUpdateHearing(UpdateHearingForListing)` — already implements the update path with all 3 cases

### Duration threshold
- `HearingDurationEnrichmentService.MINUTES_IN_DAY = 360` (6 working hours) is the boundary between single-day and multi-day.

### Log prefix convention
- All CROWN enrichment logs use `[CROWN-ENRICH]` with sub-tags: `[CourtSchedule-First]`, `[HearingDays]`, `[Duration]`.
