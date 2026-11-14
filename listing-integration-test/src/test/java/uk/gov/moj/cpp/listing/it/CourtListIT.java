package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocation;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlotsWithQueryParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.steps.AddDefendantSteps;
import uk.gov.moj.cpp.listing.steps.CourtApplicationSteps;
import uk.gov.moj.cpp.listing.steps.CourtListSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantOffencesSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CourtListIT extends AbstractIT {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String ALPHABETICAL = "Alphabetical";
    private static final String PUBLIC = "Public";
    private static final String ONLINE_PUBLIC = "Online_Public";
    public static final String STANDARD = "Standard";
    public static final String PRISON = "Prison";
    public static final String JUDGE = "Judge";
    public static final String BENCH = "Bench";
    private static final String[] PUBLISH_LIST_TYPES_SHARING_EX_PARTE_FILTERING = {PUBLIC, ONLINE_PUBLIC, STANDARD, BENCH};
    private static final UUID COURT_CENTRE_ID = getRandomCourtCenterId();
    private static final UUID HEARING_TYPE_ID = fromString("52edf232-3c09-4c74-a6ad-737985c2e662");

    private CourtListSteps courtListSteps;
    private HearingsData firstHearing;
    private ListCourtHearingSteps firstHearingListCourtHearingSteps;

    @BeforeEach
    public void setupStepsForCourtList() {
        firstHearing = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearing);
        firstHearingListCourtHearingSteps = listCourtHearingSteps;
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(firstHearing.getHearingData().get(0).getId());

        // Stub court scheduler service for listing hearings in court sessions to prevent 404 error
        stubListHearingInCourtSessionsWithMultipleSchedules(
                firstHearing.getHearingData().get(0).getId().toString(),
                updatedHearingDataForAllocation.getNonDefaultDays().get(0).getCourtScheduleId().map(UUID::fromString).orElse(null).toString(),
                updatedHearingDataForAllocation.getNonDefaultDays().get(1).getCourtScheduleId().map(UUID::fromString).orElse(null).toString(),
                ZonedDateTime.parse(updatedHearingDataForAllocation.getNonDefaultDays().get(0).getStartTime()),updatedHearingDataForAllocation.getNonDefaultDays().get(0).getDuration().orElse(20));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(firstHearing, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();
        courtListSteps = new CourtListSteps(updatedHearingDataForAllocation);
    }

    @Test

    public void generateAlphabeticalCourtListForHearing() {
        courtListSteps.verifyCourtListRequestedAndIsCorrect(ALPHABETICAL);
    }


    @Test

    public void generatePublicCourtList() {
        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithJmsDelay(PUBLIC, "PublicCourtListEnglishWelsh", new Matcher[0]);
    }

    @Test

    public void generatePublicCourtWhenHearingAdjourned() throws IOException {
        HearingsData nextHearing = HearingsData.nextHearingsData(firstHearing.getHearingData());
        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearing.getHearingData().get(0));
        listNextHearingSteps1.whenNextHearingSubmittedForListing(nextHearing);
        listNextHearingSteps1.verifyHearingListedFromAPIWithJmsDelay(nextHearing);

        UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(nextHearing.getHearingData().get(0).getId());

        final UpdateHearingSteps updateHearingSteps2 = new UpdateHearingSteps(nextHearing, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps2.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps2.getUpdatedHearingData());
        updateHearingSteps2.whenHearingIsUpdatedForListing();
        updateHearingSteps2.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();
        updateHearingSteps2.verifyPublicEventHearingChangesSaved();
        courtListSteps = new CourtListSteps(updatedHearingDataForAllocation);

        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithJmsDelay(PUBLIC, "PublicCourtListEnglishWelsh", new Matcher[0]);
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
        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithJmsDelay(PUBLIC, "PublicCourtListEnglishWelsh", allocatedMatchers);
    }

    @Test
    public void generatePublicCourtWhenOffenceAddedToHearingWithExParte() {

        final HearingsData hearingsData = HearingsData.hearingsDataWithExParteOffence();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
                listCourtHearingSteps.whenCaseIsSubmittedForListing();
                listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(AbstractIT.ALLOCATED);
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        // stubbed last listed case without exParte offence
        final ListedCaseData listedCaseWithoutExParte = hearingData.getListedCases().stream().reduce((first, second) -> second).get();
        final DefendantData defendantWithoutExParte = listedCaseWithoutExParte.getDefendants().stream().reduce((first, second) -> second).get();
        final OffenceData offenceWithoutExParte = defendantWithoutExParte.getOffences().stream().reduce((first, second) -> second).get();
        final String templateName = "PublicCourtListEnglishWelsh";
        // The ExParte scenario can list more than one hearing in this timeslot, and the court-list JSON
        // does not order them deterministically, so a positional hearings[0] matcher intermittently matched
        // the wrong hearing (90s RestPoller ConditionTimeout). Anchor the assertions to THIS hearing by id.
        final String exParteHearingPath =
                "$.hearingDates[0].courtRooms[0].timeslots[0].hearings[?(@.id=='" + hearingData.getId().toString() + "')]";
        final Matcher[] allocatedMatchers = {
                withJsonPath(exParteHearingPath + ".id", contains(hearingData.getId().toString())),
                withJsonPath(exParteHearingPath + ".caseId", contains(listedCaseWithoutExParte.getCaseId().toString())),
                withJsonPath(exParteHearingPath + ".defendants[0].id", contains(defendantWithoutExParte.getDefendantId().toString())),
                withJsonPath(exParteHearingPath + ".defendants[0].offences[0].id", contains(offenceWithoutExParte.getOffenceId().toString())),
                withJsonPath("$.templateName", is(templateName))
        };

        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithExParte(PUBLIC, allocatedMatchers,
                hearingData.getCourtCentreId(), hearingData.getCourtRoomId(), hearingData.getHearingStartDate().format(DATE_TIME_FORMATTER), hearingData.getHearingEndDate().format(DATE_TIME_FORMATTER));
    }

    @Test
    public void generatePublicCourtExcludesApplicationLinkedToSingleExParteCaseOnSameHearing() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithSingleExParteOffence();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(AbstractIT.ALLOCATED);
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final UUID applicationId = hearingData.getCourtApplications().get(0).getId();

        final Matcher[] matchers = {
                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[?(@.courtApplicationId=='" + applicationId + "')]", hasSize(0))
        };

        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithExParte(PUBLIC, matchers,
                hearingData.getCourtCentreId(), hearingData.getCourtRoomId(),
                hearingData.getHearingStartDate().format(DATE_TIME_FORMATTER), hearingData.getHearingEndDate().format(DATE_TIME_FORMATTER));
    }

    @Test
    public void generateAllCourtListTypesExcludeApplicationSharingHearingWithExParteCaseEvenWhenLinkedToDifferentCase() {

        final UUID unrelatedLinkedCaseId = UUID.randomUUID();
        final HearingsData hearingsData = HearingsData.hearingsDataWithSingleExParteOffenceAndApplicationLinkedToDifferentCase(unrelatedLinkedCaseId);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(AbstractIT.ALLOCATED);
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final UUID exParteCaseId = hearingData.getListedCases().get(0).getCaseId();
        final UUID applicationId = hearingData.getCourtApplications().get(0).getId();

        final Matcher[] matchers = {
                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[?(@.caseId=='" + exParteCaseId + "')]", hasSize(0)),
                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[?(@.courtApplicationId=='" + applicationId + "')]", hasSize(0))
        };

        for (final String listId : PUBLISH_LIST_TYPES_SHARING_EX_PARTE_FILTERING) {
            courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithExParte(listId, matchers,
                    hearingData.getCourtCentreId(), hearingData.getCourtRoomId(),
                    hearingData.getHearingStartDate().format(DATE_TIME_FORMATTER), hearingData.getHearingEndDate().format(DATE_TIME_FORMATTER));
        }

        // ALPHABETICAL renders a flat "defendants" array with no application field at all
        // (see generateAlphabeticalCourtListExcludesApplicationLinkedToExParteCase), and its query
        // scopes only by courtCentreId+date (no courtRoomId) - so other tests' unrelated hearings
        // at the same court centre/date can appear too. Assert the ex-parte case's own reference
        // is absent rather than asserting the whole list is empty.
        final String exParteCaseReference = hearingData.getListedCases().get(0).getCaseReference();
        final Matcher[] alphabeticalMatchers = {
                withJsonPath("$.defendants[?(@.caseReference=='" + exParteCaseReference + "')]", hasSize(0))
        };
        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithExParte(ALPHABETICAL, alphabeticalMatchers,
                hearingData.getCourtCentreId(), hearingData.getCourtRoomId(),
                hearingData.getHearingStartDate().format(DATE_TIME_FORMATTER), hearingData.getHearingEndDate().format(DATE_TIME_FORMATTER));
    }

    @Test
    public void generatePublicCourtExcludesApplicationLinkedToExParteCaseListedOnSeparateHearing() {

        final UUID crownCourtCentreId = getRandomCourtCenterId();
        final UUID crownCourtRoomId = UUID.randomUUID();
        final UUID exParteCaseCourtRoomId = UUID.randomUUID();
        final String courtScheduleId = UUID.randomUUID().toString();
        final LocalDate hearingDate = ItClock.today();
        final ZonedDateTime hearingStartTime = ItClock.nowUtc().withHour(10).withMinute(0).withSecond(0).withNano(0);

        final HearingsData exParteHearingsData = HearingsData.hearingsDataWithSingleExParteOffence(crownCourtCentreId, exParteCaseCourtRoomId);
        final UUID exParteCaseId = exParteHearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();

        final HearingsData standaloneApplicationData = HearingsData.hearingsDataStandaloneApplicationLinkedToCase(exParteCaseId);
        final HearingData standaloneHearing = standaloneApplicationData.getHearingData().get(0);
        setStandaloneHearingScheduling(standaloneHearing, crownCourtCentreId, crownCourtRoomId, hearingDate, hearingStartTime);

        stubGetReferenceDataHearingTypes(standaloneHearing.getHearingTypeData().getTypeId());
        stubOrganisationUnit(crownCourtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(crownCourtCentreId, LocalTime.of(10, 0), "6:30", crownCourtRoomId, "Test Crown Court"));

        final ListCourtHearingSteps standaloneApplicationSteps = new ListCourtHearingSteps(standaloneApplicationData);
        stubProvisionalBooking(crownCourtCentreId, crownCourtRoomId, courtScheduleId, hearingDate, hearingStartTime);
        stubListHearingInCourtSessions(standaloneHearing.getId().toString(), courtScheduleId, hearingStartTime);

        standaloneApplicationSteps.whenCaseIsSubmittedForListingStandaloneApplication();
        standaloneApplicationSteps.verifyHearingListedFromAPIForStandaloneApplication(AbstractIT.ALLOCATED);

        final ListCourtHearingSteps exParteListCourtHearingSteps = new ListCourtHearingSteps(exParteHearingsData);
        exParteListCourtHearingSteps.whenCaseIsSubmittedForListing();
        exParteListCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(AbstractIT.ALLOCATED);

        final UUID applicationId = standaloneHearing.getCourtApplications().get(0).getId();
        final Matcher[] matchers = {
                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[?(@.courtApplicationId=='" + applicationId + "')]", hasSize(0))
        };

        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithExParte(PUBLIC, matchers,
                crownCourtCentreId, crownCourtRoomId, hearingDate.format(DATE_TIME_FORMATTER), hearingDate.format(DATE_TIME_FORMATTER));
    }

    @Test
    public void generatePublicCourtExcludesApplicationLinkedToExParteCaseListedOnUnrelatedCaseHearing() throws Exception {

        firstHearingListCourtHearingSteps.verifyPublicEventCourtApplicationAdded();

        final UUID sharedCourtCentreId = getRandomCourtCenterId();
        final UUID exParteCaseCourtRoomId = UUID.randomUUID();

        final HearingsData exParteHearingsData = HearingsData.hearingsDataWithSingleExParteOffence(sharedCourtCentreId, exParteCaseCourtRoomId);
        final UUID exParteCaseId = exParteHearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();


        final HearingsData unrelatedHearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps unrelatedListCourtHearingSteps = new ListCourtHearingSteps(unrelatedHearingsData);
        unrelatedListCourtHearingSteps.whenCaseIsSubmittedForListing();
        unrelatedListCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        unrelatedListCourtHearingSteps.verifyPublicEventCourtApplicationAdded();
        final HearingData unrelatedHearing = unrelatedHearingsData.getHearingData().get(0);
        final UUID unrelatedCaseId = unrelatedHearing.getListedCases().get(0).getCaseId();

        final UUID applicationId = unrelatedHearing.getCourtApplications().get(0).getId();
        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(unrelatedHearingsData);
        courtApplicationSteps.whenCourtApplicationIsAddedToHearing(unrelatedHearing.getId(), exParteCaseId);
        courtApplicationSteps.verifyPublicEventCourtApplicationAdded();

        final ListCourtHearingSteps exParteListCourtHearingSteps = new ListCourtHearingSteps(exParteHearingsData);
        exParteListCourtHearingSteps.whenCaseIsSubmittedForListing();
        exParteListCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(AbstractIT.ALLOCATED);
        exParteListCourtHearingSteps.verifyPublicEventCourtApplicationAdded();

        final UpdatedHearingData unrelatedHearingAllocation = updatedHearingDataForAllocation(unrelatedHearing.getId());
        setField(unrelatedHearingAllocation, "courtCentreId", sharedCourtCentreId);
        for (final uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData nonDefaultDay : unrelatedHearingAllocation.getNonDefaultDays()) {
            setField(nonDefaultDay, "courtCentreId", Optional.of(sharedCourtCentreId.toString()));
        }

        stubListHearingInCourtSessionsWithMultipleSchedules(unrelatedHearingAllocation);
        final UpdateHearingSteps unrelatedUpdateHearingSteps = new UpdateHearingSteps(unrelatedHearingsData, unrelatedHearingAllocation);
        unrelatedUpdateHearingSteps.whenHearingIsUpdatedForListing();
        unrelatedUpdateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();
        unrelatedUpdateHearingSteps.verifyPublicEventHearingChangesSaved();

        final String unrelatedCaseHearingPath =
                "$.hearingDates[0].courtRooms[0].timeslots[0].hearings[?(@.caseId=='" + unrelatedCaseId + "')]";
        final Matcher[] matchers = {
                withJsonPath(unrelatedCaseHearingPath + ".caseId", contains(unrelatedCaseId.toString())),
                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[?(@.courtApplicationId=='" + applicationId + "')]", hasSize(0)),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[?(@.courtApplicationId=='" + applicationId + "')]", hasSize(0))
        };

        final CourtListSteps unrelatedCourtListSteps = new CourtListSteps(unrelatedHearingAllocation);

        for (final String listId : PUBLISH_LIST_TYPES_SHARING_EX_PARTE_FILTERING) {
            unrelatedCourtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithExParte(listId, matchers,
                    sharedCourtCentreId, unrelatedHearingAllocation.getCourtRoomId(),
                    unrelatedHearingAllocation.getStartDate(), unrelatedHearingAllocation.getEndDate(), true);
        }
    }

    @Test
    public void generateAlphabeticalCourtListExcludesApplicationLinkedToExParteCase() {

        final HearingsData hearingsData = HearingsData.hearingsDataWithSingleExParteOffence();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(AbstractIT.ALLOCATED);
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String exParteCaseReference = hearingData.getListedCases().get(0).getCaseReference();

        final Matcher[] matchers = {
                withJsonPath("$.defendants[?(@.caseReference=='" + exParteCaseReference + "')]", hasSize(0))
        };

        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithExParte(ALPHABETICAL, matchers,
                hearingData.getCourtCentreId(), hearingData.getCourtRoomId(),
                hearingData.getHearingStartDate().format(DATE_TIME_FORMATTER), hearingData.getHearingEndDate().format(DATE_TIME_FORMATTER));
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
        stubParams.put("BOOKING_ID", UUID.randomUUID().toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
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
        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithJmsDelay(PUBLIC, "PublicCourtListEnglishWelsh", allocatedMatchers);
    }


    @Test
    public void generateStandardCourtList() {
        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithJmsDelay(STANDARD, "BenchAndStandardCourtList", new Matcher[0]);
    }

    @Test

    public void generatePrisonCourtList() {
        final Matcher<?>[] extraMatchers = {
                withJsonPath("$.courtCentreDefaultStartTime", notNullValue())
        };
        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithJmsDelay(PRISON, "PrisonCourtList", extraMatchers);
    }

    @Test

    public void generateJudgeList() {
        stubOrganisationUnit(COURT_CENTRE_ID);
        stubGetReferenceDataHearingTypes(HEARING_TYPE_ID);
        courtListSteps.verifyCourtListRequestedAndIsCorrect(JUDGE);
    }

    @Test

    public void generateBenchList() {
        courtListSteps.verifyCourtListRequestedAndIsCorrectJsonWithJmsDelay(BENCH, "BenchAndStandardCourtList", new Matcher[0]);
    }

}
