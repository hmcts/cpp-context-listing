package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class SearchAvailableHearingIT extends AbstractIT {

    public static final String CASE_IN_HEARING = "CASE_IN_HEARING";
    public static final String MATCHED_DEFENDANTS = "MATCHED_DEFENDANTS";
    public static final String CASE_AND_MATCHED_DEFENDANTS = "CASE_IN_HEARING,MATCHED_DEFENDANTS";

    @Test
    void shouldListAvailableHearingForCaseInHearingAndCaseUrn() {

        final UUID hearingId = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID masterDefendantId2 = randomUUID();
        final String caseUrn = STRING.next();
        final String jurisdictionType = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, caseUrn, caseUrn, masterDefendantId, CASE_IN_HEARING, jurisdictionType, jurisdictionType,
                null, null);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(hearingId2, null, caseUrn, masterDefendantId2, null, null, jurisdictionType,
                null, null);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAvailableHearingListedForCaseInHearingAndCaseUrnWithJmsDelay(caseAndDefendantData, masterDefendantId2);
    }

    @Test
    void shouldListAvailableHearingForMatchedDefendant() {

        final UUID hearingId = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final String caseUrn = STRING.next();
        final String caseUrn2 = STRING.next();
        final String jurisdictionType = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, null, caseUrn, masterDefendantId, MATCHED_DEFENDANTS, jurisdictionType, jurisdictionType,
                null, null);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(hearingId2, null, caseUrn2, masterDefendantId, null, null, jurisdictionType,
                null, null);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAvailableHearingListedForMatchedDefendantWithJmsDelay(caseAndDefendantData, masterDefendantId);
    }

    @Test
    void shouldListAllAvailableHearingForMatchedCaseUrn() {

        final UUID hearingId = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final String caseUrn = STRING.next();
        final String jurisdictionType = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, null, caseUrn, masterDefendantId, MATCHED_DEFENDANTS, jurisdictionType, jurisdictionType,
                null, null);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(hearingId2, caseUrn, caseUrn, masterDefendantId, CASE_IN_HEARING, jurisdictionType, jurisdictionType,
                null, null);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData));
        final ZonedDateTime hearingStartTime = listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);

        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithUnAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAllAvailableHearingListedForMatchedDefendantWithJmsDelay(caseAndDefendantData2, masterDefendantId);
    }

    @Test
    void shouldListAvailableHearingWithCaseInHearingAndMatchedDefendant() {

        final UUID hearingId = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID masterDefendantId2 = randomUUID();
        final String caseUrn = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, null, caseUrn, masterDefendantId, CASE_AND_MATCHED_DEFENDANTS, jurisdictionTypeCrown, jurisdictionTypeCrown,
                null, null);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(hearingId2, null, caseUrn, masterDefendantId2, null, null, jurisdictionTypeCrown,
                null, null);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAvailableHearingListedForCaseInHearingAndMatchedDefendantWithJmsDelay(caseAndDefendantData);
    }

    @Test
    void shouldListAvailableHearingsWithMatchedDefendant() {
        final UUID hearingId1 = randomUUID();
        final UUID masterDefendantId1 = randomUUID();
        final String caseUrn = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData1 = new CaseAndDefendantData(hearingId1, null, caseUrn, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                null, null);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(null, null, caseUrn, masterDefendantId1, null, null, jurisdictionTypeCrown,
                null, null);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAvailableHearingWithJmsDelay(caseAndDefendantData1, masterDefendantId1, false);
    }

    @Test
    void shouldListAvailableHearingsWithCaseUrnForLinkedCases() {
        final UUID hearingId1 = randomUUID();
        final UUID masterDefendantId1 = randomUUID();
        final String caseUrn = STRING.next();
        final String caseUrnForLinkedCases = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData1 = new CaseAndDefendantData(hearingId1, null, caseUrn, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                caseUrnForLinkedCases, caseUrnForLinkedCases);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(null, null, null, null, MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                caseUrnForLinkedCases, caseUrnForLinkedCases);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAvailableHearingWithJmsDelay(caseAndDefendantData1, masterDefendantId1, false);
    }

    @Test
    
    public void shouldRetunNotesAndListAvailableHearingsWhenHearingsAndNotesExist() {
        final UUID hearingId1 = randomUUID();
        final UUID masterDefendantId1 = randomUUID();
        final String caseUrn = STRING.next();
        final String caseUrnForLinkedCases = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData1 = new CaseAndDefendantData(hearingId1, null, caseUrn, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                caseUrnForLinkedCases, caseUrnForLinkedCases);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
        listCourtHearingSteps1.createListingNotesForStartDays();
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps1.verifyAvailableHearingWithJmsDelay(caseAndDefendantData1, masterDefendantId1, true);
        listCourtHearingSteps1.verifyNotesViaRangeSearch();
    }

    @Test
    void shouldNotReturnNotesWhenAvailableHearingsNotExist() {
        final UUID hearingId1 = randomUUID();
        final UUID masterDefendantId1 = randomUUID();
        final String caseUrn = STRING.next();
        final String caseUrnForLinkedCases = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData1 = new CaseAndDefendantData(hearingId1, null, caseUrn, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                caseUrnForLinkedCases, caseUrnForLinkedCases);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
        listCourtHearingSteps1.createListingNotes();
        listCourtHearingSteps1.verifyAvailableHearingNotExistsWithJmsDelay(masterDefendantId1);
    }
}
