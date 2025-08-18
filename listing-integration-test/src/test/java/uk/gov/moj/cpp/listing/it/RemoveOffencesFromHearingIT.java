package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.listing.it.util.HearingHelper.pollForHearingById;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.singleHearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData.updateOffenceData;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.RemoveOffencesFromHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantOffencesSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class RemoveOffencesFromHearingIT extends AbstractIT{

    @Test
    void shouldCallHmiWhenOffencesRemovedFromAllocatedHearing() {
        final HearingsData firstHearings = hearingsDataWithAllocationDataAndJudiciary();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);


        final String existedHearingId = firstHearings.getHearingData().get(0).getId().toString();
        final List<String> offences = firstHearings.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().stream()
                .limit(1).map(offenceData -> offenceData.getOffenceId().toString()).collect(Collectors.toList());

        pollForHearingById(USER_ID_VALUE, UUID.fromString(existedHearingId), withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(3)));

        final RemoveOffencesFromHearingSteps removeOffencesFromHearingSteps = new RemoveOffencesFromHearingSteps();
        removeOffencesFromHearingSteps.whenRaisedOffencesRemovedPublicEvent(existedHearingId, offences);
        removeOffencesFromHearingSteps.verifyPublicListingOffencesRemovedFromAllocatedHearing();

        pollForHearingById(USER_ID_VALUE, UUID.fromString(existedHearingId), withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(2)));

    }

    @Test
    void shouldNotAllowAddOffenceOnceCaseRemovedFromHearing() {
        final HearingsData firstHearing = singleHearingsDataWithAllocationDataAndJudiciary();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearing);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final String existedHearingId = firstHearing.getHearingData().get(0).getId().toString();
        final List<String> offences = firstHearing.getHearingData().get(0).getListedCases().get(0).getDefendants().stream().flatMap(df -> df.getOffences().stream())
                .map(offenceData -> offenceData.getOffenceId().toString()).collect(Collectors.toList());

        pollForHearingById(USER_ID_VALUE, UUID.fromString(existedHearingId), withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(2)));

        //Remove all offences
        final RemoveOffencesFromHearingSteps removeOffencesFromHearingSteps = new RemoveOffencesFromHearingSteps();
        removeOffencesFromHearingSteps.whenRaisedOffencesRemovedPublicEvent(existedHearingId, offences);
        removeOffencesFromHearingSteps.verifyPublicListingOffencesRemovedFromAllocatedHearing();

        pollForHearingById(USER_ID_VALUE, UUID.fromString(existedHearingId), withJsonPath("$.listedCases.length()", equalTo(0)));

        //Add offence again to hearing
        DefendantData defendantData = firstHearing.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = firstHearing.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = firstHearing.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, null);
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedAddedOnly();

        var thrown = assertThrows(
                NoSuchElementException.class,
                steps::verifyEventOffenceAddedInActiveMQ,
                "Expected steps.verifyEventOffenceAddedInActiveMQ() to throw, but it didn't"
        );

        assertTrue(thrown.getMessage().contains("No value present"));
    }
}
