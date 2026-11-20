package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.util.List.of;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubChangeCourtRoomForMultidayHearing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubCrownBlockSearchAndBookForHearing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubFinalCrownAdSessionsInCourtRoom;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsForSchedules;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsForSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMultiDaySearchAndBookFailure;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMultiDaySearchAndBookForHearing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyChangeCourtRoomForMultidayHearingCalled;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyCrownBlockSearchAndBookCalledForHearing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyCrownBlockSearchAndBookCalledWithoutAnchorForHearing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyFinalCrownSessionLookupNeverMadeForCourtRoom;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMultiDaySearchAndBookCalledForHearing;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataOrganisationUnitById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.ChangeCourtRoomStubSession;
import uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.CrownBlockStubSession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

/**
 * Per-day courtrooms of an allocated multi-day CROWN hearing across a LATER
 * update-hearing-for-listing that carries no courtScheduleId (SPRDT-1539).
 *
 * <p>On the Crown team branches a day's courtroom changes through
 * {@code change-court-room-for-multiday-hearing} (the UI's change-courtroom journey; the command is
 * covered by {@link ChangeCourtRoomForMultidayHearingIT}). These tests pin what a later update does
 * to the rooms that command set:</p>
 * <ul>
 *   <li>same court centre, main courtroom and first day: listing sends crown.search.and.book with
 *       no anchor and runs no FINAL-session check, so courtscheduler resizes the block it holds in
 *       place and every untouched day keeps its session and courtroom; a new tail day courtscheduler
 *       cannot book is its 422 NO_AVAILABILITY, passed through;</li>
 *   <li>a new main courtroom: every day moves into it, anchored on its FINAL session.</li>
 * </ul>
 * <p>Courtscheduler is stubbed: the tests pin listing's requests and how listing projects the answer.
 * The resize itself is courtscheduler's and is proved against a deployed environment by
 * cpp-apitests. Days are Monday to Thursday so no weekend falls inside a block.</p>
 */
class HearingDayCourtRoomChangeForCrownIT extends AbstractIT {

    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING =
            "application/vnd.listing.command.update-hearing-for-listing+json";
    private static final String MEDIA_TYPE_CHANGE_COURT_ROOM =
            "application/vnd.listing.command.change-court-room-for-multiday-hearing+json";
    private static final String MEDIA_TYPE_SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    private static final String UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY = "listing.command.update-hearing-for-listing";
    private static final String CHANGE_COURT_ROOM_ENDPOINT_KEY = "listing.command.change-court-room-for-multiday-hearing";
    private static final String LISTING_QUERY_HEARING = "listing.search.hearing";
    private static final int DAY_MINUTES = 360;
    private static final LocalTime COURT_START_TIME = LocalTime.of(10, 0);

    @Test
    void shouldKeepEachDaysCourtroomWhenAnUpdateWithoutCourtScheduleIdExtendsTheBlockInTheSameMainCourtroom() {
        final AllocatedCrownBlock block = givenAllocatedMondayToWednesdayCrownBlock();
        // The first day moves: the case where an anchor in the main courtroom would turn the update into a move.
        final UUID secondRoom = UUID.randomUUID();
        final String secondRoomSessionD1 = UUID.randomUUID().toString();
        givenDayMovedToCourtRoom(block, block.day1, secondRoom, secondRoomSessionD1);

        final LocalDate day4 = block.day3.plusDays(1);
        final String sessionD4 = UUID.randomUUID().toString();
        // courtscheduler's resize answer: the held days keep their sessions and rooms, Thursday is the new tail day.
        final List<CrownBlockStubSession> resized = of(
                new CrownBlockStubSession(secondRoomSessionD1, secondRoom, block.day1),
                new CrownBlockStubSession(block.scheduleD2, block.courtRoomId, block.day2),
                new CrownBlockStubSession(block.scheduleD3, block.courtRoomId, block.day3),
                new CrownBlockStubSession(sessionD4, block.courtRoomId, day4));
        stubCrownBlockSearchAndBookForHearing(block.hearingId.toString(), block.courtHouseId, resized, COURT_START_TIME);
        stubListHearingInCourtSessionsForSessions(block.hearingId.toString(), resized, COURT_START_TIME, DAY_MINUTES);

        final Response response = postUpdateWithoutCourtScheduleId(block, block.courtRoomId, block.day1, day4);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyCrownBlockSearchAndBookCalledWithoutAnchorForHearing(block.hearingId.toString(), block.day1, day4, 4 * DAY_MINUTES);
        verifyFinalCrownSessionLookupNeverMadeForCourtRoom(block.courtRoomId);
        pollHearingDays(block.hearingId, 4,
                dayIn(block.day1, secondRoom, secondRoomSessionD1),
                dayIn(block.day2, block.courtRoomId, block.scheduleD2),
                dayIn(block.day3, block.courtRoomId, block.scheduleD3),
                dayIn(day4, block.courtRoomId, sessionD4));
    }

    @Test
    void shouldReturn422NamingOnlyTheNewDayWhenCourtschedulerCannotExtendTheBlock() {
        final AllocatedCrownBlock block = givenAllocatedMondayToWednesdayCrownBlock();
        final UUID secondRoom = UUID.randomUUID();
        givenDayMovedToCourtRoom(block, block.day1, secondRoom, UUID.randomUUID().toString());

        final LocalDate day4 = block.day3.plusDays(1);
        stubMultiDaySearchAndBookFailure(block.hearingId.toString(), 422, "NO_AVAILABILITY", of(day4.toString()));

        final Response response = postUpdateWithoutCourtScheduleId(block, block.courtRoomId, block.day1, day4);

        assertThat(response.getStatus(), is(422));
        final String body = response.readEntity(String.class);
        assertThat(body, containsString("NO_AVAILABILITY"));
        assertThat(body, containsString(day4.toString()));
        // Monday sits in another courtroom: no main-courtroom check rejects it.
        assertThat(body, not(containsString(block.day1.toString())));
        verifyFinalCrownSessionLookupNeverMadeForCourtRoom(block.courtRoomId);
    }

    @Test
    void shouldMoveEveryDayIntoTheNewMainCourtroomWhenAnUpdateWithoutCourtScheduleIdChangesIt() {
        final AllocatedCrownBlock block = givenAllocatedMondayToWednesdayCrownBlock();
        givenDayMovedToCourtRoom(block, block.day2, UUID.randomUUID(), UUID.randomUUID().toString());

        final UUID newMainRoom = UUID.randomUUID();
        final Map<LocalDate, String> newRoomSessions = new LinkedHashMap<>();
        newRoomSessions.put(block.day1, UUID.randomUUID().toString());
        newRoomSessions.put(block.day2, UUID.randomUUID().toString());
        newRoomSessions.put(block.day3, UUID.randomUUID().toString());
        stubFinalCrownAdSessionsInCourtRoom(newMainRoom, block.courtHouseId, newRoomSessions);
        final List<CrownBlockStubSession> moved = newRoomSessions.entrySet().stream()
                .map(e -> new CrownBlockStubSession(e.getValue(), newMainRoom, e.getKey()))
                .toList();
        stubCrownBlockSearchAndBookForHearing(block.hearingId.toString(), block.courtHouseId, moved, COURT_START_TIME);
        stubListHearingInCourtSessionsForSessions(block.hearingId.toString(), moved, COURT_START_TIME, DAY_MINUTES);
        givenReferenceDataStubsForUpdateHearing(block.courtCentreId, newMainRoom);

        final Response response = postUpdateWithoutCourtScheduleId(block, newMainRoom, block.day1, block.day3);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyCrownBlockSearchAndBookCalledForHearing(block.hearingId.toString(), newRoomSessions.get(block.day1),
                3 * DAY_MINUTES, block.day3);
        pollHearingDays(block.hearingId, 3,
                dayIn(block.day1, newMainRoom, newRoomSessions.get(block.day1)),
                dayIn(block.day2, newMainRoom, newRoomSessions.get(block.day2)),
                dayIn(block.day3, newMainRoom, newRoomSessions.get(block.day3)));
    }

    // ---------------------------------------------------------------------------------------
    // Arrange helpers
    // ---------------------------------------------------------------------------------------

    /**
     * An allocated CROWN hearing on Monday, Tuesday and Wednesday in one courtroom, each day on its
     * own known session: a hearing listed through list-court-hearing, then allocated by a raw
     * multi-day update the stubbed courtscheduler answers with the three sessions (the seeding
     * pattern of {@link ChangeCourtRoomForMultidayHearingIT}).
     */
    private AllocatedCrownBlock givenAllocatedMondayToWednesdayCrownBlock() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final String scheduleD1 = UUID.randomUUID().toString();
        final String scheduleD2 = UUID.randomUUID().toString();
        final String scheduleD3 = UUID.randomUUID().toString();
        final LocalDate day1 = ItClock.today().plusDays(30).with(nextOrSame(MONDAY));
        final LocalDate day2 = day1.plusDays(1);
        final LocalDate day3 = day1.plusDays(2);

        final List<String> sessions = of(scheduleD1, scheduleD2, scheduleD3);
        stubMultiDaySearchAndBookForHearing(hearingId.toString(), sessions, courtHouseId, courtRoomId, day1, false);
        stubListHearingInCourtSessionsForSchedules(hearingId.toString(), sessions, day1.atTime(9, 0).atZone(java.time.ZoneOffset.UTC), DAY_MINUTES);
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        final ListCourtHearingSteps seedSteps = new ListCourtHearingSteps(HearingsData.hearingsData(hearingId));
        seedSteps.whenCaseIsSubmittedForListing();
        seedSteps.verifyHearingIsCreated(hearingId, 2);

        givenReferenceDataStubsForUpdateHearing(courtCentreId, courtRoomId);
        restClient.postCommand(buildUrl(UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY, hearingId), MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                rawMultidayUpdatePayload(courtCentreId, courtRoomId, day1), getLoggedInHeader());
        verifyMultiDaySearchAndBookCalledForHearing(hearingId.toString(), 3 * DAY_MINUTES);

        pollHearingDays(hearingId, 3,
                dayIn(day1, courtRoomId, scheduleD1),
                dayIn(day2, courtRoomId, scheduleD2),
                dayIn(day3, courtRoomId, scheduleD3));
        return new AllocatedCrownBlock(hearingId, courtCentreId, courtRoomId, courtHouseId,
                day1, day2, day3, scheduleD1, scheduleD2, scheduleD3);
    }

    /** The UI's change-courtroom journey for one day: change-court-room-for-multiday-hearing, courtscheduler stubbed. */
    private void givenDayMovedToCourtRoom(final AllocatedCrownBlock block, final LocalDate day,
                                          final UUID newCourtRoomId, final String newCourtScheduleId) {
        stubChangeCourtRoomForMultidayHearing(block.hearingId.toString(), of(new ChangeCourtRoomStubSession(
                newCourtScheduleId, newCourtRoomId.toString(), day.toString(), day + "T09:00:00Z", DAY_MINUTES)));
        final String payload = Json.createObjectBuilder()
                .add("sendNotificationToParties", false)
                .add("nonDefaultDays", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("startTime", day + "T09:00:00Z")
                        .add("duration", DAY_MINUTES)
                        .add("courtCentreId", block.courtCentreId.toString())
                        .add("roomId", newCourtRoomId.toString())
                        .add("courtScheduleId", newCourtScheduleId)
                        .add("virtual", true)))
                .build()
                .toString();

        final Response response = restClient.postCommand(buildUrl(CHANGE_COURT_ROOM_ENDPOINT_KEY, block.hearingId),
                MEDIA_TYPE_CHANGE_COURT_ROOM, payload, getLoggedInHeader());

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyChangeCourtRoomForMultidayHearingCalled(block.hearingId.toString());
        pollHearingDays(block.hearingId, 3, dayIn(day, newCourtRoomId, newCourtScheduleId));
    }

    /** An update naming the court centre and a main courtroom, with no courtScheduleId and no nonDefaultDays. */
    private Response postUpdateWithoutCourtScheduleId(final AllocatedCrownBlock block, final UUID mainCourtRoomId,
                                                      final LocalDate startDate, final LocalDate endDate) {
        final String payload = Json.createObjectBuilder()
                .add("courtCentreId", block.courtCentreId.toString())
                .add("courtRoomId", mainCourtRoomId.toString())
                .add("selectedCourtCentre", Json.createObjectBuilder()
                        .add("id", block.courtCentreId.toString())
                        .add("courtRoomId", mainCourtRoomId.toString())
                        .add("courtCentreName", "Test Court Centre")
                        .add("ouCode", "B01LY00"))
                .add("type", Json.createObjectBuilder()
                        .add("id", "4a0e892d-c0c5-3c51-95b8-704d8c781776")
                        .add("description", "Plea"))
                .add("startDate", startDate.toString())
                .add("endDate", endDate.toString())
                .add("nonSittingDays", Json.createArrayBuilder())
                .add("nonDefaultDays", Json.createArrayBuilder())
                .add("judiciary", Json.createArrayBuilder())
                .add("jurisdictionType", "CROWN")
                .add("hearingLanguage", "ENGLISH")
                .add("publicListNote", "")
                .add("hasVideoLink", false)
                .add("sendNotificationToParties", false)
                .build()
                .toString();
        return restClient.postCommand(buildUrl(UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY, block.hearingId),
                MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, payload, getLoggedInHeader());
    }

    /** The raw multi-day shape (one nonDefaultDay carrying the block total, no courtScheduleId) that allocates the fixture. */
    private static String rawMultidayUpdatePayload(final UUID courtCentreId, final UUID courtRoomId, final LocalDate startDate) {
        return Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtRoomId", courtRoomId.toString())
                .add("selectedCourtCentre", Json.createObjectBuilder()
                        .add("id", courtCentreId.toString())
                        .add("courtRoomId", courtRoomId.toString())
                        .add("courtCentreName", "Test Court Centre")
                        .add("ouCode", "B01LY00"))
                .add("type", Json.createObjectBuilder()
                        .add("id", "4a0e892d-c0c5-3c51-95b8-704d8c781776")
                        .add("description", "Plea"))
                .add("startDate", startDate.toString())
                // The window is wider than the block, as in ChangeCourtRoomForMultidayHearingIT's
                // seeding payload, so the 1080 block total is never clamped to the window.
                .add("endDate", startDate.plusDays(57).toString())
                .add("nonSittingDays", Json.createArrayBuilder())
                .add("nonDefaultDays", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("startTime", startDate + "T09:00:00Z")
                        .add("courtCentreId", courtCentreId.toString())
                        .add("roomId", courtRoomId.toString())
                        .add("duration", 3 * DAY_MINUTES)))
                .add("judiciary", Json.createArrayBuilder())
                .add("jurisdictionType", "CROWN")
                .add("hearingLanguage", "ENGLISH")
                .add("publicListNote", "")
                .add("hasVideoLink", false)
                .add("sendNotificationToParties", false)
                .build()
                .toString();
    }

    private static void givenReferenceDataStubsForUpdateHearing(final UUID courtCentreId, final UUID courtRoomId) {
        final CourtCentreData courtCentreData = new CourtCentreData(courtCentreId, LocalTime.of(10, 30), "6:30",
                courtRoomId, "Test Court Centre");
        stubGetReferenceDataCourtCentre(courtCentreData);
        stubGetReferenceDataCourtCentreById(courtCentreData);
        stubGetReferenceDataCourtMappings(courtCentreData);
        stubGetReferenceDataHearingTypes(UUID.randomUUID());
        stubGetReferenceDataOrganisationUnitById(courtCentreId);
        stubOrganisationUnit(courtCentreId);
    }

    // ---------------------------------------------------------------------------------------
    // Assert helpers
    // ---------------------------------------------------------------------------------------

    @SafeVarargs
    private void pollHearingDays(final UUID hearingId, final int dayCount, final List<Matcher<? super ReadContext>>... days) {
        final List<Matcher<? super ReadContext>> matchers = new java.util.ArrayList<>();
        matchers.add(withJsonPath("$.id", is(hearingId.toString())));
        matchers.add(withJsonPath("$.hearingDays", hasSize(dayCount)));
        for (final List<Matcher<? super ReadContext>> day : days) {
            matchers.addAll(day);
        }
        pollWithDefaults(requestParams(searchHearingUrl(hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(OK), payload().isJson(allOf(matchers)));
    }

    /** One hearing day, matched by date: its courtroom and courtScheduleId. */
    private static List<Matcher<? super ReadContext>> dayIn(final LocalDate date, final UUID courtRoomId, final String courtScheduleId) {
        return of(
                withJsonPath("$.hearingDays[?(@.hearingDate=='" + date + "')].courtRoomId", hasItem(courtRoomId.toString())),
                withJsonPath("$.hearingDays[?(@.hearingDate=='" + date + "')].courtScheduleId", hasItem(courtScheduleId)));
    }

    private static String buildUrl(final String endpointKey, final UUID hearingId) {
        return String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(endpointKey), hearingId.toString()));
    }

    private static String searchHearingUrl(final UUID hearingId) {
        return String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId.toString()));
    }

    private record AllocatedCrownBlock(UUID hearingId, UUID courtCentreId, UUID courtRoomId, UUID courtHouseId,
                                       LocalDate day1, LocalDate day2, LocalDate day3,
                                       String scheduleD1, String scheduleD2, String scheduleD3) {
    }
}
