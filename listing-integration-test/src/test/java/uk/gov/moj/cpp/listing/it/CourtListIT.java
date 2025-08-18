package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocation;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.steps.AddDefendantSteps;
import uk.gov.moj.cpp.listing.steps.CourtListSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantOffencesSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData;

import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CourtListIT extends AbstractIT {

    private static final String ALPHABETICAL = "Alphabetical";
    private static final String PUBLIC = "Public";
    public static final String STANDARD = "Standard";
    public static final String PRISON = "Prison";
    public static final String JUDGE = "Judge";
    public static final String BENCH = "Bench";
    final UUID COURT_CENTRE_ID = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
    final UUID HEARING_TYPE_ID = fromString("52edf232-3c09-4c74-a6ad-737985c2e662");

    private CourtListSteps courtListSteps;
    private HearingsData firstHearing;

    @BeforeEach
    public void setupStepsForCourtList() {
        firstHearing = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearing);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(firstHearing.getHearingData().get(0).getId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(firstHearing, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();
        courtListSteps = new CourtListSteps(updatedHearingDataForAllocation);
    }

    @Test
    public void generateAlphabeticalCourtListForHearing() {
        courtListSteps.verifyCourtListRequestedAndIsCorrect(ALPHABETICAL);
    }


    @Test
    public void generatePublicCourtList() {
        courtListSteps.verifyCourtListRequestedAndIsCorrectJson(PUBLIC, "PublicCourtListEnglishWelsh", new Matcher[0]);
    }

    @Test
    public void generatePublicCourtWhenHearingAdjourned() {
        HearingsData nextHearing = HearingsData.nextHearingsData(firstHearing.getHearingData());
        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearing.getHearingData().get(0));
        listNextHearingSteps1.whenNextHearingSubmittedForListing(nextHearing);
        listNextHearingSteps1.verifyHearingListedFromAPI(nextHearing);

        UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(nextHearing.getHearingData().get(0).getId());

        final UpdateHearingSteps updateHearingSteps2 = new UpdateHearingSteps(nextHearing, updatedHearingDataForAllocation);
        updateHearingSteps2.whenHearingIsUpdatedForListing();
        updateHearingSteps2.verifyHearingAllocatedWhenQueryingFromAPI();
        updateHearingSteps2.verifyPublicEventHearingChangesSaved();
        courtListSteps = new CourtListSteps(updatedHearingDataForAllocation);

        courtListSteps.verifyCourtListRequestedAndIsCorrectJson(PUBLIC, "PublicCourtListEnglishWelsh", new Matcher[0]);
    }

    @Test
    public void generatePublicCourtWhenOffenceAddedToHearing() {
        DefendantData defendantData = firstHearing.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = firstHearing.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = firstHearing.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, null);
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedAddedOnly();
        final Matcher[] allocatedMatchers = {
                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[0].offences[3].id", notNullValue()),
                withoutJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[0].offences[3].listingNumber"),
        };
        courtListSteps.verifyCourtListRequestedAndIsCorrectJson(PUBLIC, "PublicCourtListEnglishWelsh", allocatedMatchers);
    }

    @Test
    public void generatePublicCourtWhenDefendantAdded() {

        UUID caseId = firstHearing.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = firstHearing.getHearingData().get(0);
        final AddDefendantSteps addDefendantSteps = new AddDefendantSteps(caseId, hearingData);
        addDefendantSteps.whenCaseDefendantsAddedPublicEventIsPublished();

        final Matcher[] allocatedMatchers = {
                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[2].offences[0].id", notNullValue()),
                withoutJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[2].offences[0].listingNumber"),
        };
        courtListSteps.verifyCourtListRequestedAndIsCorrectJson(PUBLIC, "PublicCourtListEnglishWelsh", allocatedMatchers);
    }


    @Test
    public void generateStandardCourtList() {
        courtListSteps.verifyCourtListRequestedAndIsCorrectJson(STANDARD, "BenchAndStandardCourtList", new Matcher[0]);
    }

    @Test
    public void generatePrisonCourtList() {
        final Matcher<?>[] extraMatchers = {
                withJsonPath("$.courtCentreDefaultStartTime", notNullValue())
        };
        courtListSteps.verifyCourtListRequestedAndIsCorrectJson(PRISON, "PrisonCourtList", extraMatchers);
    }

    @Test
    public void generateJudgeList() {
        stubOrganisationUnit(COURT_CENTRE_ID);
        stubGetReferenceDataHearingTypes(HEARING_TYPE_ID);
        courtListSteps.verifyCourtListRequestedAndIsCorrect(JUDGE);
    }

    @Test
    public void generateBenchList() {
        courtListSteps.verifyCourtListRequestedAndIsCorrectJson(BENCH, "BenchAndStandardCourtList", new Matcher[0]);
    }

}
