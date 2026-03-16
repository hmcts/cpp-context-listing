package uk.gov.moj.cpp.listing.it;

import static java.time.LocalDate.now;
import static java.time.LocalTime.of;
import static java.time.ZonedDateTime.of;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;

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
import uk.gov.moj.cpp.listing.steps.data.HearingsData;


import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Integration test that verifies court scheduler slot management when a case is ejected
 * from a magistrate hearing. This test:
 * 1. Creates an allocated MAGISTRATE hearing with 1 case and 1 linked court application
 * 2. Ejects the case
 * 3. Verifies that HearingSlotsService.delete() is called at least twice
 */
class EjectCaseWithMagistrateCourtSlotsManagementIT extends AbstractIT {

    private static final String COURT_SCHEDULER_ENDPOINT = "/listingcourtscheduler-api/rest/courtscheduler";
    private static final String HEARING_SLOTS = "/hearingslots";

    @Test
    void shouldNotCallHearingSlotsServiceDeleteIfHearingStillHasCasesThatAreNotEjected() {
        // Given: Create hearing with 2 cases (using the simpler approach)
        final TestHearingData testData = createTestHearingWithTwoCases();
        
        // When: Eject only one case (leaving one case in the hearing)
        final EjectCaseApplicationSteps ejectSteps = new EjectCaseApplicationSteps(testData.hearingsData);
        ejectSteps.verifyListedCasesInHearings(true, 2);
        ejectSteps.buildEjectCaseData();
        ejectSteps.verifyNoHearingsReturned(true);

        // Then: Verify that HearingSlotsService.delete() is NOT called (0 times)
        verifyDeleteAvailableHearingSlotsStubCommandIsNeverInvoked(testData.hearingsData.getHearingData().get(0).getId().toString());
    }
    
    @Test
    void shouldCallHearingSlotsServiceDeleteOnceForMagistrateHearingWithLinkedApplication() {
        // Given: Create hearing with 1 case (using CaseAndDefendantData approach)
        final TestHearingData testData = createTestHearingWithOneCase();
        
        // When: Eject the only case (leaving no cases in the hearing)
        final EjectCaseApplicationSteps ejectSteps = new EjectCaseApplicationSteps(testData.hearingsData);
        ejectSteps.verifyListedCasesInHearings(true, 1);
        ejectSteps.buildEjectCaseData();
        ejectSteps.verifyNoHearingsReturned(true);

        // Then: Verify that HearingSlotsService.delete() is called exactly once
        verifyDeleteAvailableHearingSlotsStubCommandInvoked(testData.hearingsData.getHearingData().get(0).getId().toString());
    }

    
    private TestHearingData createTestHearingWithTwoCases() {
        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = getRandomCourtCenterId();
        final UUID courtRoomUUID = getRandomCourtRoomId();

        // Create future dates for the hearing
        final LocalDate futureHearingDate = now().plusDays(7); // 7 days in the future
        final ZonedDateTime futureHearingStartTime = of(futureHearingDate, of(10, 0), UTC); // 10:00 AM UTC
        final LocalDate futureHearingEndDate = futureHearingDate.plusDays(1); // End date is next day

        // Create hearing data using factory with allocated magistrate hearing and future dates (simpler approach)
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryWithCourtCenterForMagistrate(
            courtCentreId, courtRoomUUID, futureHearingEndDate, futureHearingStartTime);

        // Setup stubs and create hearing
        setupStubsAndCreateHearing(hearingsData, courtCentreId, futureHearingDate, futureHearingStartTime);

        return new TestHearingData(hearingsData);
    }

    private TestHearingData createTestHearingWithOneCase() {
        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = getRandomCourtCenterId();
        final UUID courtRoomUUID = getRandomCourtRoomId();

        // Create future dates for the hearing
        final LocalDate futureHearingDate = now().plusDays(7); // 7 days in the future
        final ZonedDateTime futureHearingStartTime = of(futureHearingDate, of(10, 0), UTC); // 10:00 AM UTC
        final LocalDate futureHearingEndDate = futureHearingDate.plusDays(1); // End date is next day
        
        // Create CaseAndDefendantData for single case hearing
        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(
            randomUUID(),
            "test-case-urn",
            "test-case-urn",
            randomUUID(),
            "test-search",
            "MAGISTRATES",
            "MAGISTRATES",
            "test-linked-case-urn",
            "test-linked-case-urn"
        );
        
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(
            caseAndDefendantData, courtCentreId, courtRoomUUID, futureHearingEndDate, futureHearingStartTime);

        // Setup stubs and create hearing
        setupStubsAndCreateHearing(hearingsData, courtCentreId, futureHearingDate, futureHearingStartTime);

        return new TestHearingData(hearingsData);
    }

    private void setupStubsAndCreateHearing(final HearingsData hearingsData, final UUID courtCentreId, 
                                          final LocalDate futureHearingDate, final ZonedDateTime futureHearingStartTime) {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";

        // Use the future dates for stub parameters
        final UUID courtroomId = hearingsData.getHearingData().get(0).getCourtRoomId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", futureHearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", futureHearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);

        stubListHearingInCourtSessions(hearingsData.getHearingData().get(0).getId().toString(), courtScheduleId, futureHearingStartTime);

        // Stub the delete hearing slots endpoint to return 202 No Content
        stubDeleteAvailableHearingSlotsService(hearingsData.getHearingData().get(0).getId().toString());

        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    private record TestHearingData(HearingsData hearingsData) {
    }
}
