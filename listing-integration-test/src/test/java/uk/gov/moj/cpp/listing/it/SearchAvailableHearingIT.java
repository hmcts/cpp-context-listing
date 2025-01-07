package uk.gov.moj.cpp.listing.it;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class SearchAvailableHearingIT extends AbstractIT {

    public static final String CASE_IN_HEARING = "CASE_IN_HEARING";
    public static final String MATCHED_DEFENDANTS = "MATCHED_DEFENDANTS";
    public static final String CASE_AND_MATCHED_DEFENDANTS = "CASE_IN_HEARING,MATCHED_DEFENDANTS";

    private static final String CONTEXT_NAME = "listing";

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @BeforeEach
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "listing_notes");
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
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(hearingId2, null, caseUrn, masterDefendantId2, null, null, jurisdictionType,
                null, null);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAvailableHearingListedForCaseInHearingAndCaseUrn(caseAndDefendantData, masterDefendantId2);
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

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAvailableHearingListedForMatchedDefendant(caseAndDefendantData, masterDefendantId);
    }

    @Test
    public void shouldListAllAvailableHearingForMatchedCaseUrn() {

        final UUID hearingId = UUID.randomUUID();
        final UUID hearingId2 = UUID.randomUUID();
        final UUID masterDefendantId = UUID.randomUUID();
        final String caseUrn = STRING.next();
        final String caseUrn2 = STRING.next();
        final String jurisdictionType = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, null, caseUrn, masterDefendantId, MATCHED_DEFENDANTS, jurisdictionType, jurisdictionType,
                null, null);
        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(hearingId2, caseUrn, caseUrn, masterDefendantId, CASE_IN_HEARING, jurisdictionType, jurisdictionType,
                null, null);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithUnAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAllAvailableHearingListedForMatchedDefendant(caseAndDefendantData2, masterDefendantId);
    }

    @Test
    public void shouldListAvailableHearingWithCaseInHearingAndMatchedDefendant() {

        final UUID hearingId = UUID.randomUUID();
        final UUID hearingId2 = UUID.randomUUID();
        final UUID masterDefendantId = UUID.randomUUID();
        final UUID masterDefendantId2 = UUID.randomUUID();
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
        listCourtHearingSteps2.verifyAvailableHearingListedForCaseInHearingAndMatchedDefendant(caseAndDefendantData);
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

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAvailableHearing(caseAndDefendantData1, masterDefendantId1, false);
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

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyAvailableHearing(caseAndDefendantData1, masterDefendantId1, false);
    }

    @Test
    public void shouldRetunNotesAndListAvailableHearingsWhenHearingsAndNotesExist() {
        final UUID hearingId1 = UUID.randomUUID();
        final UUID masterDefendantId1 = UUID.randomUUID();
        final String caseUrn = STRING.next();
        final String caseUrnForLinkedCases = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData1 = new CaseAndDefendantData(hearingId1, null, caseUrn, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                caseUrnForLinkedCases, caseUrnForLinkedCases);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
        listCourtHearingSteps1.createListingNotes();
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps1.verifyAvailableHearing(caseAndDefendantData1, masterDefendantId1, true);
        listCourtHearingSteps1.verifyNotesViaRangeSearch();
    }

    @Test
    public void shouldNotReturnNotesWhenAvailableHearingsNotExist() {
        final UUID hearingId1 = UUID.randomUUID();
        final UUID masterDefendantId1 = UUID.randomUUID();
        final String caseUrn = STRING.next();
        final String caseUrnForLinkedCases = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData1 = new CaseAndDefendantData(hearingId1, null, caseUrn, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                caseUrnForLinkedCases, caseUrnForLinkedCases);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
        listCourtHearingSteps1.createListingNotes();
        listCourtHearingSteps1.verifyAvailableHearingNotExists(caseAndDefendantData1, masterDefendantId1);
    }
}
