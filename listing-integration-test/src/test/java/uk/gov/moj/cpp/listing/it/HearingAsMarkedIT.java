package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonassert.JsonAssert.emptyCollection;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;

import uk.gov.moj.cpp.listing.steps.HearingAsMarkedSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class HearingAsMarkedIT extends AbstractIT {

    @Test
    public void shouldRemoveHearingMarkedAsDuplicate() {
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
    public void shouldRemoveUnallocatedHearingMarkedAsDuplicate() {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        HearingData hearingData = hearingsData.getHearingData().get(0);
        final HearingAsMarkedSteps hearingAsMarkedSteps = new HearingAsMarkedSteps(hearingData);
        hearingAsMarkedSteps.whenUnallocatedHearingMarkedAsDuplicateCommandIsSent();

        pollForHearing(hearingData.getCourtCentreId().toString(), false, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings", emptyCollection())
        });
    }

    @Disabled("will be handled with DD-34779")
    @Test
    public void shouldHearingDeletedForHmi() {
        final UUID courtCentreId = randomUUID();
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, CROWN_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        listCourtHearingSteps.verifyPrivateEventRequestedHearingFromStagingHmiInActiveMQ();

        HearingData hearingData = hearingsData.getHearingData().get(0);
        final HearingAsMarkedSteps hearingAsMarkedSteps = new HearingAsMarkedSteps(hearingData);
        hearingAsMarkedSteps.whenHearingMarkedAsDuplicatePublicEventIsPublished();
        hearingAsMarkedSteps.verifyHearingMarkedAsDuplicatePublicEventInActiveMQ();

        pollForHearing(hearingData.getCourtCentreId().toString(), false, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings", emptyCollection())
        });

        hearingAsMarkedSteps.verifyHmiPublicEventForDeleteHearing();
    }

}
