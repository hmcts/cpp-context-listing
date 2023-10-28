package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import com.jayway.jsonpath.Filter;
import org.hamcrest.Matcher;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
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
    public void shouldExtendHearingForCase() throws IOException {

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(ALLOCATED_HEARING_ID, null, CASE_URN, randomUUID(), null, JURISDICTION_TYPE, JURISDICTION_TYPE,
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
        }
    }

    @Test
    public void shouldExtendHearingPartially() throws IOException {

        final CaseAndDefendantData allocatedHearingCaseAndDefendantData = new CaseAndDefendantData(ALLOCATED_HEARING_ID, null, CASE_URN, randomUUID(), null, JURISDICTION_TYPE, JURISDICTION_TYPE,
                null, null);
        final HearingsData allocatedHearingData = HearingsData.hearingsDataWithAllocationDataAndJudiciary(allocatedHearingCaseAndDefendantData);
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(allocatedHearingData)) {
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
            listCourtHearingSteps.verifyPublicHearingChangesSavedInPublicMQ(ALLOCATED_HEARING_ID);
            listCourtHearingSteps.verifyPublicHearingUpdatedPartiallyInActiveMQ(unallocatedHearingId);

        }

        final UpdatedHearingData updatedHearingDataWithUpdatedJudiciary = UpdatedHearingData.updatedHearingDataDifferentJudiciary(allocatedHearingData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithUpdatedJudiciary)) {
            updateHearingSteps.whenJudiciaryIsChangedForHearings();
            updateHearingSteps.verifyProsecutionCaseDefendantsOffenceIds(2);
        }
    }

    @Test
    public void shouldExtendHearingWhole() throws IOException {

        final CaseAndDefendantData allocatedHearingCaseAndDefendantData = new CaseAndDefendantData(ALLOCATED_HEARING_ID, null, CASE_URN, randomUUID(), null, JURISDICTION_TYPE, JURISDICTION_TYPE, null, null);

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
        }
    }

    @Test
    public void shouldExtendHearingForCaseFromUnAllocatedHearingToAnotherUnAllocatedHearing() {


        final String caseUrn = RandomGenerator.STRING.next();
        final UUID masterDefendantId = randomUUID();
        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(UNALLOCATED_HEARING_ID, null,
                caseUrn, masterDefendantId, MATCHED_DEFENDANTS, MAGISTRATES.name(), MAGISTRATES.name(), null, null);
        final HearingsData unallocatedHearingsData1 = HearingsData.hearingsDataWithUnAllocationDataAndJudiciary(caseAndDefendantData);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(unallocatedHearingsData1)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        final HearingsData hearingsData = HearingsData.singleHearingDataMultipleCasesWithSingleOffence();
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final UUID unallocatedHearingId2 = hearingData.getId();


        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            LOGGER.info("UnAllocated HearingID 1: {}  -  UnAllocated HearingId 2: {} ", UNALLOCATED_HEARING_ID, unallocatedHearingId2);
            listCourtHearingSteps.verifyHearingIsCreated(UNALLOCATED_HEARING_ID, 1);
            listCourtHearingSteps.extendHearing(unallocatedHearingId2, UNALLOCATED_HEARING_ID);
        }


        final UpdatedHearingData updatedHearingData = UpdatedHearingData.updatedHearingData(unallocatedHearingsData1.getHearingData().get(0));

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(unallocatedHearingsData1, updatedHearingData)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingConfirmedInPublicMQ();


        }
    }




}