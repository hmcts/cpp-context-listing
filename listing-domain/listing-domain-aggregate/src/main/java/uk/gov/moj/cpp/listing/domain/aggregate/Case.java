package uk.gov.moj.cpp.listing.domain.aggregate;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.*;
import uk.gov.moj.cpp.listing.domain.*;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.Hearing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.*;
import static uk.gov.moj.cpp.listing.domain.aggregate.DomainToEventConverter.createHearingsFrom;

@SuppressWarnings("squid:S1068")
public class Case implements Aggregate {

    private static final long serialVersionUID = 1L;

    private UUID caseId;
    private String urn;
    private List<Hearing> hearings = new ArrayList<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CaseSentForListing.class).apply(this::onCaseSentForListing),
                when(DefendantsToBeUpdated.class).apply(e -> onDefendantsToBeUpdated()),
                when(OffencesToBeAdded.class).apply(e -> onOffencesToBeAdded()),
                when(OffencesToBeDeleted.class).apply(e -> onOffencesToBeDeleted()),
                when(OffencesToBeUpdated.class).apply(e -> onOffencesToBeUpdated()),
                otherwiseDoNothing());
    }

    public Stream<Object> sendForListing(final UUID caseId, final String urn, final List<Hearing> hearings) {

        return apply(Stream.of(new CaseSentForListing(caseId,  createHearingsFrom(hearings), urn)));
    }

    public Stream<Object> updateDefendants(List<Defendant> defendants) {
        final List<UUID> hearingIds = this.hearings.stream().map(hearing ->
                UUID.fromString(hearing.getId())).collect(Collectors.toList());

        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(DefendantsToBeUpdated.defendantsToBeUpdated()
            .withDefendants(DomainToEventConverter.createDefendantsFrom(defendants))
            .withHearings(hearingIds)
            .build()));
    }

    public Stream<Object> updateDefendantOffences(CaseOffences caseOffences) {
        final List<UUID> hearingIds = this.hearings.stream().map(hearing ->
                UUID.fromString(hearing.getId())).collect(Collectors.toList());

        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(OffencesToBeUpdated.offencesToBeUpdated()
                .withOffences(DomainToEventConverter.createOffencesFrom(caseOffences))
                .withHearings(hearingIds)
                .build()));
    }

    public Stream<Object> deleteDefendantOffences(CaseSimpleOffences caseSimpleOffences) {
        final List<UUID> hearingIds = this.hearings.stream().map(hearing ->
                UUID.fromString(hearing.getId())).collect(Collectors.toList());

        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(OffencesToBeDeleted.offencesToBeDeleted()
                .withOffences(DomainToEventConverter.createDeletedOffencesFrom(caseSimpleOffences))
                .withHearings(hearingIds)
                .build()));
    }

    public Stream<Object> addedDefendantOffences(CaseOffences defendants) {
        final List<UUID> hearingIds = this.hearings.stream().map(hearing ->
                UUID.fromString(hearing.getId())).collect(Collectors.toList());

        if (hearingIds.isEmpty()) {
            return Stream.empty();
        }

        return apply(Stream.of(OffencesToBeAdded.offencesToBeAdded()
                .withOffences(DomainToEventConverter.createOffencesFrom(defendants))
                .withHearings(hearingIds)
                .build()));
    }

    // Methods to apply aggregate state

    private void onCaseSentForListing(CaseSentForListing event) {
        this.caseId = event.getCaseId();
        this.urn = event.getUrn();

        if(this.hearings==null){
            this.hearings = EventToDomainConverter.createHearingsFrom(event.getHearings());
        }
        else {
            this.hearings.addAll(EventToDomainConverter.createHearingsFrom(event.getHearings()));
        }
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
}
