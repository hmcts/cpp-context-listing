# SPRDT-638: Link Crown Court Scheduler to Listing

> **Epic**: [SPRDT-638](https://tools.hmcts.net/jira/browse/SPRDT-638)
> **Status**: Analysis | **Priority**: Medium | **Assignee**: Joan Porter
> **Created**: 31 Oct 2025 | **Last Updated**: 18 Feb 2026

---

## Epic Overview

This feature links the Crown Court scheduler to Listing so that all hearings listed in the Crown Court can be **booked against specific sessions** in the schedule. This enables the slot counter to calculate the level of hearings booked and time remaining at session level.

### Current Problem

- Crown Court hearing details are entered with **no reference to a schedule**
- While CP can calculate the number/duration of allocated hearings at courtroom level by date, **unallocated hearings are not accounted for**
- There is **no way** for the system to monitor or control the volume of hearings listed using parameters set in the scheduler

### Phased Rollout

| Phase | Description |
|-------|-------------|
| **Phase 1** | Create/View/Edit functionality for Crown Court sessions. |
| **Phase 2** | Listing hearings against Crown Court sessions and migration of current Crown Court hearings. **Potential blocker**: removal of Allocated UI. |
| **Phase 3** | Assigning judiciary to Crown Court sessions and week commencing solution. |

### Affected Listing Paths

The following paths currently point to "Enter hearing details" and must be redirected to the new "Find a hearing" flow:

- All NHCC (Next Hearing in a Crown Court) results, including "List from Box" work result
- Applications
- Manual case create
- Create ad hoc hearing
- Allocation journeys from the Unallocated and Unscheduled lists
- Allocate and Reallocate pages in Court Calendar

### Design References

- **Figma**: https://www.figma.com/design/et3TkPsUQXUitpkmF3JK2I/CCT-1984--Common-Platform--Court-schedule?node-id=3136-31867
- **Attachment**: "New Find a hearing screens.pptm" (attached to epic)

---

## Stories

### Common "Find a Hearing" Filter Specification

All stories below share a common filter screen and search results pattern. The variations per journey are called out in each story. The common elements are:

#### Filter Fields

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| **Operational unit** | Dropdown (optional) | — | Narrows by unit |
| **Court** | Dropdown | Pre-populated from previous selection | Shows selected courthouse |
| **Courtroom** | Dropdown (optional) | Varies by journey (see stories) | Options: "No courtroom selected" / "All" / "Courtroom [n]" |
| **Booking type** | Radio buttons | **Duration based** | "Slot based" or "Duration based". Duration-based shows "Search for a multi day hearing? (Yes/No)" — if Yes, requires number of days input |
| **Business type** | Dropdown | — | Crown court hearing categories (e.g. PTPH, Sentence) |
| **Session type** | Radio buttons | **Any** | "Any", "AM", "PM", or "All day" |
| **Start date** | Date picker | Varies by journey (see stories) | |
| **End date** | Date picker (optional) | Pre-populated to match current date | |

#### Courtroom Filter → Session Visibility Rules

| Courtroom Selection | Sessions Displayed |
|--------------------|--------------------|
| "No courtroom selected" | Courtrooms with **draft** sessions |
| "All" | Courtrooms with **assigned** sessions |
| Specific courtroom | That courtroom's **assigned** sessions |

#### Validation Rules

- For single-day sessions, duration must **not exceed six hours**
- "Back" button must allow changing the initial scheduling preference selection

#### Search Result Columns

Each result entry displays:

| Column | Description |
|--------|-------------|
| **Selection** | Radio button to choose a specific date + court combination |
| **Date** | The calendar date for the slot |
| **Court Details** | Courthouse name; for Assigned sessions, also shows specific Courtroom (e.g. "Courtroom 1") |
| **Business** | Business type assigned to slot (e.g. PTPH, Sentence) |
| **Session** | Dropdown to select a specific time range (e.g. "10:00am to 11:00am") |
| **Time/slots remaining** | Numeric indicator of available capacity |
| **Booked** | List of currently scheduled times and booking status (e.g. "10:00am to 11:00am: 0") |

#### Common Result Actions

- Each result provides a link to **"Add listing note"**
- Option to select a **Hearing Type** (pre-populated with selected hearing type)
- Option to notify parties: **"Do you want to notify to all relevant parties?"** Yes/No (default: **No**)
- For **multi-day trials**: selecting the first session automatically selects consecutive sessions based on estimated duration; consecutive sessions must be of the **same type** as the first session
- On submit, **session availability is re-verified** before sharing results/notifying parties. If sessions are no longer available, user is notified and must rebook

---

### Story 1: SPRDT-620 — Creating a New Crown Court Case

> **[SPRDT-620](https://tools.hmcts.net/jira/browse/SPRDT-620)** | Status: Analysis | Assignee: Joan Porter

**As a** crown court officer,
**I want** to choose the hearing session for a new case,
**So that** the system can direct me to the correct allocation workflow.

#### Scenario 1: Choosing a Scheduling Option

- **Entry point**: Creating a new case → reach the **"Find hearing date"** screen
- **Options displayed** (radio buttons):
  - Date and time to be fixed
  - Fixed date
  - Week commencing
- Selecting **"Fixed date"** → navigates to the **"Find a hearing"** filter screen (currently "Court hearing details")
- Selecting **"Date and time to be fixed"** or **"Week commencing"** → bypasses the primary listing flow

#### Scenario 2: Fixed Date Filters (Journey-Specific Defaults)

- **Courtroom**: Default "No courtroom selected"
- **Start date**: Default to **current date**

#### Scenario 3: Search Results & Submission

- On successful selection and submission → directed to the **Defendant details** screen (for cases)
- Success message / green confirmation bar with concise listing details appears only at the **end of the full creation flow**, not immediately after session selection

#### Scenario 4: Week Commencing

- Directed to the current "Court hearing details" screen with existing Week Commencing functionality

#### UI Reference

- https://www.figma.com/design/et3TkPsUQXUitpkmF3JK2I/CCT-1984--Common-Platform--Court-schedule?node-id=3924-47025

---

### Story 2: SPRDT-619 — Creating a Hearing for a Crown Court Case or Linked Application

> **[SPRDT-619](https://tools.hmcts.net/jira/browse/SPRDT-619)** | Status: Analysis | Assignee: Joan Porter

**As a** crown court officer,
**I want** to choose the hearing session for a new hearing for a case or linked application,
**So that** the system can direct me to the correct allocation workflow.

#### Scenario 1: Choosing a Scheduling Option

- **Entry point**: Creating a new hearing → reach the **"Find hearing date"** screen
- **Options displayed** (radio buttons):
  - Fixed date
  - Week commencing
- Selecting **"Fixed date"** → navigates to **"List for Court Hearing"** screen, then the **"Find a hearing"** tab

#### Scenario 2: Fixed Date Filters (Journey-Specific Defaults)

- **Courtroom**: Default "No courtroom selected"
- **Start date**: Pre-populated with the **Hearing date** (if hearing has a current or future date, otherwise default to current date)

#### Scenario 3: Search Results & Submission

- On submission → directed to:
  - **"Case at a glance"** screen (for case creation), with confirmation message
  - OR **"Check application"** screen (for linked application creation)

#### Scenario 4: Week Commencing

- Directed to the current "List for court hearing" screen with existing Week Commencing functionality

#### UI Reference

- https://www.figma.com/design/et3TkPsUQXUitpkmF3JK2I/CCT-1984--Common-Platform--Court-schedule?node-id=3924-47025

---

### Story 3: SPRDT-618 — Box Work Hearing (Case / Application)

> **[SPRDT-618](https://tools.hmcts.net/jira/browse/SPRDT-618)** | Status: Analysis | Assignee: Joan Porter

**As a** crown court officer,
**I want** to choose the hearing session for a new Case or Application Boxwork Hearing,
**So that** the system can direct me to the correct allocation workflow.

#### Scenario 1: Choosing a Scheduling Option

- **Entry point**: Creating a court hearing for a pre-existing "hearing" in box work (**LHBW**) → select a Crown Court → directed to **"Find hearing date"** screen
- **Options displayed** (radio buttons):
  - Date and time to be fixed
  - Fixed date
  - Week commencing
- Selecting **"Fixed date"** → navigates to **"Find a hearing"** (currently "Search for available sessions") filter screen

#### Scenario 2: Fixed Date Filters (Journey-Specific Defaults)

- **Courtroom**: Default "No courtroom selected"
- **Start date**: Default to **current date**

#### Scenario 3: Search Results & Submission

- On submission → directed to the **"Enter results"** screen to continue Case or Application boxwork, with confirmation message

#### Scenario 4: Week Commencing

- Directed to the current "Court hearing details" screen with existing Week Commencing functionality

#### UI Reference

- https://www.figma.com/design/et3TkPsUQXUitpkmF3JK2I/CCT-1984--Common-Platform--Court-schedule?node-id=3924-47025

---

### Story 4: SPRDT-617 — Unscheduled Hearing

> **[SPRDT-617](https://tools.hmcts.net/jira/browse/SPRDT-617)** | Status: Analysis | Assignee: Joan Porter

**As a** crown court officer,
**I want** to choose the hearing session for an Unscheduled hearing,
**So that** the system can direct me to the correct allocation workflow.

#### Scenario 1: Entry & Navigation

- **Entry point**: Creating a hearing session for an Unscheduled hearing → select a Crown Court
- System navigates to **"Allocate a hearing"** screen
- Selecting the **"Find a hearing"** tab applies filter constraints

**Note**: No "Find hearing date" radio selection screen — goes directly to the Allocate a hearing / Find a hearing tab.

#### Scenario 2: Fixed Date Filters (Journey-Specific Defaults)

- **Courtroom**: Default "No courtroom selected" (with options: "No courtroom selected" / "All" / "Courtroom [n]")
- **Start date**: Default to **current date**

#### Scenario 3: Search Results & Submission

- On submission → directed to the **"Unscheduled list"** screen, with confirmation message

#### UI Reference

- https://www.figma.com/design/et3TkPsUQXUitpkmF3JK2I/CCT-1984--Common-Platform--Court-schedule?node-id=3924-47025

---

### Story 5: SPRDT-616 — Unallocated Hearing

> **[SPRDT-616](https://tools.hmcts.net/jira/browse/SPRDT-616)** | Status: Analysis | Assignee: Joan Porter

**As a** crown court officer,
**I want** to choose the hearing session for an Unallocated hearing,
**So that** the system can direct me to the correct allocation workflow.

#### Scenario 1: Entry & Navigation

- **Entry point**: Creating a hearing session for an Unallocated hearing → select a Crown Court
- System navigates to **"Allocate a hearing"** screen
- Selecting the **"Find a hearing"** tab applies filter constraints

**Note**: No "Find hearing date" radio selection screen — goes directly to the Allocate a hearing / Find a hearing tab.

#### Scenario 2: Fixed Date Filters (Journey-Specific Defaults)

- **Courtroom**: Default **"All"** (options: "All" / "Courtroom [n]" — **NO** "No courtroom selected" option)
- **Start date**: Pre-populated with the **Hearing date** (if hearing has a current or future date, otherwise default to current date)

#### Scenario 3: Search Results & Submission

- On submission → directed to the **"Unallocated list"** screen, with confirmation message

#### UI Reference

- https://www.figma.com/design/et3TkPsUQXUitpkmF3JK2I/CCT-1984--Common-Platform--Court-schedule?node-id=3924-47025

---

## Cross-Story Comparison Matrix

| Aspect | SPRDT-620 (New Case) | SPRDT-619 (New Hearing / App) | SPRDT-618 (Box Work) | SPRDT-617 (Unscheduled) | SPRDT-616 (Unallocated) |
|--------|---------------------|------------------------------|---------------------|------------------------|------------------------|
| **Radio options** | Fixed / Week / DTTBF | Fixed / Week | Fixed / Week / DTTBF | None (direct) | None (direct) |
| **Entry screen** | Find hearing date | Find hearing date | Find hearing date | Allocate a hearing | Allocate a hearing |
| **Courtroom default** | No courtroom selected | No courtroom selected | No courtroom selected | No courtroom selected | **All** |
| **"No courtroom" option** | Yes | Yes | Yes | Yes | **No** |
| **Start date default** | Current date | Hearing date (or current) | Current date | Current date | Hearing date (or current) |
| **Redirect after submit** | Defendant details | Case at a glance / Check application | Enter results | Unscheduled list | Unallocated list |
| **Confirmation timing** | End of full creation flow | On redirect | On redirect | On redirect | On redirect |

---

## Appendix

### Key Terminology

| Term | Meaning |
|------|---------|
| **NHCC** | Next Hearing in a Crown Court |
| **LHBW** | List Hearing from Box Work |
| **DTTBF** | Date and Time to Be Fixed |
| **Draft session** | A session not yet assigned to a specific courtroom |
| **Assigned session** | A session allocated to a specific courtroom |
| **Slot based** | Booking by number of slots |
| **Duration based** | Booking by time duration |
| **BDF** | Bulk Data File (for importing existing hearings) |

### Comments / Notes from Epic

- **Figma (Sarat KumarPenumarthy, 20 Jan 2026)**: https://www.figma.com/design/et3TkPsUQXUitpkmF3JK2I/CCT-1984--Common-Platform--Court-schedule?node-id=3136-31867
- **Attachment**: "New Find a hearing screens.pptm" — design changes for converting "Enter hearing details" to "Find a hearing" search
