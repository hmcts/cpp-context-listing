package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingToPastDate;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingToPastDateFailure;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMoveHearingToPastDateCalled;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMoveHearingToPastDateNeverCalled;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.MoveHearingToPastDateSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

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

    private static final String COURT_ROOM_ID = "731816";

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

        return new MoveHearingToPastDateSteps(hearingsData);
    }

    @Test
    void shouldMoveMagistratesHearingToPastDateAndStoreCourtScheduleEnrichment() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        final LocalDate pastDate = ItClock.today().minusDays(1);
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
        final LocalDate pastDate = ItClock.today().minusDays(1);

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
    void shouldRejectMagistratesMoveWith404WhenNoCourtScheduleSessionExists() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        stubMoveHearingToPastDateFailure(moveSteps.getHearingId(), 404, null,
                "No court-schedule session found for the given date and court centre");

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", ItClock.today().minusDays(1));

        assertThat(response.getStatus(), is(404));
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

    @Test
    void shouldMoveCrownHearingToPastDateListingSideOnlyWithoutCallingCourtScheduler() {
        final MoveHearingToPastDateSteps moveSteps = givenAListedHearing(CROWN_JURISDICTION);
        final LocalDate pastDate = ItClock.today().minusDays(1);

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
}
