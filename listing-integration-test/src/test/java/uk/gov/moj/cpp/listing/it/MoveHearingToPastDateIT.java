package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollUntilHearingIsPresent;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingToPastDate;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingToPastDateFailure;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMoveHearingToPastDateCalled;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMoveHearingToPastDateNeverCalled;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsForSchedules;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMultiDaySearchAndBookForHearing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMultiDaySearchAndBookCalledForHearing;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataOrganisationUnitById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previous;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.MoveHearingToPastDateSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.MoveToPastDateStubSession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

/**
 * Covers listing.command.move-hearing-to-past-date: both MAGISTRATES and CROWN wrap courtscheduler
 * synchronously and store the returned slot as enrichment. The wire contract is aligned with main
 * (the review artifact this reconciles): {@code courtCentreId}/{@code courtRoomId}/
 * {@code startDateTime}/{@code endDateTime} are all mandatory UTC-instant/UUID fields. Date
 * validation (FUTURE_DATE_NOT_ALLOWED including today, INVALID_DATE, INVALID_DATE_RANGE,
 * START_DATE_TOO_OLD) is ported from main and applied by LISTING ITSELF, uniformly for BOTH
 * jurisdictions, before courtscheduler is ever called - EXCEPT main's MULTI_DAY_NOT_ALLOWED, which
 * is deliberately NOT ported so SPRDT-1333's multi-day CROWN payback (via an explicit date range or
 * the hearing's duration estimate) keeps working. courtscheduler is WireMock here, so the pay-back
 * of the prior allocation is asserted in courtscheduler's own MoveHearingToPastDateIT; these tests
 * prove listing validates up front, asks for the move (CROWN jurisdiction on the request), and
 * re-dates the hearing from the response, for single- and multi-day.
 */
class MoveHearingToPastDateIT extends AbstractIT {

    private static final String COURT_ROOM_ID = "731816c1-27ea-4711-8d92-0a1c2f3ab7de";
    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING = "application/vnd.listing.command.update-hearing-for-listing+json";
    private static final String MEDIA_TYPE_SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    private static final String UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY = "listing.command.update-hearing-for-listing";
    private static final String LISTING_QUERY_HEARING = "listing.search.hearing";
    /** 3 x 360-minute days - the same multi-day CROWN seeding shape as ChangeCourtRoomForMultidayHearingIT. */
    private static final int MULTI_DAY_TOTAL_DURATION_MINUTES = 1080;
    private static final int DAY_DURATION_MINUTES = 360;

    /**
     * Lists a real hearing through the full flow (command → events → viewstore projection) and only
     * returns once it is queryable — the move command's HEARING_ID_NOT_FOUND pre-check reads the
     * viewstore, so moving an un-listed hearing is legitimately rejected. Mirrors VacateHearingIT:
     * MAGS listing needs the provisional-booking + list-hearing-in-court-sessions stubs; CROWN
     * listing never calls courtscheduler pre-Phase-2.
     */
    private MoveHearingToPastDateSteps givenAListedHearing(final String jurisdiction) {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(jurisdiction);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);

        if (MAGISTRATES_JURISDICTION.equals(jurisdiction)) {
            final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
            final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();
            final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
            final String listedCourtScheduleId = randomUUID().toString();

            final Map<String, String> stubParams = new HashMap<>();
            stubParams.put("SESSION_DATE", hearingStartTime.toLocalDate().toString());
            stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
            stubParams.put("COURT_SCHEDULE_ID", listedCourtScheduleId);
            stubParams.put("COURT_ROOM_ID", courtroomId.toString());
            stubParams.put("BOOKING_ID", randomUUID().toString());
            stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
            stubProvisionalBookingWithCustomParams(stubParams);
            stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                    listedCourtScheduleId, hearingStartTime);
        }

        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        // verifyHearingListedFromAPI's indefinite json-path filters have no result matcher, so
        // they match vacuously against an empty hearings list - it can return before THIS hearing
        // is projected. Poll on the hearing id (hasSize(1)) so the move command's viewstore
        // pre-check cannot race the hearing-listed projection and 422 with HEARING_ID_NOT_FOUND.
        pollUntilHearingIsPresent(hearingsData.getHearingData().get(0).getCourtCentreId().toString(),
                ALLOCATED, getLoggedInUser().toString(), hearingsData.getHearingData().get(0).getId().toString());

        return new MoveHearingToPastDateSteps(hearingsData);
    }

    @Test
    void shouldMoveMagistratesHearingToPastDateAndStoreCourtScheduleEnrichment() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        final LocalDate pastDate = ItClock.today().minusDays(1);
        final String courtScheduleId = randomUUID().toString();
        stubMoveHearingToPastDate(moveSteps.getHearingId(), courtScheduleId, COURT_ROOM_ID, pastDate);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", pastDate);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingToPastDateCalled(moveSteps.getHearingId());
        moveSteps.verifyCourtScheduleStored(courtScheduleId);
        moveSteps.verifyStartAndEndDateUpdated(pastDate, pastDate);
    }

    @Test
    void shouldReleasePriorAllocationWhenMagistratesHearingMovedAgain() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);
        final LocalDate pastDate = ItClock.today().minusDays(1);

        final String firstCourtScheduleId = randomUUID().toString();
        stubMoveHearingToPastDate(moveSteps.getHearingId(), firstCourtScheduleId, COURT_ROOM_ID, pastDate);
        assertThat(moveSteps.whenHearingIsMovedToPastDate("MAGS", pastDate).getStatus(), is(ACCEPTED.getStatusCode()));
        moveSteps.verifyCourtScheduleStored(firstCourtScheduleId);

        final String secondCourtScheduleId = randomUUID().toString();
        stubMoveHearingToPastDate(moveSteps.getHearingId(), secondCourtScheduleId, COURT_ROOM_ID, pastDate);
        assertThat(moveSteps.whenHearingIsMovedToPastDate("MAGS", pastDate).getStatus(), is(ACCEPTED.getStatusCode()));
        moveSteps.verifyCourtScheduleStored(secondCourtScheduleId);
    }

    /**
     * Renamed/refactored (rule 3): FUTURE_DATE_NOT_ALLOWED validation is now ported from main and
     * applied by LISTING ITSELF, uniformly for both jurisdictions, before courtscheduler is ever
     * called - MAGS previously had no listing-side date check at all and relied on courtscheduler
     * rejecting it (the premise this test used to exercise no longer occurs).
     */
    @Test
    void shouldRejectMagistratesMoveToFutureDateWithoutCallingCourtScheduler() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", ItClock.today().plusDays(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("FUTURE_DATE_NOT_ALLOWED"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    /** SPRDT-1185, now ported to apply uniformly: today itself is rejected too, not just future dates. */
    @Test
    void shouldRejectMagistratesMoveToTodayWith422() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", ItClock.today());

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("FUTURE_DATE_NOT_ALLOWED"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    @Test
    void shouldRejectCrownMoveToTodayWith422() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(CROWN_JURISDICTION);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", ItClock.today());

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("FUTURE_DATE_NOT_ALLOWED"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    /**
     * An impossible calendar date (June 31st), not garbage text - the request schema's pattern only
     * checks digit shape, so this passes the framework's 400 schema gate and reaches
     * ListingCommandApi's own parseInstant, which raises the 422 INVALID_DATE this test exercises.
     */
    @Test
    void shouldRejectMoveWith422InvalidDateWhenStartDateTimeIsAnImpossibleCalendarDate() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        final Response response = moveSteps.whenHearingIsMovedWithImpossibleCalendarStartDateTime(ItClock.today().minusDays(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("INVALID_DATE"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    @Test
    void shouldRejectMoveWith422WhenEndDateTimeIsEarlierThanStartDateTime() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);
        final LocalDate startDate = ItClock.today().minusDays(1);

        final Response response = moveSteps.whenHearingIsMovedToPastDateRange("MAGS", startDate, startDate.minusDays(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("INVALID_DATE_RANGE"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    @Test
    void shouldRejectMoveWith422WhenStartDateIsMoreThanSixMonthsInThePast() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(CROWN_JURISDICTION);
        final LocalDate tooOld = ItClock.today().minusMonths(7);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", tooOld);

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("START_DATE_TOO_OLD"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    @Test
    void shouldRejectMagistratesMoveWith422WhenNoCourtScheduleSessionExists() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        stubMoveHearingToPastDateFailure(moveSteps.getHearingId(), 422, "NO_SESSION_FOUND",
                "No court-schedule session found for the given date and court centre");

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", ItClock.today().minusDays(1));

        assertThat(response.getStatus(), is(422));
        final String responseBody = response.readEntity(String.class);
        assertThat(responseBody, containsString("NO_SESSION_FOUND"));
        // Fixed user-facing copy (ported from main) replaces courtscheduler's own diagnostic message.
        assertThat(responseBody, containsString("No suitable sessions are available for the selected date. Please select another date."));
    }

    /** Older courtscheduler releases signalled no-session as a bare 404 - the listing adapter
     * normalises that to the 422 NO_SESSION_FOUND contract, with the fixed user-facing message. */
    @Test
    void shouldNormaliseLegacyCourtscheduler404ToA422NoSessionFound() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        stubMoveHearingToPastDateFailure(moveSteps.getHearingId(), 404, null,
                "No court-schedule session found for the given date and court centre");

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", ItClock.today().minusDays(1));

        assertThat(response.getStatus(), is(422));
        final String responseBody = response.readEntity(String.class);
        assertThat(responseBody, containsString("NO_SESSION_FOUND"));
        assertThat(responseBody, containsString("No suitable sessions are available for the selected date. Please select another date."));
    }

    @Test
    void shouldRejectMoveWith422WhenHearingIdUnknown() {
        // A hearing that was never listed - MoveHearingToPastDateSteps still needs SOME allocated
        // hearing to obtain a courtCentreId, but we submit against a random unknown hearingId.
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);
        final UUID unknownHearingId = randomUUID();

        final Response response = moveSteps.whenHearingIsMovedToPastDateForHearing(unknownHearingId, ItClock.today().minusDays(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("HEARING_ID_NOT_FOUND"));
        verifyMoveHearingToPastDateNeverCalled(unknownHearingId.toString());
    }

    @Test
    void shouldRejectMoveWith400WhenMandatoryFieldMissing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedWithMissingCourtCentre(ItClock.today().minusDays(1));

        assertThat(response.getStatus(), is(400));
    }

    /** courtRoomId is now mandatory (main-contract alignment - the review artifact this reconciles). */
    @Test
    void shouldRejectMoveWith400WhenCourtRoomIdMissing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedWithMissingCourtRoom(ItClock.today().minusDays(1));

        assertThat(response.getStatus(), is(400));
    }

    /** endDateTime is now mandatory alongside startDateTime (main-contract alignment). */
    @Test
    void shouldRejectMoveWith400WhenEndDateTimeMissing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedWithMissingEndDateTime(ItClock.today().minusDays(1));

        assertThat(response.getStatus(), is(400));
    }

    @Test
    void shouldMoveCrownHearingToPastDateViaCourtSchedulerAndStoreCourtScheduleEnrichment() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(CROWN_JURISDICTION);
        final LocalDate pastDate = ItClock.today().minusDays(1);
        final String courtScheduleId = randomUUID().toString();
        stubMoveHearingToPastDate(moveSteps.getHearingId(), courtScheduleId, COURT_ROOM_ID, pastDate);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", pastDate);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        // courtscheduler must be told this is a CROWN hearing (not the historical MAGISTRATES default) so
        // it applies the CROWN booking rules and pays back the hearing's prior allocation.
        verifyMoveHearingToPastDateCalled(moveSteps.getHearingId(), "CROWN", null);
        moveSteps.verifyCourtScheduleStored(courtScheduleId);
        moveSteps.verifyStartAndEndDateUpdated(pastDate, pastDate);
    }

    /**
     * A CROWN hearing already allocated to a 3-day FUTURE block is moved to a past date. Listing must
     * ask courtscheduler to move it (CROWN jurisdiction, the hearing's full duration - courtscheduler
     * releases EVERY prior day's allocation and books the consecutive past run; the pay-back itself is
     * asserted against a real DB in courtscheduler's own MoveHearingToPastDateIT). Listing then re-dates
     * the hearing to the first booked past session and drops the old future days/schedules.
     */
    @Test
    void shouldMoveMultiDayCrownHearingToPastDateViaCourtSchedulerReleasingTheFutureBlock() {
        final ThreeDayCrownHearing hearing = givenAllocatedThreeDayCrownHearing();
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearing.seedData);

        // Monday 4 weeks back so the 3 past sessions are consecutive weekdays, mirroring the future block.
        final LocalDate pastDay1 = ItClock.today().minusWeeks(4).with(previous(MONDAY));
        final String pastScheduleD1 = randomUUID().toString();
        final String pastScheduleD2 = randomUUID().toString();
        final String pastScheduleD3 = randomUUID().toString();
        stubMoveHearingToPastDate(hearing.hearingId.toString(), List.of(
                new MoveToPastDateStubSession(pastScheduleD1, COURT_ROOM_ID, pastDay1),
                new MoveToPastDateStubSession(pastScheduleD2, COURT_ROOM_ID, pastDay1.plusDays(1)),
                new MoveToPastDateStubSession(pastScheduleD3, COURT_ROOM_ID, pastDay1.plusDays(2))));

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", pastDay1);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingToPastDateCalled(hearing.hearingId.toString(), "CROWN", null);

        // end date follows the LAST booked past session (3 consecutive days from pastDay1)
        moveSteps.verifyStartAndEndDateUpdated(pastDay1, pastDay1.plusDays(2));
        // The hearing keeps THREE days - one per session courtscheduler booked - each on its own past
        // date with its own courtScheduleId and a third of the hearing's 1080 minutes; the future block
        // is gone entirely.
        pollWithDefaults(requestParams(searchHearingUrl(hearing.hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(hearing.hearingId.toString())),
                                withJsonPath("$.hearingDays", hasSize(3)),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + pastDay1 + "')].courtScheduleId", hasItem(pastScheduleD1)),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + pastDay1.plusDays(1) + "')].courtScheduleId", hasItem(pastScheduleD2)),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + pastDay1.plusDays(2) + "')].courtScheduleId", hasItem(pastScheduleD3)),
                                withJsonPath("$.hearingDays[*].durationMinutes", everyItem(is(DAY_DURATION_MINUTES))),
                                withJsonPath("$.hearingDays[*].isDraft", everyItem(is(false))),
                                // endTime = 09:00 start + 360 mins, not the session's 17:00 closing time
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + pastDay1 + "')].endTime", hasItem(startsWith(pastDay1 + "T15:00"))),
                                // hearing-level room follows the room courtscheduler booked the run in
                                withJsonPath("$.courtRoomId", is(COURT_ROOM_ID)),
                                withJsonPath("$.hearingDays[*].courtScheduleId", not(hasItem(hearing.scheduleD1.toString()))),
                                withJsonPath("$.hearingDays[*].courtScheduleId", not(hasItem(hearing.scheduleD2.toString()))),
                                withJsonPath("$.hearingDays[*].courtScheduleId", not(hasItem(hearing.scheduleD3.toString()))),
                                withJsonPath("$.hearingDays[*].hearingDate", not(hasItem(hearing.day1.toString()))),
                                withJsonPath("$.estimatedMinutes", is(MULTI_DAY_TOTAL_DURATION_MINUTES))
                        )));
    }

    /**
     * Regression guard (rule 3 decision): an explicit multi-day range - startDateTime/endDateTime on
     * DIFFERENT dates, driving courtscheduler's day-count directly rather than via the hearing's
     * duration estimate - must be accepted, not rejected as MULTI_DAY_NOT_ALLOWED (main's check,
     * deliberately NOT ported so SPRDT-1333's multi-day CROWN payback keeps working).
     */
    @Test
    void shouldMoveMultiDayCrownHearingWithExplicitEndDateToPastDateRange() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(CROWN_JURISDICTION);
        final LocalDate pastDay1 = ItClock.today().minusWeeks(4).with(previous(MONDAY));
        final LocalDate pastDay3 = pastDay1.plusDays(2);
        final String pastScheduleD1 = randomUUID().toString();
        final String pastScheduleD2 = randomUUID().toString();
        final String pastScheduleD3 = randomUUID().toString();
        stubMoveHearingToPastDate(moveSteps.getHearingId(), List.of(
                new MoveToPastDateStubSession(pastScheduleD1, COURT_ROOM_ID, pastDay1),
                new MoveToPastDateStubSession(pastScheduleD2, COURT_ROOM_ID, pastDay1.plusDays(1)),
                new MoveToPastDateStubSession(pastScheduleD3, COURT_ROOM_ID, pastDay3)));

        final Response response = moveSteps.whenHearingIsMovedToPastDateRange("CROWN", pastDay1, pastDay3);

        // ACCEPTED itself is the proof this wasn't rejected as MULTI_DAY_NOT_ALLOWED (that check
        // doesn't exist on this path) - a rejection would be a 422, not 202. Other passing tests in
        // this suite never call readEntity() on a 202 response; its entity stream is already closed
        // by the framework once the status is read (RESTEASY003765 otherwise).
        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingToPastDateCalled(moveSteps.getHearingId(), "CROWN", null);
        moveSteps.verifyStartAndEndDateUpdated(pastDay1, pastDay3);
    }

    @Test
    void shouldRejectCrownMoveToFutureDateWithoutCallingCourtScheduler() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(CROWN_JURISDICTION);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", ItClock.today().plusDays(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("FUTURE_DATE_NOT_ALLOWED"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    /**
     * An allocated 3-day CROWN hearing (day1..day3, one courtScheduleId each) built via the same
     * seeding pattern as ChangeCourtRoomForMultidayHearingIT: list a real hearing, then submit a
     * multi-day CROWN update-hearing-for-listing so courtscheduler (stubbed crown.search.and.book) is
     * the authority on the 3 session dates/ids, and wait for all 3 days to be projected.
     */
    private ThreeDayCrownHearing givenAllocatedThreeDayCrownHearing() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = UUID.fromString(COURT_ROOM_ID);
        final UUID courtHouseId = randomUUID();
        final UUID scheduleD1 = randomUUID();
        final UUID scheduleD2 = randomUUID();
        final UUID scheduleD3 = randomUUID();

        final LocalDate day1 = ItClock.today().plusDays(30).with(nextOrSame(MONDAY));

        final List<String> sessionScheduleIds = new ArrayList<>();
        sessionScheduleIds.add(scheduleD1.toString());
        sessionScheduleIds.add(scheduleD2.toString());
        sessionScheduleIds.add(scheduleD3.toString());

        stubMultiDaySearchAndBookForHearing(hearingId.toString(), sessionScheduleIds, courtHouseId, courtRoomId, day1, false);
        stubListHearingInCourtSessionsForSchedules(hearingId.toString(), sessionScheduleIds,
                day1.atTime(9, 0).atZone(ZoneOffset.UTC), DAY_DURATION_MINUTES);
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        final HearingsData seedData = HearingsData.hearingsData(hearingId);
        final ListCourtHearingSteps seedSteps = new ListCourtHearingSteps(seedData);
        seedSteps.whenCaseIsSubmittedForListing();
        seedSteps.verifyHearingIsCreated(hearingId, 2);

        givenReferenceDataStubsForUpdateHearing(courtCentreId, courtRoomId);

        final String updatePayload = updateHearingForListingMultidayPayload(hearingId, courtCentreId, courtRoomId, day1);
        restClient.postCommand(buildUrl(UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY, hearingId), MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                updatePayload, getLoggedInHeader());

        verifyMultiDaySearchAndBookCalledForHearing(hearingId.toString(), MULTI_DAY_TOTAL_DURATION_MINUTES);

        pollWithDefaults(requestParams(searchHearingUrl(hearingId), MEDIA_TYPE_SEARCH_HEARING)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(hearingId.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + day1 + "')].courtScheduleId", hasItem(scheduleD1.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + day1.plusDays(1) + "')].courtScheduleId", hasItem(scheduleD2.toString())),
                                withJsonPath("$.hearingDays[?(@.hearingDate=='" + day1.plusDays(2) + "')].courtScheduleId", hasItem(scheduleD3.toString()))
                        )));

        return new ThreeDayCrownHearing(hearingId, seedData, day1, scheduleD1, scheduleD2, scheduleD3);
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
        stubGetReferenceDataHearingTypes(randomUUID());
        // The event processor resolves the hearing's court centre via referencedata organisation-units/{id};
        // without this stub it rollback-redelivers the allocation events 10x into the DLQ.
        stubGetReferenceDataOrganisationUnitById(courtCentreId);
        // The command API's CROWN nonDefaultDays enrichment (HearingDaysEnrichmentService.getCpCourtRoomNumber)
        // touches the application-scoped xhibit ReferenceDataCache, whose @PostConstruct loads the
        // organisation-units LIST. Stub it here so this test does not depend on an earlier test having
        // warmed the cache (otherwise: WELD-000049 / 'Cannot find organisationunits' -> 500 on the update).
        stubOrganisationUnit(courtCentreId);
    }

    private static String updateHearingForListingMultidayPayload(final UUID hearingId,
                                                                   final UUID courtCentreId,
                                                                   final UUID courtRoomId,
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

    private static String buildUrl(final String endpointKey, final UUID hearingId) {
        return String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(endpointKey), hearingId.toString()));
    }

    private static String searchHearingUrl(final UUID hearingId) {
        return String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId.toString()));
    }

    private record ThreeDayCrownHearing(UUID hearingId, HearingsData seedData, LocalDate day1,
                                         UUID scheduleD1, UUID scheduleD2, UUID scheduleD3) {
    }
}
