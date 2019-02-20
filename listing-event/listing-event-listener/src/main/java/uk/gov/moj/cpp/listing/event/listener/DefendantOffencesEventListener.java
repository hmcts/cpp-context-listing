package uk.gov.moj.cpp.listing.event.listener;

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
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.List;
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

    @Handles("listing.events.offence-updated")
    public void offenceUpdated(final Envelope<OffenceUpdated> event) {
        final OffenceUpdated offenceUpdated = event.payload();
        final UUID hearingId = offenceUpdated.getHearingId();
        final UUID caseId = offenceUpdated.getCaseId();
        final UUID defendantId = offenceUpdated.getDefendantId();
        final Offence offence = offenceUpdated.getOffence();

        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, LISTED_CASE_TYPE, getUpdatedListedCaseFunction(caseId, defendantId, offence))
                .save();
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
        final Offence offence = offenceAdded.getOffence();

        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, LISTED_CASE_TYPE, getAddedListedCaseFunction(caseId, defendantId, offence))
                .save();
    }

    private Function<List<ListedCase>, List<ListedCase>> getAddedListedCaseFunction(UUID caseId, UUID defendantId, Offence offence) {
        return cases -> getAddedListedCase(caseId, defendantId, offence, cases);
    }

    private List<ListedCase> getUpdatedListedCase(UUID caseId, UUID defendantId, Offence updatedOffence, List<ListedCase> cases) {
        List<ListedCase> listedCases = cases;
        ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(caseId));
        List<Defendant> defendants = listedCase.getDefendants();
        Defendant originalDefendant = Iterables.find(defendants, defendant -> defendant.getId().equals(defendantId));

        originalDefendant.getOffences().replaceAll(offence -> offence.getId().equals(updatedOffence.getId()) ? updatedOffence : offence);
        return listedCases;
    }

    private List<ListedCase> getAddedListedCase(UUID caseId, UUID defendantId, Offence updatedOffence, List<ListedCase> cases) {
        List<ListedCase> listedCases = cases;
        ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(caseId));
        List<Defendant> defendants = listedCase.getDefendants();
        Defendant originalDefendant = Iterables.find(defendants, defendant -> defendant.getId().equals(defendantId));

        originalDefendant.getOffences().add(updatedOffence);
        return listedCases;
    }

    private List<ListedCase> getDeletedListedCase(UUID caseId, UUID defendantId, UUID offenceId, List<ListedCase> cases) {
        List<ListedCase> listedCases = cases;
        ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(caseId));
        List<Defendant> defendants = listedCase.getDefendants();
        Defendant originalDefendant = Iterables.find(defendants, defendant -> defendant.getId().equals(defendantId));

        originalDefendant.getOffences().removeIf(offence -> offence.getId().equals(offenceId));
        return listedCases;
    }
}
