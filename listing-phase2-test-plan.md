# Crown Court Scheduler Phase-2 -- Listing Context Test Plan

**Branch:** `team/ccsph2n` vs `origin/main`
**Repo:** `cpp-context-listing`
**Confluence:** [Crown Court Scheduler Phase-2](https://tools.hmcts.net/confluence/spaces/CPPTI/pages/1933989390/Crown+Court+Scheduler+Phase-2)
**Date Generated:** 2026-03-25

---

## Table of Contents

1. [New & Modified Endpoints](#1-new--modified-endpoints)
2. [Prerequisite SQL Insert Statements](#2-prerequisite-sql-insert-statements)
3. [Curl Commands for Each Endpoint](#3-curl-commands-for-each-endpoint)
4. [Test Scenarios](#4-test-scenarios)
5. [Unit Test Coverage Summary](#5-unit-test-coverage-summary)
6. [Integration Test Coverage Summary](#6-integration-test-coverage-summary)

---

## 1. New & Modified Endpoints

### 1.1 NEW -- Validate Session Availability (Listing Wrapper)

| # | Method | Path | Action Name | Permission |
|---|--------|------|-------------|------------|
| 1 | POST | `/sessionAvailabilityValidation` | `listing.validate.session.availability` | Court Clerks, Court Administrators, Crown Court Admin, Listing Officers, Legal Advisers, Court Associate |

This endpoint wraps the courtscheduler's `/validate-session-availability` endpoint, forwarding the payload via `CourtSchedulerServiceAdapter.validateSessionAvailability()`.

**Request Schema:** `listing.validate.session.availability.json`

| Field | Type | Required |
|-------|------|----------|
| `courtScheduleIdList` | array of `{courtScheduleId: UUID}` | YES (minItems: 1) |
| `duration` | integer | NO |

**Response Schema (on failure):** `listing.validate.session.availability.response.json`

| Field | Type | Required |
|-------|------|----------|
| `validationResult.status` | string (SUCCESS/FAILURE) | YES |
| `validationResult.conflictingCourtScheduleId` | UUID | NO |
| `validationResult.validationError` | string | NO |

### 1.2 MODIFIED -- Hearing Slots Search

| # | Method | Path | Action Name | Changes |
|---|--------|------|-------------|---------|
| 2 | GET | `/hearingSlots` | `listing.search.hearing.slots` | New query params: `status` (DRAFT/FINAL/ALL, default ALL), `isWeekCommencing` (boolean) |

When `isWeekCommencing=true`, the listing endpoint returns an empty response directly without calling courtscheduler (results=0, pageCount=0, empty hearingSlots array, empty notes array).

### 1.3 MODIFIED -- Command-Side Services (Crown Court Enrichment)

| Service | Changes |
|---------|---------|
| `CourtScheduleEnrichmentService` | Crown Court enrichment: multi-day search & book via courtscheduler, court schedule lookup by IDs, Crown-specific session enrichment |
| `HearingEnrichmentOrchestrator` | Added Crown jurisdiction branch (same enrichment pipeline as Magistrates) for both `enrichListCourtHearing` and `enrichUpdateHearingForListing` |
| `HearingDaysEnrichmentService` | Crown hearing day enrichment support |

### 1.4 MODIFIED -- Common Services

| Service | Changes |
|---------|---------|
| `HearingSlotsService` | Added `multiDaySearchAndBook(params)` (GET to courtscheduler `/multidaysearchandbook/hearingslots`), `getCourtSchedulesById(ids)` |
| `CourtSchedulerServiceAdapter` | Added `validateSessionAvailability(JsonObject)` wrapper method |

### 1.5 Domain Changes

| Class | Changes |
|-------|---------|
| `CourtSchedule` (domain-common) | Added `isDraft` field with getter |
| `HearingDay` (domain-common) | Added `courtScheduleId` field with getter/setter |
| `Hearing` (aggregate) | Updated for Crown hearing enrichment logic |
| `HearingDay` (aggregate) | Added `courtScheduleId` support, Crown allocation logic |

---

## 2. Prerequisite SQL Insert Statements

The listing context calls the courtscheduler service via HTTP, so prerequisites need to be seeded in the **courtscheduler** database (`scsl`).

```sql
-- In SCSL database (courtscheduler):
INSERT INTO court_schedule (
    id, oucode, court_room_id, court_listing_profile_id, court_house_id,
    session_start, session_end, session_end_time, business_type, court_session,
    max_slot, max_duration, available_slot, available_duration,
    is_draft, is_slot_based, jurisdiction, panel, created_on, updated_on
) VALUES
('f8254db1-1683-483e-afb3-b87fde5a0a26', 'B01LY00', 'room-001', 'LP001', 'courthouse-001',
 '2026-04-06 10:00:00', '2026-04-06 16:00:00', '2026-04-06 16:00:00', 'LNG', 'AD',
 10, 360, 10, 360, false, false, 'CROWN', 'ADULT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('9e4932f7-97b2-3010-b942-ddd2624e4dd8', 'B01LY00', 'room-001', 'LP001', 'courthouse-001',
 '2026-04-07 10:00:00', '2026-04-07 16:00:00', '2026-04-07 16:00:00', 'LNG', 'AD',
 10, 360, 10, 360, false, false, 'CROWN', 'ADULT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

```sql
-- Cleanup
DELETE FROM court_schedule WHERE id IN (
    'f8254db1-1683-483e-afb3-b87fde5a0a26',
    '9e4932f7-97b2-3010-b942-ddd2624e4dd8'
);
```

---

## 3. Curl Commands for Each Endpoint

> **Base URL:** `http://localhost:8080/listing-query-api/query/api/rest/listing`
> **Required Header:** `CJSCPPUID: {user-id}`

### 3.1 Validate Session Availability (NEW)

```bash
# Happy path
curl -X POST \
  'http://localhost:8080/listing-query-api/query/api/rest/listing/sessionAvailabilityValidation' \
  -H 'Content-Type: application/vnd.listing.validate.session.availability+json' \
  -H 'CJSCPPUID: test-user-001' \
  -d '{
    "courtScheduleIdList": [
      {"courtScheduleId": "f8254db1-1683-483e-afb3-b87fde5a0a26"},
      {"courtScheduleId": "9e4932f7-97b2-3010-b942-ddd2624e4dd8"}
    ],
    "duration": 30
  }'

# Failure case - non-existent court schedule
curl -X POST \
  'http://localhost:8080/listing-query-api/query/api/rest/listing/sessionAvailabilityValidation' \
  -H 'Content-Type: application/vnd.listing.validate.session.availability+json' \
  -H 'CJSCPPUID: test-user-001' \
  -d '{
    "courtScheduleIdList": [
      {"courtScheduleId": "00000000-0000-0000-0000-000000000000"}
    ],
    "duration": 30
  }'
```

### 3.2 Hearing Slots Search (MODIFIED -- new params)

```bash
# With status=FINAL
curl -X GET \
  'http://localhost:8080/listing-query-api/query/api/rest/listing/hearingSlots?panel=ADULT&sessionStartDate=2026-04-06&sessionEndDate=2026-04-10&pageSize=20&pageNumber=1&status=FINAL' \
  -H 'Accept: application/vnd.listing.search.hearing.slots+json' \
  -H 'CJSCPPUID: test-user-001'

# isWeekCommencing=true (empty response, no courtscheduler call)
curl -X GET \
  'http://localhost:8080/listing-query-api/query/api/rest/listing/hearingSlots?panel=ADULT&sessionStartDate=2026-04-06&sessionEndDate=2026-04-10&pageSize=20&pageNumber=1&isWeekCommencing=true' \
  -H 'Accept: application/vnd.listing.search.hearing.slots+json' \
  -H 'CJSCPPUID: test-user-001'
```

---

## 4. Test Scenarios

### 4.1 Validate Session Availability (NEW)

| # | Scenario | Precondition | Expected | HTTP |
|---|----------|-------------- |----------|------|
| SA1 | Valid payload with courtScheduleIdList + duration | Court schedules exist in courtscheduler | 200 OK | 200 |
| SA2 | Courtscheduler returns error | Non-existent court schedule ID | 400 Bad Request passthrough | 400 |
| SA3 | Missing courtScheduleIdList | -- | 400 or schema validation error | 400 |

### 4.2 Hearing Slots Search -- New Parameters (MODIFIED)

| # | Scenario | Precondition | Expected | HTTP |
|---|----------|-------------- |----------|------|
| HS1 | Filter by status=DRAFT | Mix of draft and final | 200, only draft + notes | 200 |
| HS2 | Filter by status=FINAL | Mix | 200, only final + notes | 200 |
| HS3 | Filter by status=ALL (default) | Mix | 200, all sessions + notes | 200 |
| HS4 | isWeekCommencing=true | Any | 200, empty response (results=0, hearingSlots=[], notes=[]) | 200 |

### 4.3 Crown Court Enrichment (Command Side)

| # | Scenario | Precondition | Expected |
|---|----------|-------------- |----------|
| CR1 | List-court-hearing for Crown with courtScheduleIds | Hearing with courtScheduleId on each day | Enriched with court schedule info, judiciaries |
| CR2 | List-court-hearing for Crown without courtScheduleIds | Crown hearing without courtScheduleId | Search-and-book called, sessions assigned |
| CR3 | Update-hearing for Crown allocated | Assigned (non-draft) sessions | Court schedule info enriched |
| CR4 | Update-hearing for Crown unallocated | Draft sessions | Enriched, no courtroom info |
| CR5 | Multi-day Crown hearing enrichment | 3+ day hearing | Multi-day search-and-book called |
| CR6 | Week commencing Crown hearing | Week commencing dates | No enrichment, end date calculated |
| CR7 | Unsupported jurisdiction type | Neither MAGISTRATES nor CROWN | IllegalArgumentException |

---

## 5. Unit Test Coverage Summary

### 5.1 New Unit Test Files

| File | Tests | Coverage Focus |
|------|-------|----------------|
| `DefaultQueryApiSessionAvailabilityValidationResourceTest.java` | 2 | Resource delegates to adapter, forwards success and error responses |
| `CourtScheduleTest.java` | 7 | Domain: isDraft field, builder |
| `HearingDayTest.java` (domain-common) | 6 | Domain: courtScheduleId field |
| `HearingDayTest.java` (aggregate) | 8 | Aggregate: courtScheduleId, Crown allocation |

### 5.2 Modified Unit Test Files

| File | Tests | Changes |
|------|-------|---------|
| `CourtScheduleEnrichmentServiceTest.java` | 44 | Crown enrichment, multi-day, court schedule by ID, judiciary for Crown |
| `HearingEnrichmentOrchestratorTest.java` | 21 | Crown jurisdiction, non-sitting days, sequence recalc |
| `CourtSchedulerServiceAdapterTest.java` | 10 | validateSessionAvailability delegation |
| `HearingSlotsServiceTest.java` | 26 | validateSessionAvailability, multiDaySearchAndBook, getCourtSchedulesById |
| `DefaultQueryApiHearingSlotsResourceTest.java` | 7 | status, isWeekCommencing; empty response for isWeekCommencing=true |
| `HearingAggregateTest.java` | 124 | Crown allocation/unallocation, courtScheduleId |

---

## 6. Integration Test Coverage Summary

### 6.1 New Integration Test Files

| File | Tests | Coverage Focus |
|------|-------|----------------|
| `SessionAvailabilityValidationIT.java` | 2 | POST to `/sessionAvailabilityValidation` — success (stubbed 200) and failure (stubbed 400) |

### 6.2 Modified Integration Tests

| File | Changes |
|------|---------|
| `HearingIT.java` | Updated for Crown hearing flows |
| Various scenario test data | New Crown test data JSON files |

### 6.3 Test Utilities

| File | Purpose |
|------|---------|
| `CourtSchedulerServiceStub.java` | New: `stubValidateSessionAvailability()` (200), `stubValidateSessionAvailabilityFailure()` (400) |

---

## Key Architectural Notes

1. **Listing wraps Courtscheduler:** `HearingSlotsService` makes HTTP calls to courtscheduler REST API
2. **isWeekCommencing short-circuit:** Returns empty response at listing layer without calling courtscheduler
3. **Status defaults to ALL:** Listing defaults `status` to "ALL" before forwarding
4. **Crown enrichment pipeline:** Same as Magistrates: HearingDays → Duration → CourtSchedule
5. **Access control:** `listing.validate.session.availability` accessible to Court Clerks, Court Administrators, Crown Court Admin, Listing Officers, Legal Advisers, Court Associates
