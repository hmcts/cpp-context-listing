package uk.gov.moj.cpp.listing.it;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

public class ExtendHearingIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListCourtHearingSteps.class);

    private final UUID ALLOCATED_HEARING_ID = UUID.randomUUID();
    private final UUID UNALLOCATED_HEARING_ID = UUID.randomUUID();
    private final String CASE_URN = STRING.next();

    private final String JURISDICTION_TYPE = JurisdictionType.CROWN.name();

    @Test
    public void shouldExtendHearingForCase() throws IOException {

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(ALLOCATED_HEARING_ID, null, CASE_URN, UUID.randomUUID(), null, JURISDICTION_TYPE, JURISDICTION_TYPE,
                null, null);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsData(UNALLOCATED_HEARING_ID))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();

            LOGGER.info("UnAllocated HearingID : {}  -  Allocated HearingId : {} ", UNALLOCATED_HEARING_ID, ALLOCATED_HEARING_ID);

            listCourtHearingSteps.verifyHearingIsCreated(ALLOCATED_HEARING_ID, 1);
            listCourtHearingSteps.extendHearing(UNALLOCATED_HEARING_ID, ALLOCATED_HEARING_ID);
            listCourtHearingSteps.verifyHearingConfirmedEventForExtendHearingPublicMQ(ALLOCATED_HEARING_ID, UNALLOCATED_HEARING_ID);
            listCourtHearingSteps.verifyHearingUpdatedToCaseInActiveMQ(ALLOCATED_HEARING_ID, UNALLOCATED_HEARING_ID);
            listCourtHearingSteps.verifyHearingDeletedInActiveMQ(UNALLOCATED_HEARING_ID);
        }
    }

    @Test
    public void shouldExtendHearingPartially() throws IOException {

        final CaseAndDefendantData allocatedHearingCaseAndDefendantData = new CaseAndDefendantData(ALLOCATED_HEARING_ID, null, CASE_URN, UUID.randomUUID(), null, JURISDICTION_TYPE, JURISDICTION_TYPE,
                null, null);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(allocatedHearingCaseAndDefendantData))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        final HearingsData hearingsData = HearingsData.singleHearingData();
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final UUID unallocatedHearingId = hearingData.getId();
        //remove only first case, retain the second case
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();

            LOGGER.info("UnAllocated HearingID : {}  -  Allocated HearingId : {} ", unallocatedHearingId, ALLOCATED_HEARING_ID);

            listCourtHearingSteps.verifyHearingIsCreated(ALLOCATED_HEARING_ID, 1);
            listCourtHearingSteps.extendHearingPartially(unallocatedHearingId, ALLOCATED_HEARING_ID, listedCaseData);
            listCourtHearingSteps.verifyHearingConfirmedEventForExtendPartialHearingPublicMQ(ALLOCATED_HEARING_ID, unallocatedHearingId);
            listCourtHearingSteps.verifyHearingUpdatedToCaseInActiveMQ(ALLOCATED_HEARING_ID, unallocatedHearingId);
            listCourtHearingSteps.verifyHearingUpdatedPartiallyInActiveMQ(unallocatedHearingId);
        }
    }

    @Test
    public void shouldExtendHearingWhole() throws IOException {

        final CaseAndDefendantData allocatedHearingCaseAndDefendantData = new CaseAndDefendantData(ALLOCATED_HEARING_ID, null, CASE_URN, UUID.randomUUID(), null, JURISDICTION_TYPE, JURISDICTION_TYPE, null, null);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(allocatedHearingCaseAndDefendantData))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        final HearingsData hearingsData = HearingsData.singleHearingDataMultipleCasesWithSingleOffence();
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final UUID unallocatedHearingId = hearingData.getId();

        final List<ListedCaseData> listedCaseDataList = hearingData.getListedCases();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();

            LOGGER.info("UnAllocated HearingID : {}  -  Allocated HearingId : {} ", unallocatedHearingId, ALLOCATED_HEARING_ID);

            listCourtHearingSteps.verifyHearingIsCreated(ALLOCATED_HEARING_ID, 1);
            listCourtHearingSteps.extendWholeHearing(unallocatedHearingId, ALLOCATED_HEARING_ID, listedCaseDataList);
            listCourtHearingSteps.verifyHearingConfirmedEventForExtendHearingPublicMQ(ALLOCATED_HEARING_ID, unallocatedHearingId);
            listCourtHearingSteps.verifyHearingUpdatedToCaseInActiveMQ(ALLOCATED_HEARING_ID, unallocatedHearingId);
            listCourtHearingSteps.verifyHearingDeletedInActiveMQ(unallocatedHearingId);
        }
    }
}
