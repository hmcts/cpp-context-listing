package uk.gov.moj.cpp.listing.command.hearing.rules;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.builder;
import static uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded.caseResultedDefendantProceedingsConcluded;
import static uk.gov.moj.cpp.listing.command.hearing.rules.EventSwitch.match;
import static uk.gov.moj.cpp.listing.command.hearing.rules.EventSwitch.otherwiseDoNothing;
import static uk.gov.moj.cpp.listing.command.hearing.rules.EventSwitch.when;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.events.CaseEjected;
import uk.gov.justice.listing.events.CaseEjectedForHearings;
import uk.gov.justice.listing.events.CaseMarkersToBeUpdated;
import uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded;
import uk.gov.justice.listing.events.DefendantLegalaidStatusUpdated;
import uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings;
import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.DefendantsToBeUpdatedLater;
import uk.gov.justice.listing.events.HearingAddedToCase;
import uk.gov.justice.listing.events.HearingMarkedAsDuplicateForCase;
import uk.gov.justice.listing.events.HearingUpdatedToCase;
import uk.gov.justice.listing.events.LinkedCasesToBeUpdated;
import uk.gov.justice.listing.events.MasterCaseUpdatedForGroup;
import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CaseSimpleOffences;
import uk.gov.moj.cpp.listing.domain.Cases;
import uk.gov.moj.cpp.listing.domain.Defendant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JEE {@code Case} aggregate's rules, framework-free (MBD refactor P2.3.3, ADR 015): ported
 * verbatim (same guards, same events, same state changes), with {@code Aggregate}/{@code EventSwitcher}
 * replaced by {@link EventSwitch}. The state (the hearings the case is on, the defendants as last
 * updated and the defendant updates waiting for a first hearing) is restored from and kept in
 * {@code case_rules_state} ({@link CaseRulesState}); the raised events are never stored.
 *
 * <p>One deliberate difference of form, not of behaviour: {@link #markHearingAsDuplicate} returned
 * its event without applying it (the JEE event store applied it on the next load); here it is applied
 * at once, so the kept state is what that replay produced.
 */
@SuppressWarnings({"pmd:BeanMembersShouldSerialize", "squid:S1068"})
public class CaseRules {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseRules.class);

    private UUID caseId;

    private final Set<UUID> hearingIds = new HashSet<>();

    private final Map<UUID, uk.gov.justice.listing.events.Defendant> currentDefendants = new HashMap<>();

    private final Set<uk.gov.justice.listing.events.Defendant> defendantsToBeUpdated = new HashSet<>();

    public CaseRules() {
    }

    public CaseRules(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<UUID> getHearingIds() {
        return unmodifiableList(new ArrayList<>(this.hearingIds));
    }

    public Stream<Object> apply(final Stream<Object> events) {
        return events.map(this::apply).collect(toList()).stream();
    }

    public Object apply(final Object event) {
        return match(event).with(
                when(HearingAddedToCase.class).apply(this::onHearingAddedToCase),
                when(HearingUpdatedToCase.class).apply(this::onHearingUpdatedToCase),
                when(DefendantsToBeUpdated.class).apply(this::onDefendantsToBeUpdated),
                when(DefendantsToBeUpdatedLater.class).apply(this::onDefendantsToBeUpdatedLater),
                when(OffencesToBeAdded.class).apply(e -> onOffencesToBeAdded()),
                when(OffencesToBeDeleted.class).apply(e -> onOffencesToBeDeleted()),
                when(OffencesToBeUpdated.class).apply(e -> onOffencesToBeUpdated()),
                when(CaseEjected.class).apply(e -> onCaseEjected()),
                when(DefendantLegalaidStatusUpdated.class).apply(e -> onDefendantLegalaidStatusTobeUpdated()),
                when(CaseResultedDefendantProceedingsConcluded.class).apply(e -> onCaseResultedDefendantProceedingsUpdated()),
                when(CaseEjectedForHearings.class).apply(e -> onCaseEjectedForHearings()),
                when(HearingMarkedAsDuplicateForCase.class).apply(this::onHearingMarkedAsDuplicateForCase),
                when(MasterCaseUpdatedForGroup.class).apply(this::onMasterCaseUpdatedForGroup),
                otherwiseDoNothing());
    }

    public Stream<Object> addHearing(final UUID caseId, final UUID hearingId) {
        final Stream.Builder<Object> streamBuilder = builder()
                .add(new HearingAddedToCase(caseId, hearingId));

        this.defendantsToBeUpdated.forEach(defendant ->
                streamBuilder.add(DefendantsToBeUpdated.defendantsToBeUpdated()
                        .withCaseId(caseId)
                        .withDefendants(singletonList(defendant))
                        .withHearings(singletonList(hearingId))
                        .build()));

        return apply(streamBuilder.build());
    }

    public Stream<Object> updateHearing(final UUID caseId, final UUID allocatedHearingId, final UUID unAllocatedHearingId) {
        return apply(Stream.of(new HearingUpdatedToCase(caseId, allocatedHearingId, unAllocatedHearingId)));
    }

    public Stream<Object> updateDefendant(final UUID caseId, final Defendant defendant) {
        // Build the new defendant event
        uk.gov.justice.listing.events.Defendant newDefendantEvent = NewDomainToEventConverter.buildDefendant(defendant);

        // Check if we have an existing defendant for this ID
        final uk.gov.justice.listing.events.Defendant existingDefendant = currentDefendants.get(defendant.getId());
        if (existingDefendant != null) {
            // Merge: Use updateEventDefendant logic to preserve existing values where new ones are null
            newDefendantEvent = NewDomainToEventConverter.updateEventDefendant(
                    NewDomainToEventConverter.buildNewBaseDefendant(defendant),  // Convert domain to NewBaseDefendant for merging
                    existingDefendant
            );
        }

        if (hearingIds.isEmpty()) {
            // For deferred updates, store the merged defendant
            defendantsToBeUpdated.clear();  // Clear old ones if needed, or handle per defendant
            defendantsToBeUpdated.add(newDefendantEvent);
            return apply(Stream.of(DefendantsToBeUpdatedLater.defendantsToBeUpdatedLater()
                    .withCaseId(caseId)
                    .withDefendants(singletonList(newDefendantEvent))
                    .build()));
        }

        return apply(Stream.of(DefendantsToBeUpdated.defendantsToBeUpdated()
                .withCaseId(caseId)
                .withDefendants(singletonList(newDefendantEvent))
                .withHearings(new ArrayList<>(hearingIds))
                .build()));
    }

    public Stream<Object> updateDefendantOffences(final CaseOffences caseOffences) {
        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(OffencesToBeUpdated.offencesToBeUpdated()
                .withCaseId(caseOffences.getCaseId())
                .withDefendantId(caseOffences.getDefendantId())
                .withOffences(NewDomainToEventConverter.buildOffences(caseOffences.getOffences()))
                .withHearings(new ArrayList<>(hearingIds))
                .build()));
    }

    public Stream<Object> addedDefendantForCourtProceedings(final UUID caseId, final Defendant defendant) {
        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(DefendantsToBeAddedForCourtProceedings.defendantsToBeAddedForCourtProceedings()
                .withCaseId(caseId)
                .withDefendants(singletonList(NewDomainToEventConverter.buildDefendant(defendant)))
                .withHearings(new ArrayList<>(hearingIds))
                .build()));
    }

    public Stream<Object> linkCases(final String linkActionType, final UUID caseId, final String caseUrn, final Cases cases) {
        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }
        return apply(Stream.of(LinkedCasesToBeUpdated.linkedCasesToBeUpdated()
                .withLinkActionType(linkActionType)
                .withCaseId(caseId)
                .withCaseUrn(caseUrn)
                .withHearingIds(new ArrayList<>(hearingIds))
                .withLinkedToCases(NewDomainToEventConverter.convertDomainToLinkedToCasesEvent(cases.getLinkedToCases()))
                .build()));
    }

    public Stream<Object> addedCaseMarkers(final UUID caseId, final List<CaseMarker> caseMarkers) {
        if (hearingIds.isEmpty()) {
            // Observability for the cross-aggregate race: this update is silently dropped because the
            // case is not yet linked to any hearing (hearingIds populated only by the async
            // add-hearing-to-case command). A re-published progression event lands once the link exists.
            LOGGER.warn("update-case-markers for case {} DROPPED: case not yet linked to any hearing (hearingIds empty)", caseId);
            return Stream.empty();
        }

        LOGGER.info("update-case-markers for case {} APPLIED to hearings {}", caseId, hearingIds);
        return apply(Stream.of(CaseMarkersToBeUpdated.caseMarkersToBeUpdated()
                .withProsecutionCaseId(caseId)
                .withMarkers(NewDomainToEventConverter.convertCaseMarkersListToMarkers(caseMarkers))
                .withHearings(new ArrayList<>(hearingIds))
                .build()));
    }

    public Stream<Object> deleteDefendantOffences(final CaseSimpleOffences caseSimpleOffences) {
        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(OffencesToBeDeleted.offencesToBeDeleted()
                .withCaseId(caseSimpleOffences.getCaseId())
                .withDefendantId(caseSimpleOffences.getDefendantId())
                .withOffences(NewDomainToEventConverter.buildSimpleOffences(caseSimpleOffences.getOffences()))
                .withHearings(new ArrayList<>(hearingIds))
                .build()));
    }

    public Stream<Object> addedDefendantOffences(final CaseOffences caseOffences) {
        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(OffencesToBeAdded.offencesToBeAdded()
                .withCaseId(caseOffences.getCaseId())
                .withDefendantId(caseOffences.getDefendantId())
                .withOffences(NewDomainToEventConverter.buildOffences(caseOffences.getOffences()))
                .withHearings(new ArrayList<>(hearingIds))
                .build()));
    }

    public Stream<Object> ejectCaseForHearings(final List<UUID> hearingIdOfEjectCase, final UUID caseId, final Optional<String> removalReason) {

        final Set<UUID> mergedHearingIds = new HashSet<>(this.hearingIds);
        if (Objects.nonNull(hearingIdOfEjectCase)) {
            if (!hearingIds.isEmpty()) {
                hearingIdOfEjectCase.retainAll(hearingIds);
            }
            mergedHearingIds.addAll(hearingIdOfEjectCase);
        }

        final String ejectReason = removalReason.isPresent() ? removalReason.get() : null;
        return mergedHearingIds.isEmpty() ? Stream.empty() : apply(Stream.of(CaseEjectedForHearings.caseEjectedForHearings()
                .withProsecutionCaseId(caseId)
                .withHearingIds(new ArrayList<>(mergedHearingIds))
                .withRemovalReason(ejectReason)
                .build())
        );
    }

    public Stream<Object> updateDefendantLegalAidStatus(final UUID caseId, final UUID defendantId, final String legalAidStatus) {
        return this.hearingIds.isEmpty() ? Stream.empty() : apply(Stream.of(DefendantLegalaidStatusUpdated.defendantLegalaidStatusUpdated()
                .withHearingIds(new ArrayList<>(hearingIds))
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withLegalAidStatus(legalAidStatus)
                .build()));
    }

    public Stream<Object> updateDefendantCaseResultedAndUpdated(final ProsecutionCase prosecutionCase) {
        if (this.hearingIds.isEmpty()) {
            return Stream.empty();
        }
        return apply(Stream.of(caseResultedDefendantProceedingsConcluded()
                .withHearingIds(new ArrayList<>(this.hearingIds))
                .withProsecutionCase(prosecutionCase)
                .build()));
    }

    /** JEE: returned unapplied, applied by the next load's replay; applied at once here (see the class javadoc). */
    public Stream<Object> markHearingAsDuplicate(final UUID hearingId, final UUID caseId) {
        return apply(Stream.of(HearingMarkedAsDuplicateForCase.hearingMarkedAsDuplicateForCase()
                .withCaseId(caseId)
                .withHearingId(hearingId)
                .build()));
    }

    /* when the master case of a group is changed, newGroupMaster comes to hearing context for the first time
    this method is here just to initiate the aggregate for that new case (newGroupMaster)
    and the event created (MasterCaseUpdatedForHearing) is not being consumed by any listener/processor
    * */
    public Stream<Object> updateMasterCaseForGroup(final UUID newGroupMaster, final List<UUID> hearingIds) {
        return apply(Stream.of(MasterCaseUpdatedForGroup.masterCaseUpdatedForGroup()
                .withCaseId(newGroupMaster)
                .withHearingIds(hearingIds)
                .build())
        );
    }

    // State (case_rules_state, P2.3.3)

    /** The state to keep, or {@code null} for a case nothing was decided on. */
    public CaseRulesState exportState() {
        if (caseId == null) {
            return null;
        }
        return new CaseRulesState(caseId, new ArrayList<>(hearingIds), new HashMap<>(currentDefendants), new ArrayList<>(defendantsToBeUpdated));
    }

    /** Restores the kept state (the JEE aggregate rebuilt from its stream). */
    public void restore(final CaseRulesState state) {
        this.caseId = state.caseId();
        this.hearingIds.clear();
        this.currentDefendants.clear();
        this.defendantsToBeUpdated.clear();
        if (state.hearingIds() != null) {
            this.hearingIds.addAll(state.hearingIds());
        }
        if (state.currentDefendants() != null) {
            this.currentDefendants.putAll(state.currentDefendants());
        }
        if (state.defendantsToBeUpdated() != null) {
            this.defendantsToBeUpdated.addAll(state.defendantsToBeUpdated());
        }
    }

    // Methods to apply aggregate state

    private void onHearingAddedToCase(final HearingAddedToCase event) {
        this.hearingIds.add(event.getHearingId());
    }

    private void onHearingUpdatedToCase(final HearingUpdatedToCase event) {
        this.hearingIds.add(event.getExistingHearingId());
    }

    // Update onDefendantsToBeUpdatedLater to store the defendant
    private void onDefendantsToBeUpdatedLater(final DefendantsToBeUpdatedLater event) {
        event.getDefendants().forEach(defendant -> currentDefendants.put(defendant.getId(), defendant));
        this.defendantsToBeUpdated.addAll(event.getDefendants());
    }

    private void onDefendantsToBeUpdated(final DefendantsToBeUpdated event) {
        event.getDefendants().forEach(defendant -> currentDefendants.put(defendant.getId(), defendant));
        this.defendantsToBeUpdated.clear();
    }

    private void onOffencesToBeUpdated() {
        // Do nothing
    }

    private void onOffencesToBeDeleted() {
        // Do nothing
    }

    private void onOffencesToBeAdded() {
        // Do nothing
    }

    private void onCaseEjected() {
        // Do nothing
    }

    private void onCaseEjectedForHearings() {
        // Do nothing
    }

    private void onDefendantLegalaidStatusTobeUpdated() {
        // Do nothing
    }

    private void onCaseResultedDefendantProceedingsUpdated() {
        // Do Nothing
    }

    private void onHearingMarkedAsDuplicateForCase(final HearingMarkedAsDuplicateForCase hearingMarkedAsDuplicateForCase) {
        this.hearingIds.remove(hearingMarkedAsDuplicateForCase.getHearingId());
    }

    private void onMasterCaseUpdatedForGroup(final MasterCaseUpdatedForGroup masterCaseUpdatedForGroup) {
        this.hearingIds.addAll(masterCaseUpdatedForGroup.getHearingIds());
    }
}
