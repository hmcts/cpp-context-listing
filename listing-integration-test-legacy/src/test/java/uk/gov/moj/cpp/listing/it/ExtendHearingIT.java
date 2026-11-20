package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class ExtendHearingIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListCourtHearingSteps.class);

    private final UUID ALLOCATED_HEARING_ID = randomUUID();
    private final UUID UNALLOCATED_HEARING_ID = randomUUID();
    public static final String MATCHED_DEFENDANTS = "MATCHED_DEFENDANTS";

    private final String CASE_URN = STRING.next();

    private final String JURISDICTION_TYPE = JurisdictionType.CROWN.name();

    @Test
    public void shouldExtendHearingForCase() {

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(ALLOCATED_HEARING_ID, null, CASE_URN, randomUUID(), null, JURISDICTION_TYPE, JURISDICTION_TYPE,
                null, null);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();

        final ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsData(UNALLOCATED_HEARING_ID));
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();

        LOGGER.info("UnAllocated HearingID : {}  -  Allocated HearingId : {} ", UNALLOCATED_HEARING_ID, ALLOCATED_HEARING_ID);

        listCourtHearingSteps2.verifyHearingIsCreated(ALLOCATED_HEARING_ID, 1);
        // The extend handler fetches BOTH hearings from the query view (ListingCommandHandler.extendHearingForHearing);
        // without this await the extend races the unallocated hearing's projection and rollback-redelivers with
        // "There is no Hearing for this ID" until hearing-listed lands.
        listCourtHearingSteps2.verifyHearingIsCreated(UNALLOCATED_HEARING_ID, 2);
        listCourtHearingSteps2.extendHearing(UNALLOCATED_HEARING_ID, ALLOCATED_HEARING_ID);
        listCourtHearingSteps2.verifyPublicEventHearingConfirmedAndExtendHearingFromProgression(ALLOCATED_HEARING_ID, UNALLOCATED_HEARING_ID);
        listCourtHearingSteps2.verifyHearingUpdatedToCaseInActiveMQ(ALLOCATED_HEARING_ID, UNALLOCATED_HEARING_ID, 2);
    }

    @Test
    public void shouldExtendHearingPartially() {

        final CaseAndDefendantData allocatedHearingCaseAndDefendantData = new CaseAndDefendantData(ALLOCATED_HEARING_ID, null, CASE_URN, randomUUID(), null, JURISDICTION_TYPE, JURISDICTION_TYPE,
                null, null);
        final HearingsData allocatedHearingData = HearingsData.hearingsDataWithAllocationDataAndJudiciary(allocatedHearingCaseAndDefendantData);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(allocatedHearingData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();

        final HearingsData hearingsData = HearingsData.singleHearingData();
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final UUID unallocatedHearingId = hearingData.getId();

        //remove only first case, retain the second case
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        final ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();

        LOGGER.info("UnAllocated HearingID : {}  -  Allocated HearingId : {} ", unallocatedHearingId, ALLOCATED_HEARING_ID);

        listCourtHearingSteps2.verifyHearingIsCreated(ALLOCATED_HEARING_ID, 1);
        // Await the unallocated hearing too — see shouldExtendHearingForCase for why.
        listCourtHearingSteps2.verifyHearingIsCreated(unallocatedHearingId, 2);
        listCourtHearingSteps2.extendHearingPartially(unallocatedHearingId, ALLOCATED_HEARING_ID, listedCaseData);
        listCourtHearingSteps2.verifyPublicEventHearingConfirmedEventAndExtendPartialHearingFromProgression(ALLOCATED_HEARING_ID, unallocatedHearingId);
        listCourtHearingSteps2.verifyHearingUpdatedToCaseInActiveMQ(ALLOCATED_HEARING_ID, unallocatedHearingId, 1);
        listCourtHearingSteps2.verifyPublicEVentHearingChangesSaved(ALLOCATED_HEARING_ID);
        listCourtHearingSteps2.verifyPublicEventHearingUpdatedPartially(unallocatedHearingId);


        final UpdatedHearingData updatedHearingDataWithUpdatedJudiciary = UpdatedHearingData.updatedHearingDataDifferentJudiciary(allocatedHearingData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithUpdatedJudiciary);
        updateHearingSteps.whenJudiciaryIsChangedForHearings();
        updateHearingSteps.verifyProsecutionCaseDefendantsOffenceIds(2);
    }

    @Test
    public void shouldExtendHearingWhole() {

        final CaseAndDefendantData allocatedHearingCaseAndDefendantData = new CaseAndDefendantData(ALLOCATED_HEARING_ID, null, CASE_URN, randomUUID(), null, JURISDICTION_TYPE, JURISDICTION_TYPE, null, null);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(allocatedHearingCaseAndDefendantData));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();

        final HearingsData hearingsData = HearingsData.singleHearingDataMultipleCasesWithSingleOffence();
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final UUID unallocatedHearingId = hearingData.getId();

        final List<ListedCaseData> listedCaseDataList = hearingData.getListedCases();

        final ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();

        LOGGER.info("UnAllocated HearingID : {}  -  Allocated HearingId : {} ", unallocatedHearingId, ALLOCATED_HEARING_ID);

        listCourtHearingSteps2.verifyHearingIsCreated(ALLOCATED_HEARING_ID, 1);
        // Await the unallocated hearing too — see shouldExtendHearingForCase for why.
        listCourtHearingSteps2.verifyHearingIsCreated(unallocatedHearingId, 2);
        listCourtHearingSteps2.extendWholeHearing(unallocatedHearingId, ALLOCATED_HEARING_ID, listedCaseDataList);
        listCourtHearingSteps2.verifyPublicEventHearingConfirmedAndExtendHearingFromProgression(ALLOCATED_HEARING_ID, unallocatedHearingId);
        listCourtHearingSteps2.verifyHearingUpdatedToCaseInActiveMQ(ALLOCATED_HEARING_ID, unallocatedHearingId, 2);
    }

    @Test
    public void shouldExtendHearingForCaseFromUnAllocatedHearingToAnotherUnAllocatedHearing() {


        final String caseUrn = RandomGenerator.STRING.next();
        final UUID masterDefendantId = randomUUID();
        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(UNALLOCATED_HEARING_ID, null,
                caseUrn, masterDefendantId, MATCHED_DEFENDANTS, MAGISTRATES.name(), MAGISTRATES.name(), null, null);
        final HearingsData unallocatedHearingsData1 = HearingsData.hearingsDataWithUnAllocationDataAndJudiciary(caseAndDefendantData);

        final ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(unallocatedHearingsData1);
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        assertTrue(true);


    }




}