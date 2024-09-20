package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.moj.cpp.listing.it.util.HearingHelper.getHearingById;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.RemoveOffencesFromHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class RemoveOffencesFromHearingIT extends AbstractIT{

    @Test
    public void shouldCallHmiWhenOffencesRemovedFromAllocatedHearing() {
        final HearingsData firstHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);


        final String existedHearingId = firstHearings.getHearingData().get(0).getId().toString();
        final List<String> offences = firstHearings.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().stream()
                .limit(1).map(offenceData -> offenceData.getOffenceId().toString()).collect(Collectors.toList());

        getHearingById(USER_ID_VALUE, UUID.fromString(existedHearingId), withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(3)));

        final RemoveOffencesFromHearingSteps removeOffencesFromHearingSteps = new RemoveOffencesFromHearingSteps();
        removeOffencesFromHearingSteps.whenRaisedOffencesRemovedPublicEvent(existedHearingId, offences);
        removeOffencesFromHearingSteps.verifyPublicListingOffencesRemovedFromAllocatedHearing();

        getHearingById(USER_ID_VALUE, UUID.fromString(existedHearingId), withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(2)));

    }
}
