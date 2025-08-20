package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.listing.endpoint.UnscheduledHearingsEndpoint.pollForUnscheduledHearings;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.mixtureHmiEnabledAndNotHmiEnabledHearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.notHmiEnabledHearingsData;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlotsWithQueryParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;

import uk.gov.moj.cpp.listing.steps.ListUnscheduledCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateUnscheduledHearingSteps;
import uk.gov.moj.cpp.listing.steps.VacatingTrialSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.io.IOException;
import java.util.UUID;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")

public class ListUnscheduledCourtHearingIT extends AbstractIT {

    @Test
    
    public void shouldListHearingWithUnallocatedData() throws IOException {
        final HearingsData hearingsData = notHmiEnabledHearingsData();
        final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
        listCourtHearingSteps.verifyHearingUnscheduledListedFromAPI();

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());
        final UpdateUnscheduledHearingSteps updateHearingSteps = new UpdateUnscheduledHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicEventHearingConfirmed();
        updateHearingSteps.verifyHearingIsNotUnscheduledListedFromAPI();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();
    }

    @Test
    public void shouldHideUnscheduledHearingVacatedFromHearingService() {
        final HearingsData hearingsData = notHmiEnabledHearingsData();
        final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
        listCourtHearingSteps.verifyHearingUnscheduledListedFromAPI();

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublished();
        final Matcher<? super ReadContext> noHearingPresentMatcher = withJsonPath("hearings", hasSize(0));
        pollForUnscheduledHearings(getLoggedInUser(), hearingsData.getHearingData().get(0).getCourtCentreId(), noHearingPresentMatcher);
    }

    @Test
    public void shouldOnlyListHearingForNotHmiEnabledCourtCentres() {
        final HearingsData hearingsData = mixtureHmiEnabledAndNotHmiEnabledHearingsData();
        final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();

        final Matcher<? super ReadContext> oneHearingPresentMatcher = withJsonPath("hearings", hasSize(1));
        pollForUnscheduledHearings(getLoggedInUser(), hearingsData.getHearingData().get(0).getCourtCentreId(), oneHearingPresentMatcher);

        final Matcher<? super ReadContext> noHearingPresentMatcher = withJsonPath("hearings", hasSize(0));
        pollForUnscheduledHearings(getLoggedInUser(), hearingsData.getHearingData().get(1).getCourtCentreId(), noHearingPresentMatcher);
    }


    @Test
    public void shouldHideUnscheduledHearingVacatedFromListingService() {
        final HearingsData hearingsData = notHmiEnabledHearingsData();
        final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
        listCourtHearingSteps.verifyHearingUnscheduledListedFromAPI();

        new VacatingTrialSteps(hearingsData).whenHearingIsVacatedFromWithinListing();

        final Matcher<? super ReadContext> noHearingPresentMatcher = withJsonPath("hearings", hasSize(0));
        final UUID courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId();
        pollForUnscheduledHearings(getLoggedInUser(), courtCentreId, noHearingPresentMatcher);
    }
}
