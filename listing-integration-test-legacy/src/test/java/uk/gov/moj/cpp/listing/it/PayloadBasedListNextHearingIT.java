package uk.gov.moj.cpp.listing.it;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.moj.cpp.listing.steps.PayloadBasedListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.PayloadBasedListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.PayloadBasedUpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.PayloadGenerator;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.listing.endpoint.UnscheduledHearingsEndpoint.pollForUnscheduledHearings;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;

/**
 * Example integration test demonstrating the new payload-based approach
 * using JSON files from test-data folder instead of nested classes
 */
class PayloadBasedListNextHearingIT extends AbstractIT {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadBasedListNextHearingIT.class);
    
    @Test
     void shouldListNextHearingsUsingAdhocHearingCreation() {
        // First, create an initial hearing using adhoc hearing creation scenario
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues firstHearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithAdhocHearingCreation();
        
        // Verify the first hearing was listed
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);
        
        // Now create next hearings using adjournment crown fixed date scenario
        PayloadBasedListNextHearingSteps listNextHearingSteps = new PayloadBasedListNextHearingSteps(firstHearingValues.hearingId);
        PayloadGenerator.PayloadValues nextHearingValues = listNextHearingSteps.whenListNextHearingSubmittedWithAdjournmentCrownFixedDate();
        
        // Verify the next hearing was listed
        listNextHearingSteps.verifyNextHearingListedFromAPI(AbstractIT.ALLOCATED,2);

        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully with first hearing: {} and next hearing: {}", 
                   firstHearingValues.hearingId, nextHearingValues.hearingId);
    }
    
    @Test
    void shouldUpdateHearingFromUnallocatedToAllocated() {
        // First, create an unallocated hearing using SPI unallocated scenario
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues hearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiUnallocated();
        
        // Verify the hearing was listed as unallocated
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.UNALLOCATED);
        
        // Now update the hearing from unallocated to allocated
        PayloadBasedUpdateHearingSteps updateHearingSteps = new PayloadBasedUpdateHearingSteps(hearingValues.hearingId, hearingValues);
        PayloadGenerator.PayloadValues updatedValues = updateHearingSteps.whenUpdateHearingSubmittedWithUnallocatedToAllocated();
        
        // Verify the hearing was updated to allocated
        updateHearingSteps.verifyHearingUpdatedFromAPI(AbstractIT.ALLOCATED);
        updateHearingSteps.verifyHearingAllocationStatusChanged(true);
        
        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully - hearing {} updated from unallocated to allocated", 
                   hearingValues.hearingId);
    }
    
    @Test
    void shouldAssignJudiciaryToHearing() {
        // First, create an allocated hearing using SPI allocated scenario
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues hearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiAllocated();
        
        // Verify the hearing was listed as allocated
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);
        
        // Now assign judiciary to the hearing
        PayloadBasedUpdateHearingSteps updateHearingSteps = new PayloadBasedUpdateHearingSteps(hearingValues.hearingId, hearingValues);

        PayloadGenerator.PayloadValues updatedValues = updateHearingSteps.whenUpdateHearingSubmittedWithAssignJudiciary();
        
        // Verify judiciary was assigned
        updateHearingSteps.verifyJudiciaryAssignedToHearing();
        updateHearingSteps.verifyHearingUpdatedFromAPI(AbstractIT.ALLOCATED);
        
        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully - judiciary assigned to hearing {}", 
                   hearingValues.hearingId);
    }
    
    @Test
    void shouldCreateNextHearingForMagistratesJurisdiction() {
        // First, create an initial hearing using MCC without court schedule allocated scenario
        
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues firstHearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithMccWithoutCourtScheduleAllocated();
        
        // Verify the first hearing was listed
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);
        
        // Now create next hearings for magistrates using adjournment mags scenario
        PayloadBasedListNextHearingSteps listNextHearingSteps = new PayloadBasedListNextHearingSteps(firstHearingValues.hearingId,firstHearingValues);

        stubListHearingInCourtSessions(firstHearingValues.hearingId,
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ItClock.nowUtc()
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        final ZonedDateTime hearingStartTime = listNextHearingSteps.getPayloadValues().hearingStartTime;
        final LocalDate hearingDate = LocalDate.parse(listNextHearingSteps.getPayloadValues().hearingDate);
        final String courtCentreId = listNextHearingSteps.getPayloadValues().courtCentreId;
        final String courtroomId = listNextHearingSteps.getPayloadValues().courtRoomId;

        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final String bookingId = "20576fdd-4415-4bac-8948-07aa3b8d9b08";

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId);
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId);
        stubParams.put("BOOKING_ID", bookingId);
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);

        PayloadGenerator.PayloadValues nextHearingValues = listNextHearingSteps.whenListNextHearingSubmittedWithAdjournmentMagistrates();
        
        // Verify the next hearing was listed
        listNextHearingSteps.verifyNextHearingListedFromAPI(AbstractIT.ALLOCATED,2);
        
        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully with magistrates hearing: {} and next hearing: {}", 
                   firstHearingValues.hearingId, nextHearingValues.hearingId);
    }
    
    @Test
    
    void shouldCreateUnscheduledNextHearing() {
        // First, create an initial hearing using SPI two defendants unallocated scenario
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues firstHearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiTwoDefendantsUnallocated();
        
        // Verify the first hearing was listed
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.UNALLOCATED);
        
        // Now create unscheduled next hearings using adjournment crown unscheduled scenario
        PayloadBasedListNextHearingSteps listNextHearingSteps = new PayloadBasedListNextHearingSteps(firstHearingValues.hearingId,firstHearingValues);
        PayloadGenerator.PayloadValues nextHearingValues = listNextHearingSteps.whenListNextHearingSubmittedWithAdjournmentCrownUnscheduled();
        
        // Verify the unscheduled next hearing was listed
        listNextHearingSteps.verifyNextHearingListedFromAPI(AbstractIT.UNALLOCATED,1);
        final Matcher<? super ReadContext> oneHearingPresentMatcher = withJsonPath("hearings", hasSize(1));
        pollForUnscheduledHearings(getLoggedInUser(), UUID.fromString(nextHearingValues.courtCentreId), oneHearingPresentMatcher);
        
        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully with unscheduled next hearing for initial hearing: {}", 
                   firstHearingValues.hearingId);
    }
    
    @Test
    void shouldUpdateHearingRoomAllocation() {
        // First, create an allocated hearing using SPI allocated scenario
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues hearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiAllocated();
        
        // Verify the hearing was listed as allocated
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);
        
        // Now update the room allocation
        PayloadBasedUpdateHearingSteps updateHearingSteps = new PayloadBasedUpdateHearingSteps(hearingValues.hearingId,hearingValues);
        PayloadGenerator.PayloadValues updatedValues = updateHearingSteps.whenUpdateHearingSubmittedWithAllocatedRoomUpdate();
        
        // Verify the hearing room was updated
        updateHearingSteps.verifyHearingUpdatedFromAPI(AbstractIT.ALLOCATED);
        updateHearingSteps.verifyCourtRoomUpdatedForHearing(hearingValues.courtRoomId);
        
        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully - room allocation updated for hearing {}", 
                   hearingValues.hearingId);
    }
} 