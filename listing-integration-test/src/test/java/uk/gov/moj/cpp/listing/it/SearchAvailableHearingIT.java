package uk.gov.moj.cpp.listing.it;

import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.progression.courts.JurisdictionType;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

public class SearchAvailableHearingIT extends AbstractIT {

    public static final String CASE_IN_HEARING = "CASE_IN_HEARING";
    public static final String MATCHED_DEFENDANTS = "MATCHED_DEFENDANTS";
    public static final String CASE_AND_MATCHED_DEFENDANTS = "CASE_IN_HEARING,MATCHED_DEFENDANTS";

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
    public void shouldListAvailableHearingForCaseInHearingAndCaseUrn() {

        final UUID hearingId = UUID.randomUUID();
        final UUID hearingId2 = UUID.randomUUID();
        final UUID masterDefendantId = UUID.randomUUID();
        final UUID masterDefendantId2 = UUID.randomUUID();
        final String caseUrn = STRING.next();
        final String jurisdictionType = JurisdictionType.CROWN.name();
        final String caseUrnForLinkedCases = STRING.next();

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, caseUrn, caseUrn, masterDefendantId, CASE_IN_HEARING, jurisdictionType, jurisdictionType,
                null, null);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(hearingId2, null,caseUrn, masterDefendantId2, null, null, jurisdictionType,
                null, null);

        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }
        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyAvailableHearingListedForCaseInHearingAndCaseUrn(caseAndDefendantData, masterDefendantId2);
        }
    }

    @Test
    public void shouldListAvailableHearingForMatchedDefendant() {

        final UUID hearingId = UUID.randomUUID();
        final UUID hearingId2 = UUID.randomUUID();
        final UUID masterDefendantId = UUID.randomUUID();
        final String caseUrn = STRING.next();
        final String caseUrn2 = STRING.next();
        final String jurisdictionType = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, null, caseUrn, masterDefendantId, MATCHED_DEFENDANTS, jurisdictionType, jurisdictionType,
                null, null);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(hearingId2, null, caseUrn2, masterDefendantId, null, null, jurisdictionType,
                null, null);

        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }
        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyAvailableHearingListedForMatchedDefendant(caseAndDefendantData, masterDefendantId);
        }
    }

    @Test
    public void shouldListAvailableHearingWithCaseInHearingAndMatchedDefendant() {

        final UUID hearingId = UUID.randomUUID();
        final UUID hearingId2 = UUID.randomUUID();
        final UUID masterDefendantId = UUID.randomUUID();
        final UUID masterDefendantId2 = UUID.randomUUID();
        final String caseUrn = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId,  null, caseUrn, masterDefendantId, CASE_AND_MATCHED_DEFENDANTS, jurisdictionTypeCrown, jurisdictionTypeCrown,
                null, null);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(hearingId2, null, caseUrn, masterDefendantId2, null, null, jurisdictionTypeCrown,
                null, null);

        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }
        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyAvailableHearingListedForCaseInHearingAndMatchedDefendant(caseAndDefendantData);
       }
    }

    @Test
    public void shouldListAvailableHearingsWithMatchedDefendant() {
        final UUID hearingId1 = UUID.randomUUID();
        final UUID masterDefendantId1 = UUID.randomUUID();
        final String caseUrn = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData1 = new CaseAndDefendantData(hearingId1, null, caseUrn, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                null, null);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(null, null, caseUrn, masterDefendantId1, null, null, jurisdictionTypeCrown,
                null, null);

        try (ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
             ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2))) {
            listCourtHearingSteps1.whenCaseIsSubmittedForListing();
            listCourtHearingSteps2.verifyAvailableHearing(caseAndDefendantData1, masterDefendantId1);
        }
    }

    @Test
    public void shouldListAvailableHearingsWithCaseUrnForLinkedCases() {
        final UUID hearingId1 = UUID.randomUUID();
        final UUID masterDefendantId1 = UUID.randomUUID();
        final String caseUrn = STRING.next();
        final String caseUrnForLinkedCases = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData1 = new CaseAndDefendantData(hearingId1, null, caseUrn, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                caseUrnForLinkedCases, caseUrnForLinkedCases);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(null, null, null, null, MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                caseUrnForLinkedCases, caseUrnForLinkedCases);

        try (ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
             ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2))) {
            listCourtHearingSteps1.whenCaseIsSubmittedForListing();
            listCourtHearingSteps2.verifyAvailableHearing(caseAndDefendantData1, masterDefendantId1);
        }
    }
}
