package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.listing.endpoint.UnscheduledHearingsEndpoint.pollForUnscheduledHearings;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.mixtureHmiEnabledAndNotHmiEnabledHearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.notHmiEnabledHearingsData;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListUnscheduledCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateUnscheduledHearingSteps;
import uk.gov.moj.cpp.listing.steps.VacatingTrialSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.util.UUID;

import com.jayway.jsonpath.ReadContext;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("squid:S1607")
@RunWith(DataProviderRunner.class)
public class ListUnscheduledCourtHearingIT extends AbstractIT {

    @DataProvider
    public static Object[][] provideJurisdictionTypes() {
        return new Object[][]{
                {CROWN_JURISDICTION},
                {MAGISTRATES_JURISDICTION}
        };
    }

    private static final String CONTEXT_NAME = "listing";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @Before
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");
    }

    @Test
    public void shouldListHearingWithUnallocatedData() {
        final HearingsData hearingsData = notHmiEnabledHearingsData();
        try (final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingUnscheduledListedFromAPI();
        }

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());
        try (final UpdateUnscheduledHearingSteps updateHearingSteps = new UpdateUnscheduledHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedResultsInAllocationInMQ();
            updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
            updateHearingSteps.verifyHearingConfirmedInPublicMQ();
            updateHearingSteps.verifyHearingIsNotUnscheduledListedFromAPI();
            updateHearingSteps.verifyPublicHearingChangesSaved();
        }
    }

    @Test
    public void shouldHideUnscheduledHearingVacatedFromHearingService() {
        final HearingsData hearingsData = notHmiEnabledHearingsData();
        try (final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingUnscheduledListedFromAPI();
        }

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublished();
        final Matcher<? super ReadContext> noHearingPresentMatcher = withJsonPath("hearings", hasSize(0));
        pollForUnscheduledHearings(getLoggedInUser(), hearingsData.getHearingData().get(0).getCourtCentreId(), noHearingPresentMatcher);
    }

    @Test
    public void shouldNotListHearingWhenAllHmiEnabledCourtCentres() {
        final HearingsData hearingsData = hearingsData();
        try (final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
        }

        final Matcher<? super ReadContext> noHearingPresentMatcher = withJsonPath("hearings", hasSize(0));
        pollForUnscheduledHearings(getLoggedInUser(), hearingsData.getHearingData().get(0).getCourtCentreId(), noHearingPresentMatcher);
    }

    @Test
    public void shouldOnlyListHearingForNotHmiEnabledCourtCentres() {
        final HearingsData hearingsData = mixtureHmiEnabledAndNotHmiEnabledHearingsData();
        try (final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
        }

        final Matcher<? super ReadContext> oneHearingPresentMatcher = withJsonPath("hearings", hasSize(1));
        pollForUnscheduledHearings(getLoggedInUser(), hearingsData.getHearingData().get(0).getCourtCentreId(), oneHearingPresentMatcher);

        final Matcher<? super ReadContext> noHearingPresentMatcher = withJsonPath("hearings", hasSize(0));
        pollForUnscheduledHearings(getLoggedInUser(), hearingsData.getHearingData().get(1).getCourtCentreId(), noHearingPresentMatcher);
    }


    @Test
    public void shouldHideUnscheduledHearingVacatedFromListingService() {
        final HearingsData hearingsData = notHmiEnabledHearingsData();
        try (final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingUnscheduledListedFromAPI();
        }

        new VacatingTrialSteps(hearingsData).whenHearingIsVacated();

        final Matcher<? super ReadContext> noHearingPresentMatcher = withJsonPath("hearings", hasSize(0));
        final UUID courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId();
        pollForUnscheduledHearings(getLoggedInUser(), courtCentreId, noHearingPresentMatcher);
    }
}
