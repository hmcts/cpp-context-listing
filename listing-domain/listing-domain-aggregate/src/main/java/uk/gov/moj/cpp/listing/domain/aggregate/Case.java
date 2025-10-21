package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Stream.builder;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded.caseResultedDefendantProceedingsConcluded;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.CaseEjected;
import uk.gov.justice.listing.events.CaseMarkersToBeUpdated;
import uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded;
import uk.gov.justice.listing.events.DefendantLegalaidStatusUpdated;
import uk.gov.justice.listing.events.CaseEjectedForHearings;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings({"pmd:BeanMembersShouldSerialize", "squid:S1068"})
public class Case implements Aggregate {

    private static final long serialVersionUID = 203L;

    private final Set<UUID> hearingIds = new HashSet<>();

    private final Set<uk.gov.justice.listing.events.Defendant> defendantsToBeUpdated = new HashSet<>();

    public List<UUID> getHearingIds() {
        return unmodifiableList(new ArrayList<>(this.hearingIds));
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(HearingAddedToCase.class).apply(this::onHearingAddedToCase),
                when(HearingUpdatedToCase.class).apply(this::onHearingUpdatedToCase),
                when(DefendantsToBeUpdated.class).apply(e -> onDefendantsToBeUpdated()),
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

    public Stream<Object> updateDefendant(UUID caseId, Defendant defendant) {
        if (hearingIds.isEmpty()) {
            return apply(Stream.of(DefendantsToBeUpdatedLater.defendantsToBeUpdatedLater()
                    .withCaseId(caseId)
                    .withDefendants(singletonList(NewDomainToEventConverter.buildDefendant(defendant)))
                    .build()));
        }

        return apply(Stream.of(DefendantsToBeUpdated.defendantsToBeUpdated()
                .withCaseId(caseId)
                .withDefendants(singletonList(NewDomainToEventConverter.buildDefendant(defendant)))
                .withHearings(new ArrayList<>(hearingIds))
                .build()));
    }

    public Stream<Object> updateDefendantOffences(CaseOffences caseOffences) {
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

    public Stream<Object> addedDefendantForCourtProceedings(UUID caseId, Defendant defendant) {
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

    public Stream<Object> addedCaseMarkers(UUID caseId, List<CaseMarker> caseMarkers) {
        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(CaseMarkersToBeUpdated.caseMarkersToBeUpdated()
                .withProsecutionCaseId(caseId)
                .withMarkers(NewDomainToEventConverter.convertCaseMarkersListToMarkers(caseMarkers))
                .withHearings(new ArrayList<>(hearingIds))
                .build()));
    }

    public Stream<Object> deleteDefendantOffences(CaseSimpleOffences caseSimpleOffences) {
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

    public Stream<Object> addedDefendantOffences(CaseOffences caseOffences) {
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

    public Stream<Object> ejectCaseForHearings(List<UUID> hearingIdOfEjectCase, UUID caseId, Optional<String> removalReason) {

        final Set<UUID> mergedHearingIds = new HashSet<>(this.hearingIds);
        if (Objects.nonNull(hearingIdOfEjectCase)) {
            if(!hearingIds.isEmpty()) {
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

    public Stream<Object> markHearingAsDuplicate(final UUID hearingId, final UUID caseId) {
        return Stream.of(HearingMarkedAsDuplicateForCase.hearingMarkedAsDuplicateForCase()
                .withCaseId(caseId)
                .withHearingId(hearingId)
                .build());
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

    // Methods to apply aggregate state

    private void onHearingAddedToCase(HearingAddedToCase event) {
        this.hearingIds.add(event.getHearingId());
    }

    private void onHearingUpdatedToCase(HearingUpdatedToCase event) {
        this.hearingIds.add(event.getExistingHearingId());
    }

    private void onDefendantsToBeUpdatedLater(final DefendantsToBeUpdatedLater event) {
        this.defendantsToBeUpdated.add(event.getDefendants().get(0));
    }

    private void onDefendantsToBeUpdated() {
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

    private void onHearingMarkedAsDuplicateForCase(final HearingMarkedAsDuplicateForCase hearingMarkedAsDuplicateForCase){
        this.hearingIds.remove(hearingMarkedAsDuplicateForCase.getHearingId());
    }

    private void onMasterCaseUpdatedForGroup(MasterCaseUpdatedForGroup masterCaseUpdatedForGroup) {
        this.hearingIds.addAll(masterCaseUpdatedForGroup.getHearingIds());
    }
}
