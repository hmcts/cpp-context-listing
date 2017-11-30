package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.givenAUserHasLoggedInAsAListingOfficers;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenAllocatedHearingsForACourtCentreShouldContainUpdatedHearingData;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenQueryValidationFailureOccursWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainTwoExpectedHearingsWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutCourtRoomId;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutJudgeId;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutCourtRoomAndStartTime;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.whenCaseIsSubmittedForListing;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.whenHearingIsUpdatedForListing;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithEnoughDataToBeAllocated;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithoutCourtRoomId;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithoutCourtRoomIdAndStartTime;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithoutJudgeId;
import static uk.gov.moj.cpp.listing.steps.data.factory.CaseDataFactory.caseData;
import static uk.gov.moj.cpp.listing.steps.data.factory.CaseDataFactory.caseDataExisting;

import uk.gov.moj.cpp.listing.steps.data.CaseData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import javax.jms.JMSException;

import org.junit.Test;


public class HearingIT extends AbstractIT {

    @Test
    public void shouldReturnMultipleHearingsScheduled() throws JMSException {
        final CaseData caseData = caseData();
        final CaseData caseDataNew = caseDataExisting(
                caseData.getCaseId().toString(),
                caseData.getHearingData().get(0).getCourtCentreId().toString()
        );

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        // Receive a caseForListing first time in to Listing
        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        // Receive a caseForListing that was received before
        whenCaseIsSubmittedForListing(caseDataNew);
        thenUnallocatedHearingsForACourtCentreShouldContainTwoExpectedHearingsWhenQueried(caseData, caseDataNew);
    }

    @Test
    public void shouldFailQueryValidationForSearchForUnallocatedHearings() throws
            JMSException {

        final CaseData caseData = caseData();

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        thenQueryValidationFailureOccursWhenQueried(caseData);
    }

    @Test
    public void shouldUpdateAnExistingHearingWithoutCourtRoomId() throws JMSException {
        final CaseData caseData = caseData();
        final UpdatedHearingData hearingDataWithoutJudgeData = updatedHearingDataWithoutJudgeId(caseData.getHearingData().get(0).getId());
        final UpdatedHearingData hearingDataWithoutCourtRoomData = updatedHearingDataWithoutCourtRoomId(caseData.getHearingData().get(0).getId());

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        whenHearingIsUpdatedForListing(hearingDataWithoutJudgeData);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutJudgeId(caseData, hearingDataWithoutJudgeData);

        whenHearingIsUpdatedForListing(hearingDataWithoutCourtRoomData);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutCourtRoomId(caseData, hearingDataWithoutCourtRoomData);
    }

    @Test
    public void shouldUpdateAnExistingHearingWithoutJudgeId() throws JMSException {
        final CaseData caseData = caseData();
        final UpdatedHearingData hearingDataWithoutJudgeData = updatedHearingDataWithoutJudgeId(caseData.getHearingData().get(0).getId());
        final UpdatedHearingData hearingDataWithoutCourtRoomData = updatedHearingDataWithoutCourtRoomId(caseData.getHearingData().get(0).getId());

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        whenHearingIsUpdatedForListing(hearingDataWithoutCourtRoomData);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutCourtRoomId(caseData, hearingDataWithoutCourtRoomData);

        whenHearingIsUpdatedForListing(hearingDataWithoutJudgeData);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutJudgeId(caseData, hearingDataWithoutJudgeData);
    }

    @Test
    public void shouldUpdateAnExistingHearingWithoutCourtRoomAndStartTime() throws JMSException {
        final CaseData caseData = caseData();
        final UpdatedHearingData hearingDataWithoutJudgeData = updatedHearingDataWithoutJudgeId(caseData.getHearingData().get(0).getId());
        final UpdatedHearingData updatedHearingDataWithoutCourtRoomAndStartTime = updatedHearingDataWithoutCourtRoomIdAndStartTime(caseData.getHearingData().get(0).getId());

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        whenHearingIsUpdatedForListing(hearingDataWithoutJudgeData);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutJudgeId(caseData, hearingDataWithoutJudgeData);

        whenHearingIsUpdatedForListing(updatedHearingDataWithoutCourtRoomAndStartTime);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutCourtRoomAndStartTime(caseData, updatedHearingDataWithoutCourtRoomAndStartTime);

    }

    @Test
    public void shouldUpdateAnExistingHearingToBeAllocated() throws JMSException {
        final CaseData caseData = caseData();
        final UpdatedHearingData updatedHearingData = updatedHearingDataWithEnoughDataToBeAllocated(caseData.getHearingData().get(0).getId());

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        whenHearingIsUpdatedForListing(updatedHearingData);
        thenAllocatedHearingsForACourtCentreShouldContainUpdatedHearingData(caseData, updatedHearingData);
    }

}
