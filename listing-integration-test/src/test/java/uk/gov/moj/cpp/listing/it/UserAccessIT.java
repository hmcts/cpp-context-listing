package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.setupAsUnauthorisedUser;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.Test;

public class UserAccessIT extends AbstractIT {

    @Test
    public void testUnauthorisedUserCanNotListHearing() {
        setupAsUnauthorisedUser(USER_ID_VALUE);
        HearingsData hearingsData = HearingsData.hearingsData();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingByUnauthorisedUser();
        }

    }


}
