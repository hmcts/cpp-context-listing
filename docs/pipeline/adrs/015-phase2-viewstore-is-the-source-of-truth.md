# ADR 015 — Phase 2: no aggregates or private events; the viewstore is the source of truth

- **Status:** Accepted (user decision, 2026-11-19). Supersedes ADR 011 and ADR 014 for the Spring
  Boot application. ADR 011 still describes the Phase 1 JEE build.
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §3, §4, §5, §10.1 P2.3–P2.4)

## Decision (the user's words, condensed)

The Spring Boot application keeps **no aggregates and no event log**:

1. **Commands** (the command API or its equivalent) check the business rules against viewstore
   state, **write the viewstore directly**, and write **public messages** to the outbox. The outbox
   POJOs are exactly the public messages the JEE event processor emits today (same names, same
   payloads, same envelope metadata conventions); the relay publishes them to `public.event`
   (ADR 013).
2. **Inbound public messages** that listing consumes update the viewstore directly, as the JEE
   processor → command → listener path does today.
3. **The viewstore is the source of truth.** State the aggregates kept that the viewstore does not
   (the §15.2 inventory: `duplicate`/`deleted`, offence and defendant membership, seeded-hearing
   state, Case/Application hearing links, `weekCommencing`, …) gets additive viewstore columns or
   tables, written by the command services.
4. **Cut-over and production state are out of scope:** no backfill from `event_log`, no snapshot
   migration, no Phase 1 snapshot compatibility.
5. **Parity definition:** for the same command (or inbound public message), the Spring application
   must leave the same data in the viewstore and emit the same public messages as JEE + WildFly
   today. The legacy IT suite running on the Spring stack (P2.3.0) is the parity check.

## Consequences

- The aggregate rules (`Hearing` ~4.2k lines, `Case`, `Application`, `SeedHearingAggregate`,
  `PublishCourtListRequestAggregate`) are re-implemented as framework-free domain services over the
  viewstore, with unit tests for every guard (§5). This is the main parity risk; the legacy ITs and
  per-rule unit tests are the mitigation.
- Private events disappear from the Spring code path. Where the JEE flow chains command → private
  event → processor → internal command, the Spring service calls the next step directly in the
  same transaction.
- `listing-domain-aggregate-spring`, `AggregateStore`, `EventRecorder`'s private-event path,
  `aggregate_snapshot` usage and the `PrivateEventHandler` outbox route (P2.2/P2.3.0) are removed
  from the Spring app. Generated event POJOs may still be used as payload DTOs where convenient,
  but private events are never stored or published.
- `framework-api-domain` leaves the Spring classpath with the aggregates, so §12's "no framework
  artefacts" is met without vendoring it (supersedes D30's premise).
- Slices are cut by feature again (no aggregate-wide constraint, since nothing is shared with an
  event store): P2.3a listing notes → P2.3b court lists & XHIBIT → … → the `Hearing` command groups.

## Implementation notes (P2.3.1, 2026-11-19)

**Removed from the Spring app.** `listing-domain-aggregate-spring` (module and `settings.gradle`),
`AggregateStore`, `AggregateSnapshotJdbcRepository`, the Phase 1 snapshot test resources,
`EventRecorder`/`EventProjection(s)`, `ProcessorSender`, `ListingNoteEventProcessor`, and in
`listing-messaging` `PrivateEventHandler` and `InProcessEventRoute`. The relay, the inbound
listener and dedup are unchanged. `EventOutbox` now rejects any name that is not `public.*`;
`OutboxRoute` has one implementation (`PublicEventPublisher`). The `aggregate_snapshot` table
(changeset 030) stays: the JEE app still uses it.

**Payload DTOs.** `listing-domain-event-spring` is kept for the generated POJOs that public
payloads are built from (`uk.gov.justice.core.courts.*`, and the five
`uk.gov.justice.listing.courts.events` classes of `public.listing.court-list-published`, newly
vendored from `listing-event-processor`). The framework `@Event` annotation is stripped from the
vendored sources (`refresh-generated-sources.sh` does it), so `framework-api-domain` is gone from
the Spring runtime classpath (`./gradlew --offline :listing-api:dependencies --configuration
runtimeClasspath`: no `uk.gov.justice.*` artefact). The private-event POJOs are still compiled
(the generated types reference each other) but nothing stores or publishes them.

**The command pattern** (one `@Transactional`, framework-free service method per command):
load viewstore state → the ported aggregate guards (a rejected command logs and changes nothing)
→ the viewstore writes the JEE listener made for each would-be private event (ported as plain
methods) → the public messages the JEE processor emitted for those events (same names, payloads;
metadata from the command via `EventOutbox.envelopeFor`, `stream.id` = the former aggregate id)
→ `EventOutbox.write`. Processor steps that sent internal commands call the next service method
directly, in the same transaction. Processor steps that were asynchronous run after commit
(`AfterCommit`, `listing-command-spring` `command.support`).

**Public message metadata.** JEE built a public message from the private event's metadata
(`metadataFrom(event).withName(...)`): the command's metadata, a new id, `createdAt`, the command
id appended to `causation`, `stream.id`. `EventOutbox.envelopeFor(command, …)` yields the same
fields; only the id differs (the JEE public message reused the private event's id, which no
longer exists).

**Court lists (P2.3b).**
- `PublishCourtListRequestAggregate.weekCommencing` (its only field) → changeset 032,
  `publish_court_list_request` (one row per publish request, keyed by the random request id the
  JEE handler used as stream id). `record-court-list-produced` reads `week_commencing` from it;
  an unknown request id gives `false`, as a fresh aggregate did.
- Asynchrony: JEE ran "build + store the list, request the export" and "render, PUT, record,
  publish" as processor steps after commit, one at a time in outbox order (single relay).
  Spring runs them after commit on a dedicated **single-threaded FIFO executor**
  (`courtListExecutor`), each in its own transaction; for publish-court-lists-for-crown-courts this
  reproduces the JEE order (every centre's store before any export, so CREST siblings see each
  other's stored lists). Difference: no durable hand-off and no redelivery. A crash between commit
  and the step, or a failing step (e.g. Progression down while building
  `court-list-published`), is logged at ERROR and not retried; JEE's relay retried up to 10 times
  (re-sending the XML each time). The court-list flow already treats "publish again" as the retry.
- The internal commands (`store-published-court-list`, `court-list-request-export`,
  `record-court-list-export-successful/-failed`) are `CourtListCommands` methods; the deterministic
  court-list id (`UUIDService.getCourtListId`) is `CourtListCommands.courtListId`. The metadata of
  the processor's `court-list-request-export` (new id, request id as stream id, the requesting
  user only) is reproduced, and the export's public messages take their metadata from it.
- `listing-xhibit` (new Gradle module, §8): JAXB classes generated from the existing XSDs with the
  same XJC (2.3.7) and arguments as `maven-jaxb2-plugin` (identical class set, namespace prefixes
  `cs`/`apd`/`p2`/`xsi`), rewritten to `jakarta.xml.bind`; the mappers, metadata generator, file
  generator and XSD validation ported unchanged (the 49 JEE mapper/generator tests pass against
  the same expected XML files).
- WebDAV: the JEE code used Sardine 5.7. A single-file upload is one HTTP/1.1 `PUT
  <outbound-url>/<file>` with preemptive Basic auth and `Expect: 100-continue`, any non-2xx a
  failure; `XhibitService` sends that request with the JDK `HttpClient` instead, because Sardine
  5.7 needs `javax.xml.bind` (§12) and Apache HttpClient 4. Error messages (and so the recorded
  `errorMessage`) are the JEE ones. Config `listing.xhibit.webdav.*` (`LISTING_WEBDAV_*`).
- Failure behaviour kept on purpose: an export outcome for a list that was never stored fails the
  step (the JEE listener dereferenced `findBy` without a check); a REST `court-list-request-export`
  without `sendNotificationToParties` fails when building `court-list-published` (JEE unboxed the
  null), after which nothing of that export step is committed.

**Hearing slots (D33).** Making `sessionEndDate` optional moved it after the required parameters
in the generated `searchHearingSlots` signature; the controller kept the old order, so values
were bound to the wrong names (the cause of all three `HearingSlotsIT` failures). Fixed, and
guarded by `ControllerParameterOrderTest` (every controller parameter must carry the name the
generated method binds at that position). A non-JSON error body from courtscheduler is now passed
through as the raw string, as the JAX-RS adapter did (it was wrapped in `{"error":…}`).

## Implementation notes (P2.3.2 Hearing core, 2026-11-19)

**Shape.** The JEE Hearing aggregate's rules are ported unchanged as the framework-free
`command.hearing.rules.HearingRules` (with its value classes and converters; `Aggregate` and
`EventSwitcher` replaced by a local `EventSwitch`, `apply(Stream)` eager as the framework's). The
private event POJOs it raises are kept **in memory only** as the decision record (`RaisedEvent`:
the event plus the metadata the framework gave it) between three ports, all in the command's
transaction:
- `command.hearing.flow.HearingFlow.update(command, hearingId, rules -> events)` (the JEE
  `updateHearingEventStream`): load state (`HearingStateStore`) → rules → keep the command-side
  state → for each raised event the **listener port** (`command.hearing.listener`, the 26 JEE
  listener classes, verbatim) → queue its **processor port** step (`command.hearing.processor`, the
  JEE processor's private-event handlers, verbatim). Never stored, never published.
- `ProcessorQueue`: processor steps run after the command, breadth-first, in raised order; a
  follow-up command a step sends (`ProcessorSender` → `InternalCommands`, by name) runs at once and
  queues its own steps behind — the Phase 1 relay order. Public messages go to the outbox with the
  raised event's metadata and a new id.
- `command.hearing.handler.HearingCommands` (the JEE handler methods) and
  `command.hearing.api.HearingCommandApi` (the command API and its enrichment, outside the DB
  transaction) behind the listing-api controllers.

**State: field → storage** (changeset `033`, `hearing_rules_state(hearing_id, duplicate, deleted,
state jsonb, updated_at)`; `HearingRulesStateCodec` maps the rules' own value classes by field).
| Aggregate field (inv H#) | Storage |
|---|---|
| H23 `duplicate`, H24 `deleted` | columns `duplicate`, `deleted` (the tombstone; the listener port still deletes the hearing row, so no P2.1 query can see a tombstoned hearing — none reads this table) |
| H10 `prosecutionCaseDefendantOffenceIds` (incl. `isNewOffence`, seeding hearing, post-allocation subset, group flags) | `state.prosecutionCaseDefendantOffenceIds` |
| H26 `prosecutionCaseDefendants`, H27 `applicationOffenceIds`, H17 `confirmedCourtApplicationIds` | `state.prosecutionCaseDefendants`, `state.applicationOffenceIds`, `state.confirmedCourtApplicationIds` |
| H19 `updateSlot`, H20 `hasAdjournmentDate`, H32 `unAllocatedListedCases` | `state.updateSlot`, `state.hasAdjournmentDate`, `state.unAllocatedListedCases` |
| H2 `type`, H5 `estimatedMinutes`, H7 `courtRoomId`, H9 `judiciary`, H14 `nonDefaultDays`, H16 `hearingDays` (listener rules differ: welsh type after extension, 0/1→20 and cancelled-day minutes, room kept on unallocation, cancelled days and sequence resets kept, the reduced copies `hasChanged` compares) | `state.type`, `.estimatedMinutes`, `.courtRoomId`, `.judiciary`, `.nonDefaultDays`, `.hearingDays` |
| H1, H3, H4, H6, H8, H11–H13, H15, H18, H21, H22, H25, H29, H31, H28 `currentHearingEventState` | the viewstore: `hearing.properties` (read as the listed hearing; `hasVideoLink`, `publicListNote`, `resulted`, `judiciaryAssignmentSource` beside it) |
| H30 `isSummonsApprovedExists` | dropped (decides nothing) |

Known consequence (VS is the truth for H28): the rules see `restrictFromCourtList`,
`caseIdentifier`, ejection and group-member cases as the listener wrote them, not as the JEE
aggregate kept them (inventory §8 B), so e.g. an under-18 restriction is not re-raised once the
viewstore has it. A hearing row deleted while its state is not tombstoned (hearing-deleted for a
different id) keeps its id in the state, so it still cannot be re-listed, as in JEE.

**Other decisions.**
- Processor steps run synchronously in the command transaction (D5). External reads in a step
  (refdata organisation-unit, progression case for hearing-confirmed) failing fail the command (JEE
  retried and dropped the step). The courtscheduler slot release (`available-slots-for-hearing-freed`)
  is caught and logged, never failing the command.
- `HearingRepository.save` is DeltaSpike's (persist new, keep managed): Spring Data's merge re-read
  the row by id, selecting the native-query-only columns. A missing hearing in
  update-hearings-for-listing is detected before the transactional query view so the per-hearing
  catch does not leave the transaction rollback-only.
- P2.3.3 seams (`processor.deferred.DeferredCommands`): `add-hearing-to-case` publishes
  `public.listing.hearing-added-to-case` (the stateless part of `Case.addHearing`); 17 other
  Case/Application/seed/C7 commands the processor sends are logged at WARN and ignored.
  *Superseded by P2.3.3: the class is removed.*
- `AfterCommit` (court lists) retries a failing step up to 10 attempts (the relay's), each in its
  own transaction, then logs at ERROR and drops it. `javax.xml.bind:jaxb-api` is excluded from
  liquibase-core (no runtime class references it).
- `ObjectMapperProducer` deserialised `ZonedDateTime` with zone `Z` where the framework gave `UTC`.
  *Fixed in P2.3.3 (D35).*

## Implementation notes (P2.3.3 remaining command slices, 2026-11-19)

**Shape.** As P2.3.2. The JEE `Case`, `Application` and `SeedHearingAggregate` are ported verbatim
as `command.hearing.rules.CaseRules`, `ApplicationRules` and `SeedHearingRules`. Their handlers are
`CaseCommandHandler` (Case/Application parts of `ListingCommandHandler`, the case part of
`HearingMarkedAsDuplicateCommandHandler`, `RemoveCaseFromGroupCasesCommandHandler`) and
`SeedHearingCommandHandler` (`ListNextHearingCommandHandler`, `UnscheduledListingCommandHandler`,
`DeletePreviousHearingsAndCreateNextHearingHandler` and `UpdateExistingHearingCommandHandler` seed
methods). The services are `CaseCommands` (internal, by name) and `NextHearingCommands` (API). The
rest of Table 2 (17 Hearing-stream commands) is in `HearingCommandHandler`/`HearingCommands`.
`HearingFlow.updateCase/updateApplication/updateSeedHearing` load the state, run the rules, keep the
state **only when something was raised** (every state change of these aggregates is an event), then
hand the events to the same listener port and `ProcessorQueue` with the former stream id
(`stream.id`) in the metadata. `processor.deferred.DeferredCommands` is removed; every command the
processor port sends has a service (`CaseCommandsTest` checks the 44 internal names).

**State: field → storage** (changeset `034`, three tables `(<id> uuid pk, state jsonb, updated_at)`;
`RulesStateStore`, mapped by field with the `HearingRulesStateCodec` mapper):
| Aggregate field | Storage |
|---|---|
| `Case.hearingIds` (set) | `case_rules_state.state.hearingIds` (array; restored into a `HashSet`, so payload order is the JEE one for the same set) |
| `Case.currentDefendants` (defendant id → `events.Defendant` as last updated, the merge base of `updateDefendant`) | `case_rules_state.state.currentDefendants` |
| `Case.defendantsToBeUpdated` (the "later" update replayed by `addHearing` to the case's first hearing) | `case_rules_state.state.defendantsToBeUpdated` |
| `Application.hearingIds` (list, duplicates kept) | `application_rules_state.state.hearingIds` |
| `SeedHearingAggregate.seededHearingIdsMapForHearingDay`, `existingHearingIdsMapForHearingDay` (sitting day → hearing ids) | `seed_hearing_rules_state.state.seededHearingIdsForHearingDay`, `.existingHearingIdsForHearingDay` (entry lists: the sitting day may be `null`) |
| `SeedHearingAggregate.previousHearingIds`, `currentHearingIds`, `seedingHearingId` | `seed_hearing_rules_state.state.previousHearingIds`, `.currentHearingIds`, `.seedingHearingId` |
None of these aggregates had any viewstore state; none of their events has a JEE listener apart from
those already ported for Hearing.

**Decisions and kept behaviour.**
- `Case.markHearingAsDuplicate` returned its event unapplied (the JEE store applied it on the next
  load); `CaseRules` applies it at once, so the kept state is what that replay gave.
- Known JEE bug kept: `SeedHearingAggregate:300` removes `existingHearingIdsMapForHearingDay` by the
  existing hearing's id (a UUID) from a map keyed by sitting day, so a later delete for the same day
  asks again to remove the offences from that hearing (pinned by `RulesStateStoreTest`).
- `list-next-hearings-enriched-v2` and `delete-next-hearings` release the courtscheduler slots
  (`HearingSlotsService.delete`, HTTP DELETE) inside the decision, before the events, as JEE: a
  failing release fails the command; releases already made are not undone (as before).
- `update-defendant-court-proceedings` keeps the HMI branch (`createHmiEvents`): both branches raise
  the same events.
- Command API (listing-command-spring `api`, listing-common-spring clients): next hearings with
  `PtphDetailEnrichmentService`/`PtphDetailService`/`HearingQueryClient` (hearing context REST),
  update-related-hearing, list-unscheduled-next-hearings, delete-next-hearings,
  delete-previous-hearings-and-create-next-hearing, migrate, mark-unallocated-as-duplicate and
  split-hearing (`ProgressionSplitHearingService`, REST POST to progression; any status but 202 →
  `SplitHearingRejectedException`, mapped as the JEE `SplitHearingRejectedExceptionMapper`: 409 →
  422 with progression's error code or `HEARING_NOT_SPLITTABLE`, other statuses passed through).
  New client properties `hearing-query-base-url`, `progression-command-base-url`
  (`LISTING_HEARING_QUERY_BASE_URL`, `LISTING_PROGRESSION_COMMAND_BASE_URL`). The envelope name is
  the RAML `(mapping)` name. The request body of `POST /hearings/{hearingId}` is optional in the
  spec (duplicate-unallocated-hearing has none in the RAML); the controller still requires it for
  the other media types. D34: the four RAML-only media types pass through.
- D35: `ZonedDateTime` deserialises to the region zone `UTC` as the framework did; the
  serialised strings were already identical.


## Implementation notes (P2.4 inbound public messages, 2026-11-19)

**Shape.** Decision 2 ("inbound public messages update the viewstore directly") is implemented by
reusing the P2.3 command services: each of the 28 inbound handlers (`listing-command-spring`
`command.inbound`, details in ADR 013 "Implementation notes (P2.4)") turns the upstream message into
the internal command(s) the JEE processor sent, and calls `HearingCommands` / `CaseCommands`
directly (`ProcessorSender` → `InternalCommands`). So the viewstore changes, the rules state, the
public messages (outbox) and the dedup row are one transaction, and the guards, listener port and
processor port are the ones the legacy parity checks already cover. Requester calls: the CPS
prosecutor handler's hearing search is the P2.1 query view (JEE: in-VM, no user); the
marked-as-duplicate session release is `HearingSlotsService.delete` (system user, D19).

**Concurrency.** `HearingStateStore.load` and `RulesStateStore.load*` take a per-instance Postgres
advisory transaction lock before reading (rationale, deadlock and persistence-context caveats in
ADR 013 P2.4 notes). Commands and inbound messages on one hearing, case, application or seeding
hearing now serialise; before P2.4 two concurrent commands could both read the same state and the
later commit overwrote the earlier one's rules state.

**Fix.** `HearingStateStore.save(hearingId, rules)`: rules state that has no hearing id of its own
(a hearing never listed, tombstoned by mark-hearing-as-duplicate) is keyed by the stream id instead
of failing on a null primary key.
