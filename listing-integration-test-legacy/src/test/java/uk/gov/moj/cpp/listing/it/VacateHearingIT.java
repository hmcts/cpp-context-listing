package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.notHmiEnabledHearingsData;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubDeleteAvailableHearingSlotsService;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetCourtSchedulesByIdWithDraftStatus;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyDeleteAvailableHearingSlotsStubCommandInvoked;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListUnscheduledCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.VacatingTrialSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings({"squid:S1607"})
class VacateHearingIT extends AbstractIT {

    static Stream<Arguments> provideJurisdictionTypes() {
        return Stream.of(
                Arguments.of(CROWN_JURISDICTION),
                Arguments.of(MAGISTRATES_JURISDICTION)
        );
    }

    @Test
    void shouldVacateMagistrateCourtHearingAndHearingSlotsFreed_WhenInitiatedFromHearing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);

        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), courtScheduleId,
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());

        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final String hearingId = hearingsData.getHearingData().get(0).getId().toString();
        stubDeleteAvailableHearingSlotsService(hearingId);

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublished();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        verifyDeleteAvailableHearingSlotsStubCommandInvoked(hearingId);
    }

    @Test
    void shouldVacateMagistrateCourtHearingAndHearingSlotsFreed_WhenInitiatedFromListing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), courtScheduleId,
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());

        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final String hearingId = hearingsData.getHearingData().get(0).getId().toString();
        stubDeleteAvailableHearingSlotsService(hearingId);
        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenHearingIsVacatedFromWithinListing();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        verifyDeleteAvailableHearingSlotsStubCommandInvoked(hearingId);
    }

    @Test
    void shouldNotVacateMagistrateCourtHearingAndNotAttemptToFreeHearingSlotsWhenReasonIdIsEmpty() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublishedWithEmptyVacatedTrialReasonId();
        vacatingTrialSteps.verifyVacatedTrialWithEmptyReasonIdWhenQueryingFromAPI();
    }

    @Test
    void shouldVacateCrownCourtHearingAndFreeHearingSlots() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(CROWN_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final String hearingId = hearingsData.getHearingData().get(0).getId().toString();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";

        stubGetCourtSchedulesByIdWithDraftStatus(java.util.Collections.singletonList(courtScheduleId), false);
        stubListHearingInCourtSessions(hearingId, courtScheduleId,
                hearingsData.getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        stubDeleteAvailableHearingSlotsService(hearingId);
        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublished();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        verifyDeleteAvailableHearingSlotsStubCommandInvoked(hearingId);
    }

    @ParameterizedTest
    @MethodSource("provideJurisdictionTypes")
    void shouldVacateUnallocatedHearingAndNotAttemptToFreeHearingSlots(final String jurisdictionType) {
        final HearingsData hearingsData = hearingsData(jurisdictionType);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenHearingIsVacatedFromWithinListing();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
    }

    @ParameterizedTest
    @MethodSource("provideJurisdictionTypes")
    void shouldVacateUnscheduledHearingAndNotAttemptToFreeHearingSlots(final String jurisdictionType) {
        final HearingsData hearingsData = notHmiEnabledHearingsData(jurisdictionType);
        final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
        listCourtHearingSteps.verifyHearingUnscheduledListedFromAPI();

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenHearingIsVacatedFromWithinListing();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
    }

}
