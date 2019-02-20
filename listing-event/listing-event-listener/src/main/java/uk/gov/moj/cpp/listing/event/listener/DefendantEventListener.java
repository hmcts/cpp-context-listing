package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.NewBaseDefendant;
import uk.gov.justice.listing.events.NewDefendantDetailsUpdated;
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
public class DefendantEventListener {


    private static final String LISTED_CASES_FIELD = "listedCases";

    @Inject
    private HearingRepository hearingRepository;

    @Handles("listing.events.new-defendant-details-updated")
    public void defendantDetailsUpdated(final Envelope<NewDefendantDetailsUpdated> event) {
        final NewDefendantDetailsUpdated defendantDetailsUpdated = event.payload();
        final UUID hearingId = defendantDetailsUpdated.getHearingId();
        final UUID caseId = defendantDetailsUpdated.getCaseId();
        final NewBaseDefendant defendant = defendantDetailsUpdated.getDefendant();

        TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {};

        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, typeRef, getUpdatedListedCaseFunction(caseId, defendant))
                .save();
    }

    private Function<List<ListedCase>, List<ListedCase>> getUpdatedListedCaseFunction(UUID caseId, NewBaseDefendant defendant) {
        return cases -> getUpdatedListedCase(caseId, defendant, cases);
    }

    private List<ListedCase> getUpdatedListedCase(UUID caseId, NewBaseDefendant updatedDefendant, List<ListedCase> cases) {
        List<ListedCase> listedCases = cases;
        ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(caseId));
        List<Defendant> defendants = listedCase.getDefendants();
        Defendant originalDefendant = Iterables.find(defendants, defendant1 -> defendant1.getId().equals(updatedDefendant.getId()));
        Defendant newDefendant = Defendant.defendant()
                .withBailStatus(updatedDefendant.getBailStatus())
                .withCustodyTimeLimit(updatedDefendant.getCustodyTimeLimit())
                .withId(updatedDefendant.getId())
                .withOffences(originalDefendant.getOffences())
                .withDateOfBirth(updatedDefendant.getDateOfBirth())
                .withDefenceOrganisation(updatedDefendant.getDefenceOrganisation())
                .withFirstName(updatedDefendant.getFirstName())
                .withLastName(updatedDefendant.getLastName())
                .withOrganisationName(updatedDefendant.getOrganisationName())
                .withSpecificRequirements(updatedDefendant.getSpecificRequirements())
                .withDatesToAvoid(originalDefendant.getDatesToAvoid())
                .withHearingLanguageNeeds(originalDefendant.getHearingLanguageNeeds())
                .build();

        defendants.replaceAll(defendant -> defendant.getId().equals(newDefendant.getId()) ? newDefendant : defendant);
        return listedCases;
    }
}
