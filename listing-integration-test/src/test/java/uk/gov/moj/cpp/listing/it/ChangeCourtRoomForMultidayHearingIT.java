package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.util.List.of;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubChangeCourtRoomForMultidayHearing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubChangeCourtRoomForMultidayHearingFailure;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubExtendMultiDayHearing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyChangeCourtRoomForMultidayHearingCalled;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyChangeCourtRoomForMultidayHearingNeverCalled;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyExtendMultiDayHearingCalled;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataOrganisationUnitById;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory;
import uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.ChangeCourtRoomStubSession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.http.ResponseData;

/**
 * Covers {@code listing.command.change-court-room-for-multiday-hearing}: a CROWN-only wrapper on
 * {@code POST /hearings/{hearingId}} that changes the courtroom of SELECTED days of an already
 * allocated multi-day CROWN hearing, distinguished by the media type
 * {@code application/vnd.listing.command.change-court-room-for-multiday-hearing+json}.
 *
 * <p>Each day carries an optional {@code virtual} flag. Virtual days (virtual=true) are (re)booked
 * in courtscheduler and converted into hearing days; real days (virtual false/absent) are persisted
 * as nonDefaultDays - booked in courtscheduler ONLY when their courtScheduleId differs from the
 * hearing day's current schedule on that date (SPRDT-1225), otherwise without any courtscheduler
 * booking. Schema violations (duration > 360, missing
 * courtScheduleId/roomId) are rejected as 400 by the framework before COMMAND_API runs. Business
 * failures (unknown hearing, non-CROWN, non-multiday, duplicate day dates, or a courtscheduler
 * rejection) are surfaced synchronously as 422 via {@code ChangeCourtRoomForMultidayException}.
 * The happy path is asynchronous: the enriched command is sent, the aggregate emits
 * hearing-days-changed-for-hearing + hearing-day-court-schedule-updated +
 * allocated-hearing-updated-for-listing-v2, and the event processor publishes
 * public.listing.hearing-days-changed-for-hearing + public.listing.hearing-updated.
 */
class ChangeCourtRoomForMultidayHearingIT extends AbstractIT {

    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING =
            "application/vnd.listing.command.update-hearing-for-listing+json";
    private static final String MEDIA_TYPE_CHANGE_COURT_ROOM =
            "application/vnd.listing.command.change-court-room-for-multiday-hearing+json";
    private static final String MEDIA_TYPE_SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    private static final String UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY = "listing.command.update-hearing-for-listing";
    private static final String CHANGE_COURT_ROOM_ENDPOINT_KEY = "listing.command.change-court-room-for-multiday-hearing";
    private static final String LISTING_QUERY_HEARING = "listing.search.hearing";
    private static final int MULTI_DAY_TOTAL_DURATION_MINUTES = 1080;
    private static final int DAY_DURATION_MINUTES = 360;

    private static final String PUBLIC_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String PUBLIC_HEARING_DAYS_CHANGED_FOR_HEARING = "public.listing.hearing-days-changed-for-hearing";

    // ---------------------------------------------------------------------------------------
    // Group 1: happy path — selected days change room, unsubmitted day is untouched, public
    // events observed.
    // ---------------------------------------------------------------------------------------

    @Test
    void shouldChangeCourtRoomForSelectedDaysAndEmitPublicEvents() {
        final ThreeDayCrownHearing hearing = givenAllocatedThreeDayCrownHearing();

        // Drain any stale public events left on the shared topic by a previous test before we
        // create our consumers, so a foreign hearingId event never gets picked up as "ours".
        final JmsMessageConsumerClient hearingUpdatedConsumer = publicEvents.createPublicConsumer(PUBLIC_HEARING_UPDATED);
        final JmsMessageConsumerClient hearingDaysChangedConsumer = publicEvents.createPublicConsumer(PUBLIC_HEARING_DAYS_CHANGED_FOR_HEARING);

        final UUID room2 = UUID.randomUUID();
        final UUID targetScheduleD2 = UUID.randomUUID();
        final UUID targetScheduleD3 = UUID.randomUUID();

        stubChangeCourtRoomForMultidayHearing(hearing.hearingId.toString(), of(
                new ChangeCourtRoomStubSession(targetScheduleD2.toString(), room2.toString(),
                        hearing.day2.toString(), hearing.day2 + "T09:00:00Z", DAY_DURATION_MINUTES),
                new ChangeCourtRoomStubSession(targetScheduleD3.toString(), room2.toString(),
                        hearing.day3.toString(), hearing.day3 + "T09:00:00Z", DAY_DURATION_MINUTES)));

        final String payload = changeCourtRoomPayload(hearing.courtCentreId, of(
                dayChange(hearing.day2, hearing.courtRoomId, targetScheduleD2),
                dayChange(hearing.day3, hearing.courtRoomId, targetScheduleD3)));

        final Response response = postChangeCourtRoom(hearing.hearingId, payload);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyChangeCourtRoomForMultidayHearingCalled(hearing.hearingId.toString());

        // d2 and d3 moved to room2 with the new courtScheduleIds; d1 is untouched - proved not just
        // for courtRoomId/courtScheduleId but for every hearingDay field the viewstore exposes
        // (courtCentreId, startTime, endTime; hearingDate is the filter predicate itself), each
        // compared against the value captured before the change in givenAllocatedThreeDayCrownHearing.
        // id-filtered (hearingId in URL + hearingDate JSON path) paired with a concrete result
        // matcher throughout, never a bare withJsonPath.
        pollWithDefaults(requestParams(searchHearingUrl(hearing.hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(hearing.hearingId.toString())),
                                withJsonPath("$.hearingDays", hasSize(3)),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtRoomId",
                                        hasItem(room2.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtScheduleId",
                                        hasItem(targetScheduleD2.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day3 + "')].courtRoomId",
                                        hasItem(room2.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day3 + "')].courtScheduleId",
                                        hasItem(targetScheduleD3.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].courtRoomId",
                                        hasItem(hearing.courtRoomId.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].courtScheduleId",
                                        hasItem(hearing.scheduleD1.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].courtCentreId",
                                        hasItem(hearing.day1CourtCentreId)),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].startTime",
                                        hasItem(hearing.day1StartTime)),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].endTime",
                                        hasItem(hearing.day1EndTime))
                        )));

        // Match by hearingId so a stale event from another test on the shared public topic is
        // skipped rather than mistaken for ours (see UpdateHearingSteps for the same pattern).
        final JsonPath hearingDaysChanged = retrieveMessage(hearingDaysChangedConsumer,
                containsString(hearing.hearingId.toString()));
        assertThat(hearingDaysChanged, is(org.hamcrest.CoreMatchers.notNullValue()));
        assertThat(hearingDaysChanged.get("hearingId"), is(hearing.hearingId.toString()));

        final JsonPath hearingUpdated = retrieveMessage(hearingUpdatedConsumer,
                containsString(hearing.hearingId.toString()));
        assertThat(hearingUpdated, is(org.hamcrest.CoreMatchers.notNullValue()));
        // Same accessor pattern as UpdateHearingSteps.verifyPublicEventHearingUpdated: the payload is
        // wrapped in "updatedHearing" (public.listing.hearing-updated.json refs confirmedHearing.json,
        // whose id field is "id"), so this proves the event is about OUR hearing, not a coincidental
        // hearingId-substring match on the shared public topic.
        assertThat(hearingUpdated.get("updatedHearing.id"), is(hearing.hearingId.toString()));
    }

    // ---------------------------------------------------------------------------------------
    // Group 2: schema 400s — framework rejects before COMMAND_API runs, courtscheduler untouched.
    // ---------------------------------------------------------------------------------------

    @Test
    void shouldMoveRealDayRoomWithoutBookingAndKeepScheduleWhenVirtualIsFalse() {
        final ThreeDayCrownHearing hearing = givenAllocatedThreeDayCrownHearing();
        final UUID newRoom = UUID.randomUUID();

        // Pre-change: d2 sits in the seeded room with isDraft=false (from the seeding sessions).
        pollWithDefaults(requestParams(searchHearingUrl(hearing.hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtRoomId",
                                        hasItem(hearing.courtRoomId.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].isDraft",
                                        hasItem(false))
                        )));

        // A real day (virtual:false) is accepted: NOT booked in courtscheduler, but its hearing day
        // DOES move to the new room, keeping the existing courtScheduleId and isDraft. The FE's
        // legacy integer courtRoomId is tolerated and ignored (roomId is the room identity).
        final String payload = "{\"sendNotificationToParties\":false,\"nonDefaultDays\":[{"
                + "\"startTime\":\"" + hearing.day2 + "T09:00:00Z\","
                + "\"duration\":" + DAY_DURATION_MINUTES + ","
                + "\"courtCentreId\":\"" + hearing.courtCentreId + "\","
                + "\"roomId\":\"" + newRoom + "\","
                + "\"courtScheduleId\":\"" + hearing.scheduleD2 + "\","
                + "\"courtRoomId\":772,"
                + "\"virtual\":false}]}";

        final Response response = postChangeCourtRoom(hearing.hearingId, payload);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        // Real day is never sent to courtscheduler.
        verifyChangeCourtRoomForMultidayHearingNeverCalled(hearing.hearingId.toString());

        pollWithDefaults(requestParams(searchHearingUrl(hearing.hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(hearing.hearingId.toString())),
                                withJsonPath("$.hearingDays", hasSize(3)),
                                // d2 moved to the new room WITHOUT a booking...
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtRoomId",
                                        hasItem(newRoom.toString())),
                                // ...keeping its original schedule and draft state (regression: isDraft was
                                // being wiped to null by the wholesale hearingDays overwrite).
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtScheduleId",
                                        hasItem(hearing.scheduleD2.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].isDraft",
                                        hasItem(false)),
                                // ...and it is persisted as a nonDefaultDay carrying the new room (matched by
                                // roomId rather than startTime - the blob's timestamp format carries millis).
                                withJsonPath("$.nonDefaultDays[*].roomId", hasItem(newRoom.toString())),
                                // Untouched days keep room + schedule.
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].courtRoomId",
                                        hasItem(hearing.courtRoomId.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day3 + "')].courtScheduleId",
                                        hasItem(hearing.scheduleD3.toString()))
                        )));
    }

    @Test
    void shouldMoveBothRoomsInMixedRequestBookingOnlyTheVirtualDay() {
        final ThreeDayCrownHearing hearing = givenAllocatedThreeDayCrownHearing();

        final UUID room2 = UUID.randomUUID();
        final UUID room3 = UUID.randomUUID();
        final UUID targetScheduleD2 = UUID.randomUUID();

        // Courtscheduler is stubbed for the VIRTUAL day (d2) only - the real day (d3) must never reach
        // it. The booked target session is a DRAFT one (isDraft=true) so the test proves the session's
        // draft state is threaded from the response onto the hearing day.
        stubChangeCourtRoomForMultidayHearing(hearing.hearingId.toString(), of(
                new ChangeCourtRoomStubSession(targetScheduleD2.toString(), room2.toString(),
                        hearing.day2.toString(), hearing.day2 + "T09:00:00Z", DAY_DURATION_MINUTES, true)));

        // d2 virtual -> booked + moved to the session's room/schedule; d3 real -> moved to room3
        // WITHOUT booking, keeping its schedule, and persisted as a nonDefaultDay.
        final String payload = "{\"sendNotificationToParties\":false,\"nonDefaultDays\":["
                + "{\"startTime\":\"" + hearing.day2 + "T09:00:00Z\",\"duration\":" + DAY_DURATION_MINUTES + ","
                + "\"courtCentreId\":\"" + hearing.courtCentreId + "\",\"roomId\":\"" + room2 + "\","
                + "\"courtScheduleId\":\"" + targetScheduleD2 + "\",\"virtual\":true},"
                + "{\"startTime\":\"" + hearing.day3 + "T09:00:00Z\",\"duration\":" + DAY_DURATION_MINUTES + ","
                + "\"courtCentreId\":\"" + hearing.courtCentreId + "\",\"roomId\":\"" + room3 + "\","
                + "\"courtScheduleId\":\"" + hearing.scheduleD3 + "\",\"courtRoomId\":772,\"virtual\":false}]}";

        final Response response = postChangeCourtRoom(hearing.hearingId, payload);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyChangeCourtRoomForMultidayHearingCalled(hearing.hearingId.toString());

        pollWithDefaults(requestParams(searchHearingUrl(hearing.hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(hearing.hearingId.toString())),
                                withJsonPath("$.hearingDays", hasSize(3)),
                                // d2 (virtual) booked: room + courtScheduleId from the booked session,
                                // isDraft threaded from the session (true).
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtRoomId",
                                        hasItem(room2.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtScheduleId",
                                        hasItem(targetScheduleD2.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].isDraft",
                                        hasItem(true)),
                                // d3 (real) ALSO moved room - the live bug was that only the virtual day moved -
                                // but WITHOUT booking: schedule + isDraft preserved.
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day3 + "')].courtRoomId",
                                        hasItem(room3.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day3 + "')].courtScheduleId",
                                        hasItem(hearing.scheduleD3.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day3 + "')].isDraft",
                                        hasItem(false)),
                                // The real day is persisted as a nonDefaultDay carrying the new room; the
                                // virtual day's room must NOT appear there (matched by roomId - the blob's
                                // timestamp format carries millis, so startTime equality would be brittle).
                                withJsonPath("$.nonDefaultDays[*].roomId", hasItem(room3.toString())),
                                withJsonPath("$.nonDefaultDays[*].roomId", not(hasItem(room2.toString()))),
                                // d1 untouched.
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].courtRoomId",
                                        hasItem(hearing.courtRoomId.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].courtScheduleId",
                                        hasItem(hearing.scheduleD1.toString()))
                        )));
    }

    /**
     * SPRDT-1225: a real day (virtual:false) whose courtScheduleId DIFFERS from the hearing day's
     * current schedule is a room change onto another session, so it IS sent to courtscheduler -
     * the old session pays its booked duration back and the new session is deducted there - and
     * the hearing day carries the newly booked schedule while keeping the nonDefaultDay's custom
     * start time. Same-schedule real days keep the no-booking behaviour proven by
     * {@link #shouldMoveRealDayRoomWithoutBookingAndKeepScheduleWhenVirtualIsFalse}.
     */
    @Test
    void shouldRebookRealDayInCourtschedulerWhenItsCourtScheduleIdChanges() {
        final ThreeDayCrownHearing hearing = givenAllocatedThreeDayCrownHearing();

        final UUID newRoom = UUID.randomUUID();
        final UUID targetScheduleD2 = UUID.randomUUID();

        // Courtscheduler is stubbed for the REAL day: a schedule-changing real day routes through
        // the same change-court-room booking as a virtual day (release old date, book new session).
        stubChangeCourtRoomForMultidayHearing(hearing.hearingId.toString(), of(
                new ChangeCourtRoomStubSession(targetScheduleD2.toString(), newRoom.toString(),
                        hearing.day2.toString(), hearing.day2 + "T09:00:00Z", DAY_DURATION_MINUTES, false)));

        // Real day on d2 with a NEW courtScheduleId and a CUSTOM start time (11:00, unlike the
        // seeded 09:00) - the custom time must survive the rebooking on the hearing day.
        final String payload = "{\"sendNotificationToParties\":false,\"nonDefaultDays\":[{"
                + "\"startTime\":\"" + hearing.day2 + "T11:00:00Z\","
                + "\"duration\":" + DAY_DURATION_MINUTES + ","
                + "\"courtCentreId\":\"" + hearing.courtCentreId + "\","
                + "\"roomId\":\"" + newRoom + "\","
                + "\"courtScheduleId\":\"" + targetScheduleD2 + "\","
                + "\"virtual\":false}]}";

        final Response response = postChangeCourtRoom(hearing.hearingId, payload);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        // Unlike the same-schedule real day, the schedule-changing real day IS booked.
        verifyChangeCourtRoomForMultidayHearingCalled(hearing.hearingId.toString());

        pollWithDefaults(requestParams(searchHearingUrl(hearing.hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(hearing.hearingId.toString())),
                                withJsonPath("$.hearingDays", hasSize(3)),
                                // d2 rebooked: new room + NEW courtScheduleId from the booked session,
                                // isDraft threaded from the session response.
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtRoomId",
                                        hasItem(newRoom.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtScheduleId",
                                        hasItem(targetScheduleD2.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].isDraft",
                                        hasItem(false)),
                                // ...keeping the nonDefaultDay's CUSTOM 11:00 start time, not the session's
                                // 09:00 (prefix match - the blob's timestamp format carries millis).
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].startTime",
                                        hasItem(startsWith(hearing.day2 + "T11:00"))),
                                // Persisted as a nonDefaultDay carrying the new room AND the new schedule.
                                withJsonPath("$.nonDefaultDays[*].roomId", hasItem(newRoom.toString())),
                                withJsonPath("$.nonDefaultDays[*].courtScheduleId",
                                        hasItem(targetScheduleD2.toString())),
                                // Untouched days keep room + schedule.
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].courtRoomId",
                                        hasItem(hearing.courtRoomId.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].courtScheduleId",
                                        hasItem(hearing.scheduleD1.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day3 + "')].courtScheduleId",
                                        hasItem(hearing.scheduleD3.toString()))
                        )));
    }

    /**
     * SPRDT-1225 regression report (ccm34): the deployed UI omits courtScheduleId - and the
     * 'virtual' flag - on a real day when no bookable slot exists for the room/date (e.g. the
     * day's own session is fully consumed), and sends the legacy integer courtRoomId alongside
     * the uuid roomId. That exact shape used to be a schema 400 ("required key [courtScheduleId]
     * not found"); it must be ACCEPTED: persisted as a nonDefaultDay, moved to the requested room,
     * keeping the stored day's existing schedule and draft state, and never sent to courtscheduler.
     */
    @Test
    void shouldAcceptRealDayWithoutCourtScheduleIdMovingRoomWithoutBooking() {
        final ThreeDayCrownHearing hearing = givenAllocatedThreeDayCrownHearing();
        final UUID newRoom = UUID.randomUUID();

        // Exact FE shape from the live repro: no courtScheduleId, no virtual, legacy int courtRoomId,
        // custom 11:00 start time (the non-default day's whole point).
        final String payload = "{\"sendNotificationToParties\":false,\"nonDefaultDays\":[{"
                + "\"roomId\":\"" + newRoom + "\","
                + "\"duration\":" + DAY_DURATION_MINUTES + ","
                + "\"startTime\":\"" + hearing.day2 + "T11:00:00Z\","
                + "\"courtRoomId\":235,"
                + "\"courtCentreId\":\"" + hearing.courtCentreId + "\"}]}";

        final Response response = postChangeCourtRoom(hearing.hearingId, payload);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        // No courtScheduleId -> nothing to (re)book: courtscheduler is never called.
        verifyChangeCourtRoomForMultidayHearingNeverCalled(hearing.hearingId.toString());

        pollWithDefaults(requestParams(searchHearingUrl(hearing.hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(hearing.hearingId.toString())),
                                withJsonPath("$.hearingDays", hasSize(3)),
                                // d2 moved to the requested room WITHOUT a booking...
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtRoomId",
                                        hasItem(newRoom.toString())),
                                // ...keeping the stored day's existing schedule and draft state (no
                                // courtScheduleId was supplied, so the merge preserves them).
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtScheduleId",
                                        hasItem(hearing.scheduleD2.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].isDraft",
                                        hasItem(false)),
                                // ...and the custom 11:00 start time survives (prefix match - the blob's
                                // timestamp format carries millis).
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].startTime",
                                        hasItem(startsWith(hearing.day2 + "T11:00"))),
                                // Persisted as a nonDefaultDay carrying the requested room.
                                withJsonPath("$.nonDefaultDays[*].roomId", hasItem(newRoom.toString())),
                                // Untouched days keep room + schedule.
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day1 + "')].courtRoomId",
                                        hasItem(hearing.courtRoomId.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day3 + "')].courtScheduleId",
                                        hasItem(hearing.scheduleD3.toString()))
                        )));
    }

    @Test
    void shouldReturn400WhenDurationExceedsMaximum() {
        final ThreeDayCrownHearing hearing = givenAllocatedThreeDayCrownHearing();

        final String payload = "{\"nonDefaultDays\":[{"
                + "\"startTime\":\"" + hearing.day2 + "T09:00:00Z\","
                + "\"duration\":720,"
                + "\"courtCentreId\":\"" + hearing.courtCentreId + "\","
                + "\"roomId\":\"" + hearing.courtRoomId + "\","
                + "\"courtScheduleId\":\"" + UUID.randomUUID() + "\","
                + "\"virtual\":true}]}";

        final Response response = postChangeCourtRoom(hearing.hearingId, payload);

        assertThat(response.getStatus(), is(400));
        verifyChangeCourtRoomForMultidayHearingNeverCalled(hearing.hearingId.toString());
    }

    // ---------------------------------------------------------------------------------------
    // Group 3: business 422s — synchronous rejection from ListingCommandApi, no side effects.
    // ---------------------------------------------------------------------------------------

    /**
     * courtScheduleId is optional at the schema layer (real days may legitimately omit it -
     * SPRDT-1225 regression report), so a VIRTUAL day without one is now a business 422
     * (booking is its whole purpose), no longer a schema 400.
     */
    @Test
    void shouldReturn422WhenVirtualDayHasNoCourtScheduleId() {
        final ThreeDayCrownHearing hearing = givenAllocatedThreeDayCrownHearing();

        final String payload = "{\"nonDefaultDays\":[{"
                + "\"startTime\":\"" + hearing.day2 + "T09:00:00Z\","
                + "\"duration\":" + DAY_DURATION_MINUTES + ","
                + "\"courtCentreId\":\"" + hearing.courtCentreId + "\","
                + "\"roomId\":\"" + hearing.courtRoomId + "\","
                + "\"virtual\":true}]}";

        final Response response = postChangeCourtRoom(hearing.hearingId, payload);

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("MISSING_COURT_SCHEDULE_ID"));
        verifyChangeCourtRoomForMultidayHearingNeverCalled(hearing.hearingId.toString());
    }

    @Test
    void shouldReturn422OnDuplicateDayDates() {
        final ThreeDayCrownHearing hearing = givenAllocatedThreeDayCrownHearing();

        final String payload = changeCourtRoomPayload(hearing.courtCentreId, of(
                dayChange(hearing.day2, hearing.courtRoomId, UUID.randomUUID()),
                dayChange(hearing.day2, hearing.courtRoomId, UUID.randomUUID())));

        final Response response = postChangeCourtRoom(hearing.hearingId, payload);

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("DUPLICATE_DAY_DATES"));
        verifyChangeCourtRoomForMultidayHearingNeverCalled(hearing.hearingId.toString());
    }

    @Test
    void shouldReturn422WhenCourtSchedulerReturnsNoSessionFound() {
        final ThreeDayCrownHearing hearing = givenAllocatedThreeDayCrownHearing();

        final UUID targetScheduleD2 = UUID.randomUUID();
        stubChangeCourtRoomForMultidayHearingFailure(hearing.hearingId.toString(), 422, "NO_SESSION_FOUND",
                "No court-schedule session found for hearingId " + hearing.hearingId);

        final String payload = changeCourtRoomPayload(hearing.courtCentreId, of(
                dayChange(hearing.day2, hearing.courtRoomId, targetScheduleD2)));

        final Response response = postChangeCourtRoom(hearing.hearingId, payload);

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("NO_SESSION_FOUND"));
        verifyChangeCourtRoomForMultidayHearingCalled(hearing.hearingId.toString());

        // No enriched command was ever sent - the viewstore must retain the original allocation.
        pollWithDefaults(requestParams(searchHearingUrl(hearing.hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(hearing.hearingId.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + hearing.day2 + "')].courtScheduleId",
                                        hasItem(hearing.scheduleD2.toString()))
                        )));
    }

    @Test
    void shouldReturn422WhenHearingIsNotCrown() {
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary(
                HearingsDataFactory.MAGISTRATES_JURISDICTION);
        final ListCourtHearingSteps seedSteps = new ListCourtHearingSteps(hearingsData);

        // MAGS listing calls courtscheduler during list-court-hearing; CROWN doesn't (mirrors
        // MoveHearingToPastDateIT.givenAListedHearing).
        final HearingData seedHearingData = hearingsData.getHearingData().get(0);
        final ZonedDateTime hearingStartTime = seedHearingData.getHearingStartTime();
        final String listedCourtScheduleId = UUID.randomUUID().toString();
        final Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingStartTime.toLocalDate().toString());
        stubParams.put("COURT_CENTRE_ID", seedHearingData.getCourtCentreId().toString());
        stubParams.put("COURT_SCHEDULE_ID", listedCourtScheduleId);
        stubParams.put("COURT_ROOM_ID", seedHearingData.getCourtRoomId().toString());
        stubParams.put("BOOKING_ID", UUID.randomUUID().toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        stubListHearingInCourtSessions(seedHearingData.getId().toString(), listedCourtScheduleId, hearingStartTime);

        seedSteps.whenCaseIsSubmittedForListing();
        final UUID hearingId = seedHearingData.getId();
        seedSteps.verifyHearingIsCreated(hearingId, 2);

        final UUID courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId();
        final UUID courtRoomId = hearingsData.getHearingData().get(0).getCourtRoomId();
        final String payload = changeCourtRoomPayload(courtCentreId, of(
                dayChange(ItClock.today().plusDays(30), courtRoomId, UUID.randomUUID())));

        final Response response = postChangeCourtRoom(hearingId, payload);

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("NOT_CROWN_HEARING"));
        verifyChangeCourtRoomForMultidayHearingNeverCalled(hearingId.toString());
    }

    @Test
    void shouldReturn422WhenHearingIdUnknown() {
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        final UUID unknownHearingId = UUID.randomUUID();
        final String payload = changeCourtRoomPayload(UUID.randomUUID(), of(
                dayChange(ItClock.today().plusDays(30), UUID.randomUUID(), UUID.randomUUID())));

        final Response response = postChangeCourtRoom(unknownHearingId, payload);

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("HEARING_ID_NOT_FOUND"));
        verifyChangeCourtRoomForMultidayHearingNeverCalled(unknownHearingId.toString());
    }

    // ---------------------------------------------------------------------------------------
    // Arrange helpers
    // ---------------------------------------------------------------------------------------

    /** An allocated 3-day CROWN hearing (d1, d2, d3), all in courtRoomId, each day's own
     * courtScheduleId known up front - built via the same extend-multiday seeding pattern as
     * {@code CrownUpdateHearingMultidayIT}: seed a real hearing via list-court-hearing, then
     * submit a raw multi-day CROWN update-hearing-for-listing (single nonDefaultDay spanning the
     * full duration, no courtScheduleId) so courtscheduler (stubbed) is the authority on the 3
     * session dates/ids/room. Days anchored on a Monday so d1/d2/d3 are consecutive weekdays. */
    private ThreeDayCrownHearing givenAllocatedThreeDayCrownHearing() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final UUID scheduleD1 = UUID.randomUUID();
        final UUID scheduleD2 = UUID.randomUUID();
        final UUID scheduleD3 = UUID.randomUUID();

        final LocalDate day1 = ItClock.today().plusDays(30).with(nextOrSame(MONDAY));
        final LocalDate day2 = day1.plusDays(1);
        final LocalDate day3 = day1.plusDays(2);

        final List<String> sessionScheduleIds = new ArrayList<>();
        sessionScheduleIds.add(scheduleD1.toString());
        sessionScheduleIds.add(scheduleD2.toString());
        sessionScheduleIds.add(scheduleD3.toString());

        stubExtendMultiDayHearing(hearingId.toString(), sessionScheduleIds, courtHouseId, courtRoomId, day1, false);
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        final ListCourtHearingSteps seedSteps = new ListCourtHearingSteps(HearingsData.hearingsData(hearingId));
        seedSteps.whenCaseIsSubmittedForListing();
        seedSteps.verifyHearingIsCreated(hearingId, 2);

        givenReferenceDataStubsForUpdateHearing(courtCentreId, courtRoomId);

        final String updatePayload = updateHearingForListingMultidayPayload(hearingId, courtCentreId, courtRoomId, scheduleD1, day1);
        restClient.postCommand(buildUrl(UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY, hearingId), MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                updatePayload, getLoggedInHeader());

        verifyExtendMultiDayHearingCalled(hearingId.toString(), MULTI_DAY_TOTAL_DURATION_MINUTES);

        final ResponseData allocatedResponse = pollWithDefaults(requestParams(searchHearingUrl(hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(hearingId.toString())),
                                withJsonPath("$.hearingDays", hasSize(3)),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + day1 + "')].courtScheduleId", hasItem(scheduleD1.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + day2 + "')].courtScheduleId", hasItem(scheduleD2.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + day3 + "')].courtScheduleId", hasItem(scheduleD3.toString()))
                        )));

        // Capture day 1's pre-change startTime/endTime (as actually serialised by the viewstore, per
        // uk.gov.moj.cpp.listing.query.view.hearing.HearingDay) so the happy-path test can prove
        // byte-identity of the untouched day after the courtroom change, without hardcoding the
        // stub-generated time format.
        final List<String> day1StartTimes = com.jayway.jsonpath.JsonPath.read(allocatedResponse.getPayload(),
                "$.hearingDays[?(@.hearingDate=='" + day1 + "')].startTime");
        final List<String> day1EndTimes = com.jayway.jsonpath.JsonPath.read(allocatedResponse.getPayload(),
                "$.hearingDays[?(@.hearingDate=='" + day1 + "')].endTime");
        // day-level courtCentreId is enriched during creation and can differ from the update
        // payload's courtCentreId — capture the actual pre-change value rather than assuming it.
        final List<String> day1CourtCentreIds = com.jayway.jsonpath.JsonPath.read(allocatedResponse.getPayload(),
                "$.hearingDays[?(@.hearingDate=='" + day1 + "')].courtCentreId");

        return new ThreeDayCrownHearing(hearingId, courtCentreId, courtRoomId, day1, day2, day3,
                scheduleD1, scheduleD2, scheduleD3, day1StartTimes.get(0), day1EndTimes.get(0),
                day1CourtCentreIds.get(0));
    }

    private static void givenReferenceDataStubsForUpdateHearing(final UUID courtCentreId, final UUID courtRoomId) {
        final CourtCentreData courtCentreData = new CourtCentreData(
                courtCentreId,
                LocalTime.of(10, 30),
                "6:30",
                courtRoomId,
                "Test Court Centre");
        stubGetReferenceDataCourtCentre(courtCentreData);
        stubGetReferenceDataCourtCentreById(courtCentreData);
        stubGetReferenceDataCourtMappings(courtCentreData);
        stubGetReferenceDataHearingTypes(UUID.randomUUID());
        // The event processor's public hearing-confirmed V2 factory resolves the hearing's court
        // centre via referencedata organisation-units/{id}. Without this stub the requester
        // returns a NULL-payload envelope and the processor rollback-redelivers 10x into the DLQ.
        stubGetReferenceDataOrganisationUnitById(courtCentreId);
    }

    private static String updateHearingForListingMultidayPayload(final UUID hearingId,
                                                                   final UUID courtCentreId,
                                                                   final UUID courtRoomId,
                                                                   final UUID startingCourtScheduleId,
                                                                   final LocalDate startDate) {
        final LocalDate endDate = startDate.plusDays(57);
        final String startTime = startDate + "T09:00:00Z";
        return "{"
                + "\"courtCentreId\":\"" + courtCentreId + "\","
                + "\"courtRoomId\":\"" + courtRoomId + "\","
                + "\"selectedCourtCentre\":{"
                + "\"id\":\"" + courtCentreId + "\","
                + "\"courtRoomId\":\"" + courtRoomId + "\","
                + "\"courtCentreName\":\"Test Court Centre\","
                + "\"ouCode\":\"B01LY00\"},"
                + "\"type\":{\"id\":\"4a0e892d-c0c5-3c51-95b8-704d8c781776\",\"description\":\"Plea\"},"
                + "\"startDate\":\"" + startDate + "\","
                + "\"endDate\":\"" + endDate + "\","
                + "\"nonSittingDays\":[],"
                + "\"nonDefaultDays\":[{"
                + "\"startTime\":\"" + startTime + "\","
                + "\"courtCentreId\":\"" + courtCentreId + "\","
                + "\"roomId\":\"" + courtRoomId + "\","
                + "\"duration\":" + MULTI_DAY_TOTAL_DURATION_MINUTES
                + "}],"
                + "\"judiciary\":[],"
                + "\"jurisdictionType\":\"CROWN\","
                + "\"hearingLanguage\":\"ENGLISH\","
                + "\"publicListNote\":\"\","
                + "\"hasVideoLink\":false,"
                + "\"sendNotificationToParties\":false}";
    }

    private static DayChange dayChange(final LocalDate date, final UUID courtRoomId, final UUID targetCourtScheduleId) {
        return new DayChange(date, courtRoomId, targetCourtScheduleId);
    }

    private static String changeCourtRoomPayload(final UUID courtCentreId, final List<DayChange> days) {
        final JsonArrayBuilder nonDefaultDays = Json.createArrayBuilder();
        for (final DayChange day : days) {
            final JsonObjectBuilder dayBuilder = Json.createObjectBuilder()
                    .add("startTime", day.date + "T09:00:00Z")
                    .add("duration", DAY_DURATION_MINUTES)
                    .add("courtCentreId", courtCentreId.toString())
                    .add("roomId", day.courtRoomId.toString())
                    .add("courtScheduleId", day.targetCourtScheduleId.toString())
                    .add("virtual", true);
            nonDefaultDays.add(dayBuilder);
        }
        return Json.createObjectBuilder()
                .add("sendNotificationToParties", true)
                .add("nonDefaultDays", nonDefaultDays)
                .build()
                .toString();
    }

    private Response postChangeCourtRoom(final UUID hearingId, final String payload) {
        return restClient.postCommand(buildUrl(CHANGE_COURT_ROOM_ENDPOINT_KEY, hearingId), MEDIA_TYPE_CHANGE_COURT_ROOM,
                payload, getLoggedInHeader());
    }

    private static String buildUrl(final String endpointKey, final UUID hearingId) {
        return String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(endpointKey), hearingId.toString()));
    }

    private static String searchHearingUrl(final UUID hearingId) {
        return String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId.toString()));
    }

    private record ThreeDayCrownHearing(UUID hearingId, UUID courtCentreId, UUID courtRoomId,
                                         LocalDate day1, LocalDate day2, LocalDate day3,
                                         UUID scheduleD1, UUID scheduleD2, UUID scheduleD3,
                                         String day1StartTime, String day1EndTime,
                                         String day1CourtCentreId) {
    }

    private record DayChange(LocalDate date, UUID courtRoomId, UUID targetCourtScheduleId) {
    }
}
