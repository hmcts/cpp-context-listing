package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.ReferenceDataDefinitions.*;
import static uk.gov.moj.cpp.listing.steps.data.factory.ReferenceDataFactory.courtRoomData;
import static uk.gov.moj.cpp.listing.steps.data.factory.ReferenceDataFactory.judgeData;
import static uk.gov.moj.cpp.listing.steps.data.factory.ReferenceDataFactory.courtCentreData;

import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.CourtRoomData;
import uk.gov.moj.cpp.listing.steps.data.JudgeData;

import org.junit.Test;


public class ReferenceDataIT extends AbstractIT {

    @Test
    public void shouldAddAndRetrieveJudge() {
        final JudgeData judge = judgeData();

        givenAUserHasLoggedInAsACourtClerk(USER_ID_VALUE);
        whenJudgeHasBeenAddedItCanRetrieved(judge);

    }

    @Test
    public void shouldAddAndRetrieveCourtCentre() {
        final CourtCentreData courtCentre = courtCentreData();

        givenAUserHasLoggedInAsACourtClerk(USER_ID_VALUE);
        whenCourtCentreHasBeenAddedAndItCanBeRetrieved(courtCentre);

    }

    @Test
    public void shouldAddAndRetrieveCourtRoom() {
        final CourtRoomData courtRoom = courtRoomData();

        givenAUserHasLoggedInAsACourtClerk(USER_ID_VALUE);
        whenCourtRoomHasBeenAddedAndCanBeRetrieved(courtRoom);

    }


}

