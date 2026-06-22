# UI Changes Required for CROWN Court Schedule Enrichment

This document describes the UI changes needed to enable court schedule enrichment for CROWN fixed-date hearings.

---

## Overview

CROWN court schedule enrichment requires **`courtScheduleId`** to be included in `nonDefaultDays` when calling the listing backend. The schema already supports this field — the UI just needs to start passing it.

The `courtScheduleId` is the UUID of the court schedule session selected by the user in the scheduling calendar. It links the hearing day to a specific session in the listing-court-scheduler viewstore.

---

## Endpoint 1: `update-hearing-for-listing`

**Used when:** A listing officer allocates, re-allocates, or updates a CROWN hearing from the listing UI.

**Endpoint:** `POST /hearings/{hearingId}`
**Content-Type:** `application/vnd.listing.command.update-hearing-for-listing+json`

### What to change

Add `courtScheduleId` (string, UUID format) to each `nonDefaultDays` entry.

### Current payload (without courtScheduleId)

```json
{
    "courtCentreId": "89592405-c29b-3706-b1d3-b1dd3a08b227",
    "courtRoomId": "d0624ee3-9198-3c8b-94d6-42fb197ebe5e",
    "type": {
        "id": "06b0c2bf-3f98-46ed-ab7e-56efaf9ecced",
        "description": "Plea and Trial Preparation"
    },
    "startDate": "2026-03-16",
    "endDate": "2026-03-16",
    "nonSittingDays": [],
    "nonDefaultDays": [
        {
            "startTime": "2026-03-16T10:00:00.000Z",
            "courtCentreId": "89592405-c29b-3706-b1d3-b1dd3a08b227",
            "roomId": "d0624ee3-9198-3c8b-94d6-42fb197ebe5e",
            "duration": 180
        }
    ],
    "judiciary": [],
    "jurisdictionType": "CROWN",
    "hearingLanguage": "ENGLISH",
    "sendNotificationToParties": false,
    "hearingId": "16e9796e-8912-4081-a573-80f1d1f81c7e"
}
```

### Required payload (with courtScheduleId)

```json
{
    "courtCentreId": "89592405-c29b-3706-b1d3-b1dd3a08b227",
    "courtRoomId": "d0624ee3-9198-3c8b-94d6-42fb197ebe5e",
    "type": {
        "id": "06b0c2bf-3f98-46ed-ab7e-56efaf9ecced",
        "description": "Plea and Trial Preparation"
    },
    "startDate": "2026-03-16",
    "endDate": "2026-03-16",
    "nonSittingDays": [],
    "nonDefaultDays": [
        {
            "startTime": "2026-03-16T10:00:00.000Z",
            "courtCentreId": "89592405-c29b-3706-b1d3-b1dd3a08b227",
            "roomId": "d0624ee3-9198-3c8b-94d6-42fb197ebe5e",
            "duration": 180,
            "courtScheduleId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        }
    ],
    "judiciary": [],
    "jurisdictionType": "CROWN",
    "hearingLanguage": "ENGLISH",
    "sendNotificationToParties": false,
    "hearingId": "16e9796e-8912-4081-a573-80f1d1f81c7e"
}
```

### Multi-day example

For a multi-day CROWN hearing (e.g. 3-day trial, duration 1080 mins), only one `nonDefaultDay` entry is needed with the **first day's** `courtScheduleId`. The backend will use the `multidaysearchandbook` endpoint to find and book consecutive sessions automatically.

```json
{
    "courtCentreId": "1e6f8561-2dff-3161-b7b3-6dcd679e7c65",
    "courtRoomId": "a0f7a73e-e99d-3e93-b0f1-f86fcfdab315",
    "startDate": "2026-03-16",
    "endDate": "2026-03-18",
    "nonSittingDays": [],
    "nonDefaultDays": [
        {
            "startTime": "2026-03-16T10:00:00.000Z",
            "courtCentreId": "1e6f8561-2dff-3161-b7b3-6dcd679e7c65",
            "roomId": "a0f7a73e-e99d-3e93-b0f1-f86fcfdab315",
            "duration": 1080,
            "courtScheduleId": "f1e2d3c4-b5a6-7890-1234-567890abcdef"
        }
    ],
    "judiciary": [],
    "jurisdictionType": "CROWN",
    "hearingLanguage": "ENGLISH",
    "sendNotificationToParties": false,
    "hearingId": "2cdfa174-a970-401e-80d1-a72fbfae4c35"
}
```

---

## Endpoint 2: `list-court-hearing`

**Used when:** Progression context sends a hearing to listing (adhoc hearing creation, adjournment path C).

**Endpoint:** `POST /hearings`
**Content-Type:** `application/vnd.listing.command.list-court-hearing+json`

### What to change

The `nonDefaultDays` array inside each hearing object should include `courtScheduleId`. This field is set by progression when it has court schedule information (e.g. from the scheduling calendar).

### Current payload (without courtScheduleId)

```json
{
    "hearings": [
        {
            "courtCentre": {
                "id": "89592405-c29b-3706-b1d3-b1dd3a08b227",
                "name": "Blackfriars Crown Court",
                "roomId": "d0624ee3-9198-3c8b-94d6-42fb197ebe5e",
                "roomName": "Courtroom 01"
            },
            "estimatedMinutes": 180,
            "id": "16e9796e-8912-4081-a573-80f1d1f81c7e",
            "jurisdictionType": "CROWN",
            "nonDefaultDays": [
                {
                    "startTime": "2026-03-16T10:00:00.000Z",
                    "courtCentreId": "89592405-c29b-3706-b1d3-b1dd3a08b227",
                    "roomId": "d0624ee3-9198-3c8b-94d6-42fb197ebe5e",
                    "duration": 180
                }
            ],
            "prosecutionCases": [ ... ]
        }
    ]
}
```

### Required payload (with courtScheduleId)

```json
{
    "hearings": [
        {
            "courtCentre": {
                "id": "89592405-c29b-3706-b1d3-b1dd3a08b227",
                "name": "Blackfriars Crown Court",
                "roomId": "d0624ee3-9198-3c8b-94d6-42fb197ebe5e",
                "roomName": "Courtroom 01"
            },
            "estimatedMinutes": 180,
            "id": "16e9796e-8912-4081-a573-80f1d1f81c7e",
            "jurisdictionType": "CROWN",
            "nonDefaultDays": [
                {
                    "startTime": "2026-03-16T10:00:00.000Z",
                    "courtCentreId": "89592405-c29b-3706-b1d3-b1dd3a08b227",
                    "roomId": "d0624ee3-9198-3c8b-94d6-42fb197ebe5e",
                    "duration": 180,
                    "courtScheduleId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                }
            ],
            "prosecutionCases": [ ... ]
        }
    ]
}
```

---

## Where to get the `courtScheduleId`

The `courtScheduleId` is obtained from the court scheduler when the user selects a session in the scheduling calendar. The existing `courtscheduler.get.court_schedule` endpoint returns sessions that include their `courtScheduleId` (the `id` field in the court_schedule table).

When the user picks a session from the calendar:
1. The session's `id` (UUID) becomes the `courtScheduleId`
2. Pass it in the `nonDefaultDays` entry for that hearing day

---

## Scope — What does NOT need courtScheduleId

| Scenario | courtScheduleId needed? | Reason |
|---|---|---|
| CROWN fixed-date single-day | Yes | Enrichment resolves session, checks isDraft, books slot |
| CROWN fixed-date multi-day | Yes (first day only) | Backend expands to consecutive sessions via `multidaysearchandbook` |
| CROWN week-commencing | No | Out of scope — `weekCommencingDate != null` skips enrichment |
| CROWN unscheduled | No | Out of scope — no dates, "date and time to be fixed" |
| MAGISTRATES (all types) | No change | Existing MAGS flow already handles courtScheduleId |

---

## Backend behaviour when courtScheduleId is missing

If `courtScheduleId` is not included in `nonDefaultDays` for a CROWN fixed-date hearing:
- The hearing will still be processed normally (hearing days, duration enrichment)
- Court schedule enrichment will be skipped (no isDraft check, no slot booking)
- The hearing will remain **unallocated** (no courtScheduleIds on hearingDays → `canAllocateForCrown` returns false)

This is backward-compatible — existing payloads without `courtScheduleId` will continue to work as before.

---

## Journey reference

See `docs/development-guidelines/journeys.md` for the full end-to-end flows:
- **Journey 1** (Adhoc Hearing) → Step 4: `list-court-hearing` enrichment
- **Journey 2** (Unallocated → Allocated) → Step 1: `update-hearing-for-listing` enrichment
- **Journey 3** (Update Allocated) → Step 1: same endpoint as Journey 2
- **Journey 4** (Adjournment Path C) → Step C2: same as Journey 1 Step 4
