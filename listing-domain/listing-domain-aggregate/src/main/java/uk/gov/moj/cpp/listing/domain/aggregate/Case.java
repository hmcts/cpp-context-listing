package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Collections.singletonList;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.CaseEjectedForHearings;
import uk.gov.justice.listing.events.CaseMarkersToBeUpdated;
import uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings;
import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.HearingAddedToCase;
import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CaseSimpleOffences;
import uk.gov.moj.cpp.listing.domain.Defendant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings("squid:S1068")
public class Case implements Aggregate {

    private static final long serialVersionUID = 201L;

    private final List<UUID> hearingIds = new ArrayList<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(HearingAddedToCase.class).apply(this::onHearingAddedToCase),
                when(DefendantsToBeUpdated.class).apply(e -> onDefendantsToBeUpdated()),
                when(OffencesToBeAdded.class).apply(e -> onOffencesToBeAdded()),
                when(OffencesToBeDeleted.class).apply(e -> onOffencesToBeDeleted()),
                when(OffencesToBeUpdated.class).apply(e -> onOffencesToBeUpdated()),
                when(CaseEjectedForHearings.class).apply(e -> onCaseEjectedForHearings()),
                otherwiseDoNothing());
    }

    public Stream<Object> addHearing(final UUID caseId, final UUID hearingId) {

        return apply(Stream.of(new HearingAddedToCase(caseId, hearingId)));
    }

    public Stream<Object> updateDefendant(UUID caseId, Defendant defendant) {
        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(DefendantsToBeUpdated.defendantsToBeUpdated()
                .withCaseId(caseId)
                .withDefendants(singletonList(NewDomainToEventConverter.buildDefendant(defendant)))
                .withHearings(hearingIds)
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
                .withHearings(hearingIds)
                .build()));
    }

    public Stream<Object> addedDefendantForCourtProceedings(UUID caseId, Defendant defendant) {
        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(DefendantsToBeAddedForCourtProceedings.defendantsToBeAddedForCourtProceedings()
                .withCaseId(caseId)
                .withDefendants(singletonList(NewDomainToEventConverter.buildDefendant(defendant)))
                .withHearings(hearingIds)
                .build()));
    }

    public Stream<Object> addedCaseMarkers(UUID caseId, List<CaseMarker> caseMarkers) {
        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(CaseMarkersToBeUpdated.caseMarkersToBeUpdated()
                .withProsecutionCaseId(caseId)
                .withMarkers(NewDomainToEventConverter.convertCaseMarkersListToMarkers(caseMarkers))
                .withHearings(hearingIds)
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
                .withHearings(hearingIds)
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
                .withHearings(hearingIds)
                .build()));
    }

    public Stream<Object> ejectCaseForHearings(List<UUID> hearingIdOfEjectCase, UUID caseId, Optional<String> removalReason) {

        final Set<UUID> mergedHearingIds = new HashSet<>(this.hearingIds);
        if (Objects.nonNull(hearingIdOfEjectCase)) {
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

    // Methods to apply aggregate state

    private void onHearingAddedToCase(HearingAddedToCase event) {
        this.hearingIds.add(event.getHearingId());
    }

    private void onDefendantsToBeUpdated() {
        // Do nothing
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

    private void onCaseEjectedForHearings() {
        // Do nothing
    }

}
