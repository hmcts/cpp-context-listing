package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.givenAUserHasLoggedInAsAListingOfficers;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenQueryValidationFailureOccursWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainTwoExpectedHearingsWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingData;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.whenCaseIsSubmittedForListing;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.whenHearingIsUpdatedForListing;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingData;
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
                caseData.getCaseProgressionId().toString(),
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
    public void shouldUpdateAnExistingHearing() throws JMSException {
        final CaseData caseData = caseData();
        final UpdatedHearingData updatedHearingData = updatedHearingData(caseData.getHearingData().get(0).getId());

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        whenHearingIsUpdatedForListing(updatedHearingData);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingData(caseData, updatedHearingData);
    }

}
