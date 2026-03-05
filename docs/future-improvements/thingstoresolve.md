# Things to Resolve in cpp-context-listing

## 1. Cyclic Calls

The following handlers emit events that route back to themselves, creating 2-node cycles.
These are likely intentional fan-out patterns (processing items one at a time), but should
be reviewed to confirm they have proper termination conditions.

### listing.command.mark-hearing-as-duplicate-for-case
- **Cycle:** `listing.command.mark-hearing-as-duplicate-for-case` (COMMAND_HANDLER) -> `listing.events.hearing-marked-as-duplicate` (EVENT_LISTENER) -> back to start
- **Risk:** If there's no guard, marking one hearing as duplicate could trigger an infinite loop across cases.

### listing.command.remove-offences-from-existing-hearing
- **Cycle:** `listing.command.remove-offences-from-existing-hearing` (COMMAND_HANDLER) -> `listing.events.remove-offences-from-existing-hearing-requested` (EVENT_PROCESSOR) -> back to start
- **Risk:** The handler fires a "requested" event that loops back to itself — likely iterating through offences individually.

### listing.command.list-unscheduled-next-hearing
- **Cycle:** `listing.command.list-unscheduled-next-hearing` (COMMAND_HANDLER) -> `listing.events.unscheduled-next-hearing-requested` (EVENT_PROCESSOR) -> back to start
- **Risk:** Processes one hearing, fires a "requested" event for the next — needs a clear stop condition.

---

## 2. Command API Naming — Needs Renaming

The following COMMAND_API handlers are incorrectly named `listing.command.X` instead of
`listing.X`. This means the COMMAND_API and COMMAND_HANDLER share the exact same action
name, which breaks the naming convention and makes flow tracing ambiguous.

The convention is: COMMAND_API handles `listing.X` and forwards to `listing.command.X`
on the COMMAND_HANDLER. These 7 all skip that and go straight to `listing.command.X` on
both sides.

| # | Current action name (used by both API & Handler) | COMMAND_API class.method | COMMAND_HANDLER class.method | Correct API name |
|---|--------------------------------------------------|--------------------------|------------------------------|------------------|
| 1 | `listing.command.restrict-court-list` | `ListingCommandApi.handleRestrictCourtList()` | `ListingCommandHandler.restrictFromCourtList()` | `listing.restrict-court-list` |
| 2 | `listing.command.sequence-hearings` | `ListingCommandApi.handleSequenceHearings()` | `ListingCommandHandler.sequenceHearings()` | `listing.sequence-hearings` |
| 3 | `listing.command.change-judiciary-for-hearings` | `ListingCommandApi.handleChangeJudiciaryForHearings()` | `ListingCommandHandler.changeJudiciaryForHearings()` | `listing.change-judiciary-for-hearings` |
| 4 | `listing.command.court-list-request-export` | `ListingCommandApi.handleCourtListRequestExport()` | `ListingCommandHandler.courtListRequestExport()` | `listing.court-list-request-export` |
| 5 | `listing.command.create-listing-note` | `ListingCommandApi.handleCreateNote()` | `ListingNoteCommandHandler.handleCreateNote()` | `listing.create-listing-note` |
| 6 | `listing.command.delete-hearing` | `ListingCommandApi.handleDeleteHearing()` | `ListingCommandHandler.deleteHearing()` | `listing.delete-hearing` |
| 7 | `listing.command.publish-court-list` | `ListingCommandApi.handlePublishCourtList()` | `ListingCommandHandler.publishCourtList()` | `listing.publish-court-list` |
