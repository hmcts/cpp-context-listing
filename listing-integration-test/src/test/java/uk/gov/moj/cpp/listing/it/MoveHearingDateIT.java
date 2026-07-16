package uk.gov.moj.cpp.listing.it;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollUntilHearingIsPresent;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetCourtSchedulesByIdWithDraftStatus;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingDate;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingDateMultiDay;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMoveHearingDateFailure;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMoveHearingDateCalled;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMoveHearingDateNeverCalled;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCrownCourtCentreById;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.MoveHearingDateSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

/**
 * Covers listing.command.move-hearing-date: MAGISTRATES wraps courtscheduler synchronously and stores
 * the returned slot as enrichment; CROWN is listing-side-only (Baris decision D1) and never calls
 * courtscheduler. Past and future dates are accepted; single- and multi-day ranges are covered.
 */
class MoveHearingDateIT extends AbstractIT {

    private static final String COURT_ROOM_ID = "731816c1-27ea-4711-8d92-0a1c2f3ab7de";

    /**
     * Lists a real hearing through the full flow (command → events → viewstore projection) and only
     * returns once it is queryable — the move re-issues the hearing's EXISTING days onto the new date,
     * so the aggregate must already hold hearing days (an un-listed hearing is a silent no-op). Mirrors
     * VacateHearingIT: MAGS listing needs the provisional-booking + list-hearing-in-court-sessions
     * stubs; CROWN listing never calls courtscheduler pre-Phase-2.
     */
    private MoveHearingDateSteps givenAListedHearing(final String jurisdiction) {
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

        return new MoveHearingDateSteps(hearingsData);
    }

    @Test
    void shouldMoveMagistratesHearingDateAndStoreCourtScheduleEnrichment() {
        final MoveHearingDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        final LocalDate pastDate = pastWorkingDay(1);
        final String courtScheduleId = randomUUID().toString();
        stubMoveHearingDate(moveSteps.getHearingId(), courtScheduleId, COURT_ROOM_ID, pastDate, 30);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", pastDate);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingDateCalled(moveSteps.getHearingId());
        moveSteps.verifyCourtScheduleStored(courtScheduleId);
    }

    @Test
    void shouldReleasePriorAllocationWhenMagistratesHearingMovedAgain() {
        final MoveHearingDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);
        final LocalDate pastDate = pastWorkingDay(1);

        final String firstCourtScheduleId = randomUUID().toString();
        stubMoveHearingDate(moveSteps.getHearingId(), firstCourtScheduleId, COURT_ROOM_ID, pastDate, 30);
        assertThat(moveSteps.whenHearingIsMovedToPastDate("MAGS", pastDate).getStatus(), is(ACCEPTED.getStatusCode()));
        moveSteps.verifyCourtScheduleStored(firstCourtScheduleId);

        final String secondCourtScheduleId = randomUUID().toString();
        stubMoveHearingDate(moveSteps.getHearingId(), secondCourtScheduleId, COURT_ROOM_ID, pastDate, 30);
        assertThat(moveSteps.whenHearingIsMovedToPastDate("MAGS", pastDate).getStatus(), is(ACCEPTED.getStatusCode()));
        moveSteps.verifyCourtScheduleStored(secondCourtScheduleId);
    }

    @Test
    void shouldRejectMagistratesMoveWith422WhenNoCourtScheduleSessionExists() {
        final MoveHearingDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        stubMoveHearingDateFailure(moveSteps.getHearingId(), 422, "NO_SESSION_FOUND",
                "No court-schedule session found for the given date and court centre");

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", pastWorkingDay(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("NO_SESSION_FOUND"));
    }

    /** Older courtscheduler releases signalled no-session as a bare 404 - the listing adapter
     * normalises that to the 422 NO_SESSION_FOUND contract. */
    @Test
    void shouldNormaliseLegacyCourtscheduler404ToA422NoSessionFound() {
        final MoveHearingDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);

        stubMoveHearingDateFailure(moveSteps.getHearingId(), 404, null,
                "No court-schedule session found for the given date and court centre");

        final Response response = moveSteps.whenHearingIsMovedToPastDate("MAGS", pastWorkingDay(1));

        assertThat(response.getStatus(), is(422));
        assertThat(response.readEntity(String.class), containsString("NO_SESSION_FOUND"));
    }

    @Test
    void shouldRejectMoveWith400WhenMandatoryFieldMissing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingDateSteps moveSteps = new MoveHearingDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedWithMissingCourtCentre(pastWorkingDay(1));

        assertThat(response.getStatus(), is(400));
    }

    @Test
    void shouldRejectMoveWith400WhenCourtRoomIdMissing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingDateSteps moveSteps = new MoveHearingDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedWithMissingCourtRoom(pastWorkingDay(1));

        assertThat(response.getStatus(), is(400));
    }

    @Test
    void shouldRejectMoveWith400WhenStartTimeMissing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(MAGISTRATES_JURISDICTION);
        final MoveHearingDateSteps moveSteps = new MoveHearingDateSteps(hearingsData);

        final Response response = moveSteps.whenHearingIsMovedWithMissingStartTime();

        assertThat(response.getStatus(), is(400));
    }

    @Test
    void shouldMoveCrownHearingDateListingSideOnlyWithoutCallingCourtScheduler() {
        final MoveHearingDateSteps moveSteps = givenAListedHearing(CROWN_JURISDICTION);
        final LocalDate pastDate = pastWorkingDay(1);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", pastDate);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingDateNeverCalled(moveSteps.getHearingId());
        moveSteps.verifyStartDateUpdated(pastDate);
    }

    /** FUTURE_DATE_NOT_ALLOWED has been removed - a CROWN move to a future date is now accepted and
     * still stays listing-side (courtscheduler is never called for CROWN). */
    @Test
    void shouldAllowCrownMoveToFutureDateWithoutCallingCourtScheduler() {
        final MoveHearingDateSteps moveSteps = givenAListedHearing(CROWN_JURISDICTION);
        final LocalDate futureDate = futureWorkingDay(1);

        final Response response = moveSteps.whenHearingIsMovedToPastDate("CROWN", futureDate);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingDateNeverCalled(moveSteps.getHearingId());
        moveSteps.verifyStartDateUpdated(futureDate);
    }

    @Test
    void shouldMoveMagistratesHearingDateOverAMultiDayRange() {
        final MoveHearingDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);
        final LocalDate startDate = pastWorkingDay(2);
        final LocalDate endDate = pastWorkingDay(1);
        final String firstScheduleId = randomUUID().toString();
        final String secondScheduleId = randomUUID().toString();
        stubMoveHearingDateMultiDay(moveSteps.getHearingId(), COURT_ROOM_ID, 30,
                List.of(firstScheduleId, secondScheduleId), List.of(startDate, endDate));

        final Response response = moveSteps.whenHearingIsMovedToPastDateRange(startDate, endDate, COURT_ROOM_ID);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingDateCalled(moveSteps.getHearingId());
        moveSteps.verifyHearingDayCount(2);
        moveSteps.verifyCourtScheduleStored(firstScheduleId);
        moveSteps.verifyCourtScheduleStored(secondScheduleId);
        // main-level start/end track the hearing days: earliest (startDate) and latest (endDate) sitting day
        moveSteps.verifyStartAndEndDatesUpdated(startDate, endDate);
    }

    @Test
    void shouldMoveMagistratesHearingWithinTheRequestedRoom() {
        final MoveHearingDateSteps moveSteps = givenAListedHearing(MAGISTRATES_JURISDICTION);
        final LocalDate pastDate = pastWorkingDay(1);
        final String courtScheduleId = randomUUID().toString();
        stubMoveHearingDate(moveSteps.getHearingId(), courtScheduleId, COURT_ROOM_ID, pastDate, 30);

        final Response response = moveSteps.whenHearingIsMovedToPastDateRange(pastDate, pastDate, COURT_ROOM_ID);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingDateCalled(moveSteps.getHearingId());
        moveSteps.verifyCourtScheduleStored(courtScheduleId);
    }

    // ---- non-sitting / non-default day preservation (retain unaffected, drop affected) ----

    @Test
    void shouldRetainNonSittingAndNonDefaultDaysStillWithinTheMovedSpan() {
        final LocalDate spanStart = ItClock.today().minusDays(20);
        final LocalDate spanEnd = spanStart.plusDays(4);
        final LocalDate nonSittingDay = spanStart.plusDays(2);              // middle of the span
        final String nonDefaultStartTime = spanStart.plusDays(3) + "T11:30:00Z";

        final HearingsData hearingsData = givenAListedCrownHearingWithNonSittingAndNonDefaultDays(
                spanStart, spanEnd, nonSittingDay, nonDefaultStartTime);
        final MoveHearingDateSteps moveSteps = new MoveHearingDateSteps(hearingsData);
        final String roomId = hearingsData.getHearingData().get(0).getCourtRoomId().toString();

        // precondition: the update has projected the non-sitting / non-default days onto the hearing
        moveSteps.verifyNonSittingDayRetained(nonSittingDay);

        // move to the SAME span - both days stay inside [spanStart, spanEnd], so both are retained
        final Response response = moveSteps.whenHearingIsMovedToPastDateRange(spanStart, spanEnd, roomId);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingDateNeverCalled(moveSteps.getHearingId());        // CROWN stays listing-side
        moveSteps.verifyNonSittingDayRetained(nonSittingDay);
        moveSteps.verifyNonDefaultDaysRetained();
    }

    @Test
    void shouldDropNonSittingAndNonDefaultDaysPushedOutsideTheMovedSpan() {
        final LocalDate spanStart = ItClock.today().minusDays(20);
        final LocalDate spanEnd = spanStart.plusDays(4);
        final LocalDate nonSittingDay = spanStart.plusDays(2);
        final String nonDefaultStartTime = spanStart.plusDays(3) + "T11:30:00Z";

        final HearingsData hearingsData = givenAListedCrownHearingWithNonSittingAndNonDefaultDays(
                spanStart, spanEnd, nonSittingDay, nonDefaultStartTime);
        final MoveHearingDateSteps moveSteps = new MoveHearingDateSteps(hearingsData);
        final String roomId = hearingsData.getHearingData().get(0).getCourtRoomId().toString();

        moveSteps.verifyNonSittingDayRetained(nonSittingDay);              // update projected

        // move to a later, non-overlapping span - both existing days now fall outside it, so both drop
        final LocalDate newStart = spanEnd.plusDays(10);
        final Response response = moveSteps.whenHearingIsMovedToPastDateRange(newStart, newStart.plusDays(2), roomId);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
        verifyMoveHearingDateNeverCalled(moveSteps.getHearingId());
        moveSteps.verifyNonSittingAndNonDefaultDaysCleared();
    }

    /**
     * Lists a multi-day CROWN hearing, then updates it to carry a non-sitting day and a non-default-room
     * day inside its span, so a subsequent move can be shown to retain or drop them by date. CROWN keeps
     * the whole flow listing-side (no courtscheduler).
     */
    private HearingsData givenAListedCrownHearingWithNonSittingAndNonDefaultDays(
            final LocalDate spanStart, final LocalDate spanEnd, final LocalDate nonSittingDay, final String nonDefaultStartTime) {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = getRandomCourtCenterId();
        final UUID courtRoomId = getRandomCourtRoomId();
        final ZonedDateTime hearingStartTime = spanStart.atTime(10, 30).atZone(ZoneOffset.UTC);

        final CaseAndDefendantData caseData = new CaseAndDefendantData(hearingId, null, "CASE_URN_" + hearingId,
                randomUUID(), null, CROWN_JURISDICTION, CROWN_JURISDICTION, null, null);
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(
                caseData, courtCentreId, courtRoomId, spanEnd, hearingStartTime);

        final ListCourtHearingSteps listSteps = new ListCourtHearingSteps(hearingsData);
        final String listedCourtScheduleId = randomUUID().toString();
        stubGetCourtSchedulesByIdWithDraftStatus(singletonList(listedCourtScheduleId), false);
        stubListHearingInCourtSessions(hearingId.toString(), listedCourtScheduleId, hearingStartTime);
        stubGetReferenceDataCrownCourtCentreById(courtCentreId, courtRoomId);

        listSteps.whenCaseIsSubmittedForListing();
        listSteps.verifyHearingListedFromAPI(ALLOCATED);
        pollUntilHearingIsPresent(courtCentreId.toString(), ALLOCATED, getLoggedInUser().toString(), hearingId.toString());

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final List<NonDefaultDayData> nonDefaultDays = singletonList(new NonDefaultDayData(
                nonDefaultStartTime, Optional.of(360), Optional.of(courtCentreId.toString()), Optional.of(courtRoomId.toString())));
        final UpdatedHearingData updated = new UpdatedHearingData(
                hearingId, courtCentreId, hearingData.getName(), courtRoomId, hearingData.getHearingTypeData(),
                spanStart.toString(), spanEnd.toString(), nonDefaultDays, singletonList(nonSittingDay.toString()),
                "ENGLISH", hearingData.getJudiciary(), CROWN_JURISDICTION, null, null, null,
                hearingData.getHasVideoLink(), hearingData.getPublicListNote(), "High", null, null, false, null);
        new UpdateHearingSteps(hearingsData, updated).whenHearingIsUpdatedForListing();

        return hearingsData;
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
}
