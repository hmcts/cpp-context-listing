package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonassert.JsonAssert.emptyCollection;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;

import uk.gov.moj.cpp.listing.steps.HearingAsMarkedSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class HearingAsMarkedIT extends AbstractIT {

    @Test
    void shouldRemoveHearingMarkedAsDuplicate() {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        HearingData hearingData = hearingsData.getHearingData().get(0);
        final HearingAsMarkedSteps hearingAsMarkedSteps = new HearingAsMarkedSteps(hearingData);
        hearingAsMarkedSteps.whenHearingMarkedAsDuplicatePublicEventIsPublished();

        pollForHearing(hearingData.getCourtCentreId().toString(), false, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings", emptyCollection())
       });
    }

    @Test
    void shouldRemoveUnallocatedHearingMarkedAsDuplicate() {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        HearingData hearingData = hearingsData.getHearingData().get(0);
        final HearingAsMarkedSteps hearingAsMarkedSteps = new HearingAsMarkedSteps(hearingData);
        hearingAsMarkedSteps.whenUnallocatedHearingMarkedAsDuplicateCommandIsSent();
//       TODO: uncomment when jms works
//        hearingAsMarkedSteps.verifyHearingMarkedAsDuplicatePublicEventInActiveMQ();

        pollForHearing(hearingData.getCourtCentreId().toString(), false, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings", emptyCollection())
        });
    }
}
