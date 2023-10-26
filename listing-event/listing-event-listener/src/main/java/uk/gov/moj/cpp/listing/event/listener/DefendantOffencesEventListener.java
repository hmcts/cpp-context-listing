package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.listing.event.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;
import static uk.gov.moj.cpp.listing.event.util.ReportingRestrictionHelper.dedupReportingRestrictions;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceUpdated;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Iterables;

@ServiceComponent(Component.EVENT_LISTENER)
public class DefendantOffencesEventListener {

    private static final String LISTED_CASES_FIELD = "listedCases";

    private static final TypeReference<List<ListedCase>> LISTED_CASE_TYPE = new TypeReference<List<ListedCase>>() {};

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingSearchSyncService hearingSearchSyncService;


    @Handles("listing.events.offence-updated")
    public void offenceUpdated(final Envelope<OffenceUpdated> event) {
        final OffenceUpdated offenceUpdated = event.payload();
        final UUID hearingId = offenceUpdated.getHearingId();
        final UUID caseId = offenceUpdated.getCaseId();
        final UUID defendantId = offenceUpdated.getDefendantId();
        final Offence offence = dedupAllReportingRestrictions(offenceUpdated.getOffence());

        if (nonNull(hearingRepository.findBy(hearingId))) {
            using(hearingRepository)
                    .find(hearingId)
                    .putSubList(LISTED_CASES_FIELD, LISTED_CASE_TYPE, getUpdatedListedCaseFunction(caseId, defendantId, offence))
                    .save();
        }

        hearingSearchSyncService.sync(hearingId);
    }

    private Function<List<ListedCase>, List<ListedCase>> getUpdatedListedCaseFunction(UUID caseId, UUID defendantId, Offence offence) {
        return cases -> getUpdatedListedCase(caseId, defendantId, offence, cases);
    }

    @Handles("listing.events.offence-deleted")
    public void offenceDeleted(final Envelope<OffenceDeleted> event) {
        final OffenceDeleted offenceDeleted = event.payload();
        final UUID hearingId = offenceDeleted.getHearingId();
        final UUID caseId = offenceDeleted.getCaseId();
        final UUID defendantId = offenceDeleted.getDefendantId();
        final UUID offenceId = offenceDeleted.getOffenceId();

        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, LISTED_CASE_TYPE, getDeletedListedCaseFunction(caseId, defendantId, offenceId))
                .save();

        hearingSearchSyncService.sync(hearingId);

    }

    private Function<List<ListedCase>, List<ListedCase>> getDeletedListedCaseFunction(UUID caseId, UUID defendantId, UUID offenceId) {
        return cases -> getDeletedListedCase(caseId, defendantId, offenceId, cases);
    }

    @Handles("listing.events.offence-added")
    public void offenceAdded(final Envelope<OffenceAdded> event) {
        final OffenceAdded offenceAdded = event.payload();
        final UUID hearingId = offenceAdded.getHearingId();
        final UUID caseId = offenceAdded.getCaseId();
        final UUID defendantId = offenceAdded.getDefendantId();
        final Offence offence = dedupAllReportingRestrictions(offenceAdded.getOffence());

        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, LISTED_CASE_TYPE, getAddedListedCaseFunction(caseId, defendantId, offence))
                .save();
    }

    private Function<List<ListedCase>, List<ListedCase>> getAddedListedCaseFunction(UUID caseId, UUID defendantId, Offence offence) {
        return cases -> getAddedListedCase(caseId, defendantId, offence, cases);
    }

    private List<ListedCase> getUpdatedListedCase(UUID caseId, UUID defendantId, Offence updatedOffence, List<ListedCase> listedCases) {
        ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(caseId));
        List<Defendant> defendants = listedCase.getDefendants();
        Defendant originalDefendant = Iterables.find(defendants, defendant -> defendant.getId().equals(defendantId));
        final Optional<Offence> originalOffence = originalDefendant.getOffences().stream().filter(oo -> oo.getId().equals(updatedOffence.getId())).findFirst();
        originalDefendant.getOffences().replaceAll(offence -> offence.getId().equals(updatedOffence.getId()) ? buildOffence(updatedOffence, originalOffence, getRestrictCourtList(originalOffence)) : offence);
        return listedCases;
    }

    private List<ListedCase> getAddedListedCase(UUID caseId, UUID defendantId, Offence updatedOffence, List<ListedCase> listedCases) {
        ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(caseId));
        List<Defendant> defendants = listedCase.getDefendants();
        Defendant originalDefendant = Iterables.find(defendants, defendant -> defendant.getId().equals(defendantId));

        final Optional<Offence> offence = originalDefendant.getOffences().stream().filter(offence1 ->  offence1.getId().equals(updatedOffence.getId())).findFirst();
        offence.ifPresent(o -> originalDefendant.getOffences().remove(o));
        originalDefendant.getOffences().add(updatedOffence);
        listedCases.replaceAll(
                listedCase1 -> listedCase1.getId().equals(listedCase.getId()) ? updateShadowListedFlagForListedCase(listedCase) : listedCase1);

        return listedCases;
    }

    private List<ListedCase> getDeletedListedCase(UUID caseId, UUID defendantId, UUID offenceId, List<ListedCase> listedCases) {
        ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(caseId));
        List<Defendant> defendants = listedCase.getDefendants();
        Defendant originalDefendant = Iterables.find(defendants, defendant -> defendant.getId().equals(defendantId));

        originalDefendant.getOffences().removeIf(offence -> offence.getId().equals(offenceId));
        listedCases.replaceAll(
                listedCase1 -> listedCase1.getId().equals(listedCase.getId()) ? updateShadowListedFlagForListedCase(listedCase) : listedCase1);

        return listedCases;
    }

    private Offence buildOffence(Offence updatedOffence, Optional<Offence> originalOffence, Optional<Boolean> restrictCourtList){
        return Offence.offence()
                .withStatementOfOffence(updatedOffence.getStatementOfOffence())
                .withOffenceWording(updatedOffence.getOffenceWording())
                .withEndDate(updatedOffence.getEndDate())
                .withId(updatedOffence.getId())
                .withOffenceCode(updatedOffence.getOffenceCode())
                .withStartDate(updatedOffence.getStartDate())
                .withRestrictFromCourtList(nonNull(restrictCourtList) && restrictCourtList.isPresent() ? restrictCourtList.get() : null)
                .withLaaApplnReference(updatedOffence.getLaaApplnReference())
                .withShadowListed(originalOffence.map(Offence::getShadowListed).orElse(null))
                .withListingNumber(originalOffence.map( Offence::getListingNumber).orElse(null))
                .withReportingRestrictions(dedupReportingRestrictions(updatedOffence.getReportingRestrictions()))
                .withIndictmentParticular(updatedOffence.getIndictmentParticular())
                .build();
    }

    private Optional<Boolean> getRestrictCourtList(Optional<Offence> offence) {
        if (offence.isPresent() && nonNull(offence.get().getRestrictFromCourtList())) {
            return of(offence.get().getRestrictFromCourtList());
        }
        return Optional.empty();
    }

    private ListedCase updateShadowListedFlagForListedCase(ListedCase listedCase){
        final boolean caseShadowListed = listedCase.getDefendants().stream()
                .flatMap(defendant -> defendant.getOffences().stream())
                .allMatch(offence -> nonNull(offence.getShadowListed()) && offence.getShadowListed());

        return ListedCase.listedCase()
                .withCaseIdentifier(listedCase.getCaseIdentifier())
                .withCaseStatus(listedCase.getCaseStatus())
                .withDefendants(listedCase.getDefendants())
                .withIsEjected(listedCase.getIsEjected())
                .withLinkedCases(listedCase.getLinkedCases())
                .withMarkers(listedCase.getMarkers())
                .withRestrictFromCourtList(listedCase.getRestrictFromCourtList())
                .withId(listedCase.getId())
                .withShadowListed(caseShadowListed)
                .build();
    }

}
