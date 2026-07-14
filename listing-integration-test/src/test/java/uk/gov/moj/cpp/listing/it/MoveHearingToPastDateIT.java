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
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingToPastDateMultiDay;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingToPastDateFailure;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMoveHearingToPastDateCalled;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMoveHearingToPastDateNeverCalled;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.MoveHearingToPastDateSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
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
    void shouldRejectMagistratesMoveWith422WhenCourtschedulerReturnsFutureDateNotAllowed() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        stubMoveHearingToPastDateFailure(moveSteps.getHearingId(), 422, "FUTURE_DATE_NOT_ALLOWED",
                "Hearings can only be moved to today or an earlier date");

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", ItClock.today().plusDays(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("FUTURE_DATE_NOT_ALLOWED"));
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
    void shouldRejectMoveWith422WhenHearingIdUnknown() {
        // A hearing that was never listed - MoveHearingToPastDateSteps still needs SOME allocated
        // hearing to obtain a courtCentreId, but we submit against a random unknown hearingId.
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);
        final UUID unknownHearingId = randomUUID();

        final Response response = moveSteps.whenHearingIsMovedToPastDateForHearing(unknownHearingId, pastWorkingDay(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("HEARING_ID_NOT_FOUND"));
        verifyMoveHearingToPastDateNeverCalled(unknownHearingId.toString());
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

    @Test
    void shouldRejectCrownMoveToFutureDateWithoutCallingCourtScheduler() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(CROWN_JURISDICTION);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", ItClock.today().plusDays(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("FUTURE_DATE_NOT_ALLOWED"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }

    @Test
    void shouldMoveMagistratesHearingToPastDateOverAMultiDayRange() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);
        final LocalDate startDate = pastWorkingDay(2);
        final LocalDate endDate = pastWorkingDay(1);
        final String firstScheduleId = randomUUID().toString();
        final String secondScheduleId = randomUUID().toString();
        stubMoveHearingToPastDateMultiDay(moveSteps.getHearingId(), COURT_ROOM_ID, 30,
                List.of(firstScheduleId, secondScheduleId), List.of(startDate, endDate));

        final Response response = moveSteps.whenHearingIsMovedToPastDateRange(startDate, endDate, COURT_ROOM_ID);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingToPastDateCalled(moveSteps.getHearingId());
        moveSteps.verifyHearingDayCount(2);
        moveSteps.verifyCourtScheduleStored(firstScheduleId);
        moveSteps.verifyCourtScheduleStored(secondScheduleId);
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
}
