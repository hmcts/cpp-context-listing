package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.givenAUserHasLoggedInAsAListingOfficers;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenQueryValidationFailureOccursWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsAreReturnedWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.whenCaseIsSubmittedForListing;
import static uk.gov.moj.cpp.listing.steps.data.factory.CaseDataFactory.caseData;

import uk.gov.moj.cpp.listing.steps.data.CaseData;

import javax.jms.JMSException;

import org.junit.Test;


public class HearingIT extends AbstractIT {


    @Test
    public void shouldSearchForAndReturnUnallocatedHearings() throws JMSException {
        final String hearingId = randomUUID().toString();

        final CaseData caseData = caseData();

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        whenCaseIsSubmittedForListing(hearingId, caseData);

        thenUnallocatedHearingsAreReturnedWhenQueried(caseData);
    }

    @Test
    public void shouldFailQueryValidationForSearchForUnallocatedHearings() throws
            JMSException {

        final CaseData caseData = caseData();

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        thenQueryValidationFailureOccursWhenQueried(caseData);
    }



}
