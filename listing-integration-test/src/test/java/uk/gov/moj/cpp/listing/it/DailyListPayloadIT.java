package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForCrownAllocation;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.DailyListPayloadSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DailyListPayloadIT extends AbstractIT {

    private DailyListPayloadSteps dailyListPayloadSteps;
    private UpdatedHearingData updatedHearingData;

    @BeforeEach
    public void setUp() {
        super.setUp();
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        // FIRM (like WARN) requires CROWN jurisdiction - RangeSearchQueryRequestFactory filters
        // week-commencing list types on jurisdictionType = CROWN.
        updatedHearingData = updatedHearingDataForCrownAllocation(hearingsData.getHearingData().get(0).getId());

        stubListHearingInCourtSessionsWithMultipleSchedules(
                hearingsData.getHearingData().get(0).getId().toString(),
                updatedHearingData.getNonDefaultDays().get(0).getCourtScheduleId().map(UUID::fromString).orElse(null).toString(),
                updatedHearingData.getNonDefaultDays().get(1).getCourtScheduleId().map(UUID::fromString).orElse(null).toString(),
                ZonedDateTime.parse(updatedHearingData.getNonDefaultDays().get(0).getStartTime()),
                updatedHearingData.getNonDefaultDays().get(0).getDuration().orElse(20));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingData);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();

        dailyListPayloadSteps = new DailyListPayloadSteps(hearingsData, updatedHearingData);
    }

    @Test
    void shouldReturnDailyListPayloadForDraft() {
        dailyListPayloadSteps.verifyDailyListPayloadContainsHearing("DRAFT");
    }

    @Test
    void shouldReturnDailyListPayloadForFinal() {
        dailyListPayloadSteps.verifyDailyListPayloadContainsHearing("FINAL");
    }

    @Test
    void shouldReturnDailyListPayloadForFirm() {
//        dailyListPayloadSteps.verifyWeekCommencingListPayloadContainsHearing("FIRM", updatedHearingData.getEndDate());

        // Also submit a standalone application (no case) on its own isolated court centre and
        // verify it shows up on a FIRM list too. Kept on a separate centre from the case: a
        // standalone application has no judiciary, so it always lands in a different sitting than
        // a judge-bearing case (SittingKey = date+room+judicialId) - and the shared
        // verifyHearingListedFromAPIForStandaloneApplication helper assumes a single hearing per
        // court centre, which the case sharing a centre would break anyway.
        final UUID crownCourtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID crownCourtRoomId = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final String courtScheduleId = randomUUID().toString();

        final HearingsData standaloneApplicationData = HearingsData.hearingsDataStandaloneApplicationWithSubject();
        final HearingData standaloneHearing = standaloneApplicationData.getHearingData().get(0);

        final LocalDate hearingDate = ItClock.today();
        final ZonedDateTime hearingStartTime = ItClock.nowUtc().withHour(10).withMinute(0).withSecond(0).withNano(0);

        setStandaloneHearingScheduling(standaloneHearing, crownCourtCentreId, crownCourtRoomId, hearingDate, hearingStartTime);

        stubGetReferenceDataHearingTypes(standaloneHearing.getHearingTypeData().getTypeId());
        stubOrganisationUnit(crownCourtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(crownCourtCentreId, LocalTime.of(10, 0), "6:30", crownCourtRoomId, "Test Crown Court"));

        final ListCourtHearingSteps standaloneApplicationSteps = new ListCourtHearingSteps(standaloneApplicationData);
        stubProvisionalBooking(crownCourtCentreId, crownCourtRoomId, courtScheduleId, hearingDate, hearingStartTime);
        stubListHearingInCourtSessions(standaloneHearing.getId().toString(), courtScheduleId, hearingStartTime);

        standaloneApplicationSteps.whenCaseIsSubmittedForListingStandaloneApplication();
        standaloneApplicationSteps.verifyHearingListedFromAPIForStandaloneApplication(ALLOCATED);

        final String subjectFirstName = standaloneHearing.getCourtApplications().get(0).getSubject().getFirstName();
        final String subjectLastName = standaloneHearing.getCourtApplications().get(0).getSubject().getLastName();
        final String weekCommencingEndDate = hearingDate.plusDays(2).toString();

        dailyListPayloadSteps.verifyWeekCommencingFirmListPayloadContainsApplicationSubject(
                crownCourtCentreId, hearingDate, weekCommencingEndDate, subjectFirstName, subjectLastName);
    }

    private void setStandaloneHearingScheduling(final HearingData hearing, final UUID courtCentreId, final UUID courtRoomId,
                                                final LocalDate hearingDate, final ZonedDateTime hearingStartTime) {
        try {
            setField(hearing, "courtCentreId", courtCentreId);
            setField(hearing, "courtRoomId", courtRoomId);
            setField(hearing, "hearingStartDate", hearingDate);
            setField(hearing, "hearingEndDate", hearingDate);
            setField(hearing, "hearingStartTime", hearingStartTime);
            hearing.setName("Test Crown Court");
        } catch (final Exception e) {
            throw new RuntimeException("Failed to set standalone hearing scheduling fields", e);
        }
    }

    private void setField(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void stubProvisionalBooking(final UUID courtCentreId, final UUID courtRoomId, final String courtScheduleId,
                                        final LocalDate hearingDate, final ZonedDateTime hearingStartTime) {
        final Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtRoomId.toString());
        stubParams.put("BOOKING_ID", randomUUID().toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
    }
}
