package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingToPastDate;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingToPastDateFailure;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMoveHearingToPastDateCalled;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMoveHearingToPastDateNeverCalled;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.MoveHearingToPastDateSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
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

    @Test
    void shouldMoveMagistratesHearingToPastDateAndStoreCourtScheduleEnrichment() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

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
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);
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
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

        stubMoveHearingToPastDateFailure(moveSteps.getHearingId(), 422, "FUTURE_DATE_NOT_ALLOWED",
                "Hearings can only be moved to today or an earlier date");

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", ItClock.today().plusDays(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("FUTURE_DATE_NOT_ALLOWED"));
    }

    @Test
    void shouldRejectMagistratesMoveWith404WhenNoCourtScheduleSessionExists() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

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
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(CROWN_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);
        final LocalDate pastDate = ItClock.today().minusDays(1);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", pastDate);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
        moveSteps.verifyStartDateUpdated(pastDate);
    }

    @Test
    void shouldRejectCrownMoveToFutureDateWithoutCallingCourtScheduler() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(CROWN_JURISDICTION);
        final MoveHearingToPastDateSteps moveSteps = new MoveHearingToPastDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", ItClock.today().plusDays(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("FUTURE_DATE_NOT_ALLOWED"));
        verifyMoveHearingToPastDateNeverCalled(moveSteps.getHearingId());
    }
}
