package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearingByWeekCommencing;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataForYoungCourtApplicationRespondent;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataForYoungCourtApplicationSubject;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataForYoungDefendants;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataForWeekCommencingWithYoungDefendants;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAdultDefendants;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData.updatedDefendantDataWithUnder18DateOfBirth;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocation;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithWeekCommencingDate;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlotsWithQueryParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtRoom;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.WeekCommencingHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

class YouthCourtListRestrictionIT extends AbstractIT {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";

    @Test
    void shouldRestrictUnder18DefendantFromCourtListWhenHearingIsAllocated() throws IOException {
        final HearingsData hearingsData = hearingsDataForYoungDefendants();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(hearingData.getId());

        stubGetReferenceDataCourtRoom(updatedHearingDataForAllocation.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingDataForAllocation.getCourtRoomId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();

        final String defendantId = hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString();

        pollForHearing(updatedHearingDataForAllocation.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].id", equalTo(hearingData.getId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].id", equalTo(defendantId)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList", equalTo(true))
        });
    }

    @Test
    void shouldRestrictUnder18DefendantFromCourtListWhenWeekCommencingDateIsSet() throws IOException {
        final LocalDate initialWeekCommencingStart = ItClock.today();
        final HearingsData hearingsData = hearingsDataForWeekCommencingWithYoungDefendants(initialWeekCommencingStart, 1);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final String defendantId = hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString();

        final LocalDate changedWeekCommencingStart = initialWeekCommencingStart.plusWeeks(3);
        final LocalDate changedWeekCommencingEnd = changedWeekCommencingStart.plusWeeks(1).minusDays(1);
        final UpdatedHearingData updatedWeekCommencingData = updatedHearingDataWithWeekCommencingDate(
                hearingData, changedWeekCommencingStart, changedWeekCommencingEnd, 1);

        final WeekCommencingHearingSteps weekCommencingHearingSteps = new WeekCommencingHearingSteps(updatedWeekCommencingData);
        weekCommencingHearingSteps.whenHearingIsUpdatedForListingForWeekCommencingDate();

        pollForHearingByWeekCommencing(
                updatedWeekCommencingData.getCourtCentreId().toString(),
                UNALLOCATED,
                updatedWeekCommencingData.getWeekCommencingStartDate(),
                updatedWeekCommencingData.getWeekCommencingEndDate(),
                getLoggedInUser().toString(),
                new Matcher[]{
                        withJsonPath("$.hearings[0].id", equalTo(hearingData.getId().toString())),
                        withJsonPath("$.hearings[0].weekCommencingStartDate", equalTo(updatedWeekCommencingData.getWeekCommencingStartDate())),
                        withJsonPath("$.hearings[0].weekCommencingEndDate", equalTo(updatedWeekCommencingData.getWeekCommencingEndDate())),
                        withJsonPath("$.hearings[0].listedCases[0].defendants[0].id", equalTo(defendantId)),
                        withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList", equalTo(true))
                });

        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(hearingData.getId());

        stubGetReferenceDataCourtRoom(updatedHearingDataForAllocation.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingDataForAllocation.getCourtRoomId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();

        pollForHearingByWeekCommencing(
                updatedHearingDataForAllocation.getCourtCentreId().toString(),
                ALLOCATED,
                "1970-01-01",
                "2100-12-31",
                getLoggedInUser().toString(),
                new Matcher[]{
                        withJsonPath("$.hearings[0].id", equalTo(hearingData.getId().toString())),
                        withJsonPath("$.hearings[0].listedCases[0].defendants[0].id", equalTo(defendantId)),
                        withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList", equalTo(true))
                });
    }

    @Test
    void shouldRestrictUnder18CourtApplicationRespondentFromCourtListWhenHearingIsAllocated() throws IOException {
        final HearingsData hearingsData = hearingsDataForYoungCourtApplicationRespondent();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final CourtApplicationData courtApplicationData = hearingData.getCourtApplications().get(0);
        final String respondentId = courtApplicationData.getRespondent().getId().toString();

        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(hearingData.getId());
        stubGetReferenceDataCourtRoom(updatedHearingDataForAllocation.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingDataForAllocation.getCourtRoomId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();

        pollForHearing(updatedHearingDataForAllocation.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].id", equalTo(hearingData.getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].id", equalTo(courtApplicationData.getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].id", equalTo(respondentId)),
                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].restrictFromCourtList", equalTo(true))
        });
    }

    @Test
    void shouldRestrictUnder18CourtApplicationSubjectFromCourtListWhenHearingIsAllocated() throws IOException {
        final HearingsData hearingsData = hearingsDataForYoungCourtApplicationSubject();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final CourtApplicationData courtApplicationData = hearingData.getCourtApplications().get(0);
        final String subjectId = courtApplicationData.getSubject().getId().toString();

        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(hearingData.getId());
        stubGetReferenceDataCourtRoom(updatedHearingDataForAllocation.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingDataForAllocation.getCourtRoomId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();

        pollForHearing(updatedHearingDataForAllocation.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].id", equalTo(hearingData.getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].id", equalTo(courtApplicationData.getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].subject.id", equalTo(subjectId)),
                withJsonPath("$.hearings[0].courtApplications[0].subject.restrictFromCourtList", equalTo(true))
        });
    }

    @Test
    void shouldRestrictDefendantFromCourtListWhenAgeUpdatedToUnder18AndHearingIsAllocated() throws IOException {
        final HearingsData hearingsData = hearingsDataWithAdultDefendants();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final DefendantData defendantData = hearingData.getListedCases().get(0).getDefendants().get(0);
        final String defendantId = defendantData.getDefendantId().toString();
        final UUID caseId = hearingData.getListedCases().get(0).getCaseId();

        pollForHearing(hearingData.getCourtCentreId().toString(), UNALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].id", equalTo(hearingData.getId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].id", equalTo(defendantId)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList", equalTo(false))
        });

        final UpdatedDefendantData updatedDefendantData = updatedDefendantDataWithUnder18DateOfBirth(defendantData);
        final UpdateDefendantSteps updateDefendantSteps = new UpdateDefendantSteps(caseId, hearingData, updatedDefendantData);
        updateDefendantSteps.whenPublicEventProgressionCaseDefendantsUpdatedIsPublished();
        updateDefendantSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED, true);

        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(hearingData.getId());

        stubGetReferenceDataCourtRoom(updatedHearingDataForAllocation.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingDataForAllocation.getCourtRoomId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();

        pollForHearing(updatedHearingDataForAllocation.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].id", equalTo(hearingData.getId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].id", equalTo(defendantId)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList", equalTo(true))
        });
    }
}
