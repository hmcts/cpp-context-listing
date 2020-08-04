package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.io.IOException;

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
    public void shouldListHearingWithAdjournedDateMultipleCountBasedSlots() {

        stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
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
    public void shouldListHearingWithAdjournedDateMultipleDurationBasedSlots() {

        stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
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
        }
    }

    @Test
    public void shouldExtendHearingWithShadowListedFlag() throws IOException {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithShadowListedOffences())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.whenProgressionHearingExtended();
            listCourtHearingSteps.verifyHearingExtendedWithShadowListedFlag(ALLOCATED);
        }
    }
}
