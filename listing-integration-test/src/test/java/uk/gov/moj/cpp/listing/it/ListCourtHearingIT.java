package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.moj.cpp.listing.it.SearchAvailableHearingIT.MATCHED_DEFENDANTS;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased;

import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.StagingHmiStub;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class ListCourtHearingIT extends AbstractIT {

    static final boolean UNALLOCATED = false;
    static final boolean ALLOCATED = true;
    private static final String CONTEXT_NAME = "listing";

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @Before
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "listing_notes");
    }

    @Test
    public void shouldListHearingWithUnallocatedData() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsData())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
    }

    @Test
    public void shouldListHearingWithAdjournedDateSingleCountBasedSlot() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
    }

    @Test
    public void shouldListHearingWithAdjournedDateSingleCountBasedSlotHmiEnabled() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }
    }

    @Test
    public void shouldListHearingWithPossibleDisqualification() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithPossibleDisqualification())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedWithPossibleDisqualificationInActiveMQ();
            listCourtHearingSteps.verifyHearingWithPossibleDisqualificationFromAPI();
        }
    }

    @Test
    public void shouldListHearingWithAdjournedDateMultipleCountBasedSlots() {

        stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
    }

    @Test
    public void shouldListHearingWithAdjournedDateMultipleCountBasedSlotsHmiEnabled() {

        stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }
    }

    @Test
    public void shouldListHearingWithAdjournedDateSingleDurationBasedSlot() {

        stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
    }

    @Test
    public void shouldListHearingWithAdjournedDateSingleDurationBasedSlotHmiEnabled() {

        stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }
    }

    @Test
    public void shouldListHearingWithAdjournedDateMultipleDurationBasedSlotsWinterTime() {

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1);

        final String courtRoomId1 = hearingsData.getHearingData().get(0).getCourtRoomId().toString();
        final String courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final Map<String, String> courtRoomSchedules = new LinkedHashMap<String, String>() {{
            put("2020-02-11", courtRoomId1);
            put("2020-02-12", courtRoomId1);
            put("2020-02-13", "33b7d399-8379-437c-980d-af9487b1198c");
        }};
        stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(courtRoomSchedules, courtCentreId);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedWithHearingDays(ALLOCATED, courtRoomSchedules.keySet().stream().toArray(String[]::new), courtRoomSchedules.values().stream().toArray(String[]::new));
        }
    }

    @Test
    public void shouldListHearingWithAdjournedDateMultipleDurationBasedSlotsWinterTimeHmiEnabled() {

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1);

        final String courtRoomId1 = hearingsData.getHearingData().get(0).getCourtRoomId().toString();
        final String courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final Map<String, String> courtRoomSchedules = new LinkedHashMap<String, String>() {{
            put("2020-02-11", courtRoomId1);
            put("2020-02-12", courtRoomId1);
            put("2020-02-13", "33b7d399-8379-437c-980d-af9487b1198c");
        }};
        stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(courtRoomSchedules, courtCentreId);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedWithHearingDays(ALLOCATED, courtRoomSchedules.keySet().stream().toArray(String[]::new), courtRoomSchedules.values().stream().toArray(String[]::new));
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }
    }

    @Test
    public void shouldListHearingWithAdjournedDateMultipleDurationBasedSlotsSummerTime() {

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1);

        final String courtRoomId1 = hearingsData.getHearingData().get(0).getCourtRoomId().toString();
        final String courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final Map<String, String> courtRoomSchedules = new LinkedHashMap<String, String>() {{
            put("2020-05-21", courtRoomId1);
            put("2020-05-22", "33b7d399-8379-437c-980d-af9487b1198c");
            put("2020-05-23", courtRoomId1);
        }};
        stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(courtRoomSchedules, courtCentreId);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedWithHearingDays(ALLOCATED, courtRoomSchedules.keySet().stream().toArray(String[]::new), courtRoomSchedules.values().stream().toArray(String[]::new));
        }
    }

    @Test
    public void shouldListHearingWithAdjournedDateMultipleDurationBasedSlotsSummerTimeHmiEnabled() {

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1);

        final String courtRoomId1 = hearingsData.getHearingData().get(0).getCourtRoomId().toString();
        final String courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final Map<String, String> courtRoomSchedules = new LinkedHashMap<String, String>() {{
            put("2020-05-21", courtRoomId1);
            put("2020-05-22", "33b7d399-8379-437c-980d-af9487b1198c");
            put("2020-05-23", courtRoomId1);
        }};
        stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(courtRoomSchedules, courtCentreId);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedWithHearingDays(ALLOCATED, courtRoomSchedules.keySet().stream().toArray(String[]::new), courtRoomSchedules.values().stream().toArray(String[]::new));
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }
    }

    @Test
    public void shouldListHearingWithAllocatedData() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
    }

    @Test
    public void shouldListHearingAsUnallocatedAndSendDummyCourtroomToHmi() {
        final UUID hearingId = randomUUID();
        final String caseUrn = RandomGenerator.STRING.next();
        final UUID masterDefendantId = randomUUID();
        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, null, caseUrn, masterDefendantId, MATCHED_DEFENDANTS, MAGISTRATES.name(), MAGISTRATES.name(),
                null, null);
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithUnAllocationDataAndJudiciary(caseAndDefendantData))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
    }

    @Test
    public void shouldListHearingWithAllocatedDataHmiEnabled() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedWithJudiciaryInfoInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }
    }

    @Test
    public void shouldListHearingWithAllocatedWhenCourtroomFoundButNoSessionsAvailablefromHmi() {
        StagingHmiStub.stubHmiNoSessionsAvailable();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedWithJudiciaryInfoInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }
    }

    @Test
    public void shouldListHearingAllocatedWithJudiciaryInfoWhenCourtroomFoundAndSessionsAvailablefromHmi() {
        StagingHmiStub.stubHmiMagsSession();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedWithJudiciaryInfoInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }
    }

    @Test
    public void shouldListHearingWithUnallocatedDataForStandaloneApplication() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataStandaloneApplication())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingStandaloneApplication();
            listCourtHearingSteps.verifyHearingListedInActiveMQForStandaloneApplication();
            listCourtHearingSteps.verifyHearingListedFromAPIForStandaloneApplication(UNALLOCATED);
        }
    }

    @Test
    public void shouldListHearingWithLegalEntity() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithLegalEntity())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingWithLegalEntity();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedWithLegalEntity(UNALLOCATED);
        }
    }

    @Test
    public void shouldListHearingByIdWhenItExists() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.singleHearingData())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyExistingHearingById();
        }
    }

    @Test
    public void shouldListHearingByIdWhenItDoesntExist() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.singleHearingData())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyNonExistentHearingById();
        }
    }

    @Test
    public void shouldListHearingByIdWhenIdIsInvalid() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.singleHearingData())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingByIdWithInvalidId();
        }
    }

    @Test
    public void shouldListHearingWithShadowListedFlag() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithShadowListedOffences())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedWithShadowListedFlag(ALLOCATED);
            listCourtHearingSteps.verifyHearingExtendedWithReportingRestriction(ALLOCATED);
        }
    }

    @Test
    public void shouldListHearingWithShadowListedFlagHmiEnabled() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithShadowListedOffences())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedWithShadowListedFlag(ALLOCATED);
            listCourtHearingSteps.verifyHearingExtendedWithReportingRestriction(ALLOCATED);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }
    }

    @Test
    public void shouldExtendHearingWithShadowListedFlag() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithShadowListedOffences())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.whenProgressionHearingExtended();
            listCourtHearingSteps.verifyHearingExtendedWithShadowListedFlag(ALLOCATED);
            listCourtHearingSteps.verifyHearingExtendedWithReportingRestriction(ALLOCATED);
        }
    }

    @Test
    public void shouldExtendHearingWithShadowListedFlagHmiEnabled() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithShadowListedOffences())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.whenProgressionHearingExtended();
            listCourtHearingSteps.verifyHearingExtendedWithShadowListedFlag(ALLOCATED);
            listCourtHearingSteps.verifyHearingExtendedWithReportingRestriction(ALLOCATED);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }
    }

    @Test
    public void shouldRetrieveCasesByDefendantAndHearingDateForAllocatedHearing() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
            listCourtHearingSteps.verifyQueryAPIFindCaseByPersonDefendantAndHearingDate();
        }
    }

    @Test
    public void shouldRetrieveCasesByDefendantAndHearingDateForUnAllocatedHearing() {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithLegalEntity())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingWithLegalEntity();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedWithLegalEntity(UNALLOCATED);
            listCourtHearingSteps.verifyQueryAPIFindCaseByOrganisationDefendantAndHearingDate();
        }
    }
}
