package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
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
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCrownCourtCentreById;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.MoveHearingToPastDateSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

/**
 * Covers listing.command.move-hearing-to-past-date: MAGISTRATES wraps courtscheduler synchronously
 * and stores the returned slot as enrichment; CROWN is listing-side-only (Baris decision D1) and
 * never calls courtscheduler. Single-day only.
 */
class MoveHearingToPastDateIT extends AbstractIT {

    private static final String COURT_ROOM_ID = "731816c1-27ea-4711-8d92-0a1c2f3ab7de";

    /**
     * Lists a real hearing through the full flow (command → events → viewstore projection) and only
     * returns once it is queryable — the move re-issues the hearing's EXISTING days onto the new date,
     * so the aggregate must already hold hearing days (an un-listed hearing is a silent no-op). Mirrors
     * VacateHearingIT: MAGS listing needs the provisional-booking + list-hearing-in-court-sessions
     * stubs; CROWN listing never calls courtscheduler pre-Phase-2.
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
        } else if (CROWN_JURISDICTION.equals(jurisdiction)) {
            // The move derives jurisdiction from the target court centre's oucodeL1Name; stub this CROWN
            // hearing's court centre as a Crown court centre so the move stays listing-side (no courtscheduler).
            stubGetReferenceDataCrownCourtCentreById(
                    listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId(),
                    listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId());
        }

        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        // verifyHearingListedFromAPI's indefinite json-path filters have no result matcher, so
        // they match vacuously against an empty hearings list - it can return before THIS hearing
        // is projected. Poll on the hearing id (hasSize(1)) so the move's day re-assignment cannot
        // race the hearing-listed projection (the aggregate needs its existing hearing days first).
        pollUntilHearingIsPresent(hearingsData.getHearingData().get(0).getCourtCentreId().toString(),
                ALLOCATED, getLoggedInUser().toString(), hearingsData.getHearingData().get(0).getId().toString());

        return new MoveHearingToPastDateSteps(hearingsData);
    }

    @Test
    void shouldMoveMagistratesHearingToPastDateAndStoreCourtScheduleEnrichment() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        final LocalDate pastDate = pastWorkingDay(1);
        final String courtScheduleId = randomUUID().toString();
        stubMoveHearingToPastDate(moveSteps.getHearingId(), courtScheduleId, COURT_ROOM_ID, pastDate, 30);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", pastDate);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingToPastDateCalled(moveSteps.getHearingId());
        moveSteps.verifyCourtScheduleStored(courtScheduleId);
    }

    @Test
    void shouldReleasePriorAllocationWhenMagistratesHearingMovedAgain() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);
        final LocalDate pastDate = pastWorkingDay(1);

        final String firstCourtScheduleId = randomUUID().toString();
        stubMoveHearingToPastDate(moveSteps.getHearingId(), firstCourtScheduleId, COURT_ROOM_ID, pastDate, 30);
        assertThat(moveSteps.whenHearingIsMovedToPastDate("MAGS", pastDate).getStatus(), is(ACCEPTED.getStatusCode()));
        moveSteps.verifyCourtScheduleStored(firstCourtScheduleId);

        final String secondCourtScheduleId = randomUUID().toString();
        stubMoveHearingToPastDate(moveSteps.getHearingId(), secondCourtScheduleId, COURT_ROOM_ID, pastDate, 30);
        assertThat(moveSteps.whenHearingIsMovedToPastDate("MAGS", pastDate).getStatus(), is(ACCEPTED.getStatusCode()));
        moveSteps.verifyCourtScheduleStored(secondCourtScheduleId);
    }

    @Test
    void shouldRejectMagistratesMoveWith422WhenNoCourtScheduleSessionExists() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        stubMoveHearingToPastDateFailure(moveSteps.getHearingId(), 422, "NO_SESSION_FOUND",
                "No court-schedule session found for the given date and court centre");

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", pastWorkingDay(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("NO_SESSION_FOUND"));
    }

    /** Older courtscheduler releases signalled no-session as a bare 404 - the listing adapter
     * normalises that to the 422 NO_SESSION_FOUND contract. */
    @Test
    void shouldNormaliseLegacyCourtscheduler404ToA422NoSessionFound() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        stubMoveHearingToPastDateFailure(moveSteps.getHearingId(), 404, null,
                "No court-schedule session found for the given date and court centre");

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", pastWorkingDay(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("NO_SESSION_FOUND"));
    }

    @Test
    void shouldRejectMoveWith400WhenMandatoryFieldMissing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedWithMissingCourtCentre(pastWorkingDay(1));

        assertThat(response.getStatus(), is(400));
    }

    @Test
    void shouldRejectMoveWith400WhenCourtRoomIdMissing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedWithMissingCourtRoom(pastWorkingDay(1));

        assertThat(response.getStatus(), is(400));
    }

    @Test
    void shouldRejectMoveWith400WhenStartTimeMissing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedWithMissingStartTime();

        assertThat(response.getStatus(), is(400));
    }

    @Test
    void shouldMoveCrownHearingToPastDateListingSideOnlyWithoutCallingCourtScheduler() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(CROWN_JURISDICTION);
        final LocalDate pastDate = pastWorkingDay(1);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", pastDate);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
        moveSteps.verifyStartDateUpdated(pastDate);
    }

    /** This endpoint moves hearings to earlier dates only - a future date is rejected synchronously
     * with 422 FUTURE_DATE_NOT_ALLOWED before any event is sent or courtscheduler call is made. */
    @Test
    void shouldRejectMoveToFutureDateWith422() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(CROWN_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);
        final LocalDate futureDate = futureWorkingDay(1);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", futureDate);

        assertThat(response.getStatus(), is(422));
        final String body = response.readEntity(String.class);
        assertThat(body, containsString("FUTURE_DATE_NOT_ALLOWED"));
        assertThat(body, containsString("Hearings can only be moved to an earlier date"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    /** Today is no longer a valid target - a hearing can only be moved to a date strictly before
     * today. Rejected synchronously with 422 FUTURE_DATE_NOT_ALLOWED before any event or
     * courtscheduler call, for the same reason a future date is. */
    @Test
    void shouldRejectMoveToTodayWith422() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(CROWN_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", ItClock.today());

        assertThat(response.getStatus(), is(422));
        final String body = response.readEntity(String.class);
        assertThat(body, containsString("FUTURE_DATE_NOT_ALLOWED"));
        assertThat(body, containsString("Hearings can only be moved to an earlier date"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    /** The target hearing must already exist in the listing viewstore. A move against a hearingId
     * that was never listed is rejected synchronously with 422 HEARING_ID_NOT_FOUND (via
     * hearingLookupService) before any court-centre lookup, courtscheduler call, or event - even
     * though the date itself is a valid past working day. */
    @Test
    void shouldRejectMoveWhenHearingDoesNotExistInViewstoreWith422() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);
        final UUID unknownHearingId = randomUUID();

        final Response response = moveSteps.whenHearingIsMovedToPastDateForHearing(unknownHearingId, pastWorkingDay(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("HEARING_ID_NOT_FOUND"));
        verifyMoveHearingToPastDateNeverCalled(unknownHearingId.toString());
    }

    /** Courts do not sit at weekends, so a weekend date can never have a bookable session. Rejected
     * synchronously with the same 422 NO_SESSION_FOUND fixed copy as the courtscheduler no-slot case
     * (uniform failure shape for the caller), before any event or courtscheduler call. */
    @Test
    void shouldRejectWeekendMoveWithNoSessionFoundFixedCopy() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);
        final LocalDate saturday = mostRecentSaturday();

        final Response response = moveSteps.whenHearingIsMovedToPastDateRange(saturday, saturday, COURT_ROOM_ID);

        assertThat(response.getStatus(), is(422));
        final String body = response.readEntity(String.class);
        assertThat(body, containsString("NO_SESSION_FOUND"));
        assertThat(body, containsString(
                "No suitable sessions are available for the selected date. Please select another date."));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    /** The request schema's regex only range-checks digits, so an impossible calendar date such as
     * 2026-06-31 passes schema validation - the handler rejects it with a clean 422 INVALID_DATE
     * instead of an unhandled 500. */
    @Test
    void shouldRejectImpossibleCalendarDateWith422InvalidDate() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedWithRawTimes(
                "2026-06-31T10:30:00.000Z", "2026-06-31T10:50:00.000Z");

        assertThat(response.getStatus(), is(422));
        final String body = response.readEntity(String.class);
        assertThat(body, containsString("INVALID_DATE"));
        assertThat(body, containsString("startDateTime is not a valid date"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    /** endDateTime earlier than startDateTime (same day, negative window) is rejected with 422
     * INVALID_DATE_RANGE - the comparison is on the full instants, not just the dates. */
    @Test
    void shouldRejectEndTimeEarlierThanStartTimeWith422() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);
        final LocalDate pastDate = pastWorkingDay(1);

        final Response response = moveSteps.whenHearingIsMovedWithRawTimes(
                pastDate + "T10:50:00.000Z", pastDate + "T10:30:00.000Z");

        assertThat(response.getStatus(), is(422));
        final String body = response.readEntity(String.class);
        assertThat(body, containsString("INVALID_DATE_RANGE"));
        assertThat(body, containsString("endDateTime must not be earlier than startDateTime"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    /** Multi-day moves are not allowed - startDateTime and endDateTime must fall on the same date.
     * Rejected synchronously with 422 MULTI_DAY_NOT_ALLOWED for ALL jurisdictions, before any event
     * is sent or courtscheduler call is made. */
    @Test
    void shouldRejectMultiDayMoveWith422() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);
        final LocalDate startDate = pastWorkingDay(2);
        final LocalDate endDate = pastWorkingDay(1);

        final Response response = moveSteps.whenHearingIsMovedToPastDateRange(startDate, endDate, COURT_ROOM_ID);

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("MULTI_DAY_NOT_ALLOWED"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    @Test
    void shouldMoveMagistratesHearingWithinTheRequestedRoom() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);
        final LocalDate pastDate = pastWorkingDay(1);
        final String courtScheduleId = randomUUID().toString();
        stubMoveHearingToPastDate(moveSteps.getHearingId(), courtScheduleId, COURT_ROOM_ID, pastDate, 30);

        final Response response = moveSteps.whenHearingIsMovedToPastDateRange(pastDate, pastDate, COURT_ROOM_ID);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingToPastDateCalled(moveSteps.getHearingId());
        moveSteps.verifyCourtScheduleStored(courtScheduleId);
    }

    /** n-th working (Mon-Fri) day strictly before ItClock.today() - keeps past-date moves off weekends. */
    private static LocalDate pastWorkingDay(final int n) {
        LocalDate day = ItClock.today();
        int found = 0;
        while (found < n) {
            day = day.minusDays(1);
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                found++;
            }
        }
        return day;
    }

    /** n-th working (Mon-Fri) day strictly after ItClock.today() - keeps future-date moves off weekends. */
    private static LocalDate futureWorkingDay(final int n) {
        LocalDate day = ItClock.today();
        int found = 0;
        while (found < n) {
            day = day.plusDays(1);
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                found++;
            }
        }
        return day;
    }

    /** most recent Saturday strictly before ItClock.today() - always past, single-day, within 6 months,
     * so only the weekend (no-sitting-day) rule can reject it. */
    private static LocalDate mostRecentSaturday() {
        LocalDate day = ItClock.today().minusDays(1);
        while (day.getDayOfWeek() != DayOfWeek.SATURDAY) {
            day = day.minusDays(1);
        }
        return day;
    }
}
