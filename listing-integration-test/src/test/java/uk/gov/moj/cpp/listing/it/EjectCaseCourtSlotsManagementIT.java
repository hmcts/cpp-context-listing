package uk.gov.moj.cpp.listing.it;

import static java.time.LocalDate.now;
import static java.time.LocalTime.of;
import static java.time.ZonedDateTime.of;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithCourtCenterForMagistrate;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubDeleteAvailableHearingSlotsService;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubUpdateAvailableHearingSlotsService;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyDeleteAvailableHearingSlotsStubCommandInvoked;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyDeleteAvailableHearingSlotsStubCommandIsNeverInvoked;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;

import uk.gov.moj.cpp.listing.steps.EjectCaseApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Integration tests verifying court scheduler slot management when cases are ejected
 * from allocated hearings across jurisdictions.
 */
class EjectCaseCourtSlotsManagementIT extends AbstractIT {

    @Test
    void shouldNotCallHearingSlotsServiceDeleteIfHearingStillHasCasesThatAreNotEjected() {
        final TestHearingData testData = createTestHearingWithTwoCases();

        final EjectCaseApplicationSteps ejectSteps = new EjectCaseApplicationSteps(testData.hearingsData);
        ejectSteps.verifyListedCasesInHearings(true, 2);
        ejectSteps.buildEjectCaseData();
        ejectSteps.verifyNoHearingsReturned(true);

        verifyDeleteAvailableHearingSlotsStubCommandIsNeverInvoked(testData.hearingsData.getHearingData().get(0).getId().toString());
    }

    @Test
    void shouldCallHearingSlotsServiceDeleteOnceForMagistrateHearingWithLinkedApplication() {
        final TestHearingData testData = createTestHearingWithOneCaseForJurisdiction("MAGISTRATES");

        final EjectCaseApplicationSteps ejectSteps = new EjectCaseApplicationSteps(testData.hearingsData);
        ejectSteps.verifyListedCasesInHearings(true, 1);
        ejectSteps.buildEjectCaseData();
        ejectSteps.verifyNoHearingsReturned(true);

        verifyDeleteAvailableHearingSlotsStubCommandInvoked(testData.hearingsData.getHearingData().get(0).getId().toString());
    }

    @Test
    void shouldCallHearingSlotsServiceDeleteOnceForCrownHearingWithLinkedApplication() {
        final TestHearingData testData = createTestHearingWithOneCaseForJurisdiction("CROWN");

        final EjectCaseApplicationSteps ejectSteps = new EjectCaseApplicationSteps(testData.hearingsData);
        ejectSteps.verifyListedCasesInHearings(true, 1);
        ejectSteps.buildEjectCaseData();
        ejectSteps.verifyNoHearingsReturned(true);

        verifyDeleteAvailableHearingSlotsStubCommandInvoked(testData.hearingsData.getHearingData().get(0).getId().toString());
    }

    private TestHearingData createTestHearingWithTwoCases() {
        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = getRandomCourtCenterId();
        final UUID courtRoomUUID = getRandomCourtRoomId();

        final LocalDate futureHearingDate = now().plusDays(7);
        final ZonedDateTime futureHearingStartTime = of(futureHearingDate, of(10, 0), UTC);
        final LocalDate futureHearingEndDate = futureHearingDate.plusDays(1);

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryWithCourtCenterForMagistrate(
                courtCentreId, courtRoomUUID, futureHearingEndDate, futureHearingStartTime);

        setupStubsAndCreateHearing(hearingsData, courtCentreId, futureHearingDate, futureHearingStartTime);
        return new TestHearingData(hearingsData);
    }

    private TestHearingData createTestHearingWithOneCaseForJurisdiction(final String jurisdictionType) {
        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = getRandomCourtCenterId();
        final UUID courtRoomUUID = getRandomCourtRoomId();

        final LocalDate futureHearingDate = now().plusDays(7);
        final ZonedDateTime futureHearingStartTime = of(futureHearingDate, of(10, 0), UTC);
        final LocalDate futureHearingEndDate = futureHearingDate.plusDays(1);

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(
                randomUUID(),
                "test-case-urn",
                "test-case-urn",
                randomUUID(),
                "test-search",
                jurisdictionType,
                jurisdictionType,
                "test-linked-case-urn",
                "test-linked-case-urn"
        );

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(
                caseAndDefendantData, courtCentreId, courtRoomUUID, futureHearingEndDate, futureHearingStartTime);

        setupStubsAndCreateHearing(hearingsData, courtCentreId, futureHearingDate, futureHearingStartTime);
        return new TestHearingData(hearingsData);
    }

    private void setupStubsAndCreateHearing(final HearingsData hearingsData, final UUID courtCentreId,
                                            final LocalDate futureHearingDate, final ZonedDateTime futureHearingStartTime) {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";

        final UUID courtroomId = hearingsData.getHearingData().get(0).getCourtRoomId();

        final Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", futureHearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", futureHearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);

        stubListHearingInCourtSessions(hearingsData.getHearingData().get(0).getId().toString(), courtScheduleId, futureHearingStartTime);
        stubDeleteAvailableHearingSlotsService(hearingsData.getHearingData().get(0).getId().toString());

        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    private record TestHearingData(HearingsData hearingsData) {
    }
}
