package uk.gov.moj.cpp.listing.common.pastdate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Purpose-built result of a successful {@code courtscheduler.move-hearing-to-past-date} call for
 * MAGISTRATES and CROWN paths. Deliberately narrow — unlike the ccsph2n-only {@code CrownFallbackResult}
 * this branch does not carry any crown-fallback/search-and-book concerns, only the booked session
 * details needed to enrich {@code listing.command.move-hearing-to-past-date-enriched}.
 *
 * <p>{@code sessions} is EVERY past session courtscheduler booked, ordered by date — one for a
 * single-day hearing, N consecutive business days for a multi-day one (courtscheduler derives N from
 * the duration). Listing re-issues one hearing day per session so its day list, start/end dates and
 * courtscheduler's allocated_listings all agree. The first-session accessors are conveniences for the
 * flat (single-day) fields on the enriched command.</p>
 */
public record MoveHearingToPastDateResult(List<BookedSession> sessions) {

    /** One booked court-schedule session as returned by courtscheduler. */
    public record BookedSession(UUID courtScheduleId,
                                String courtRoomId,
                                UUID courtCentreId,
                                LocalDate sessionDate,
                                String sessionStartTime,
                                String sessionEndTime,
                                Integer durationInMinutes,
                                Boolean isDraft) {
    }

    public MoveHearingToPastDateResult {
        sessions = sessions == null ? List.of() : List.copyOf(sessions);
    }

    /** Single-session result (test/legacy convenience). */
    public MoveHearingToPastDateResult(final UUID courtScheduleId,
                                       final String courtRoomId,
                                       final LocalDate sessionDate,
                                       final String sessionStartTime,
                                       final String sessionEndTime,
                                       final Integer durationInMinutes) {
        this(List.of(new BookedSession(courtScheduleId, courtRoomId, null, sessionDate,
                sessionStartTime, sessionEndTime, durationInMinutes, null)));
    }

    private BookedSession first() {
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    public UUID courtScheduleId() {
        return first() == null ? null : first().courtScheduleId();
    }

    public String courtRoomId() {
        return first() == null ? null : first().courtRoomId();
    }

    public LocalDate sessionDate() {
        return first() == null ? null : first().sessionDate();
    }

    public String sessionStartTime() {
        return first() == null ? null : first().sessionStartTime();
    }

    public String sessionEndTime() {
        return first() == null ? null : first().sessionEndTime();
    }

    public Integer durationInMinutes() {
        return first() == null ? null : first().durationInMinutes();
    }

    /** Date of the LAST booked session — the hearing's new end date (== sessionDate for single-day). */
    public LocalDate lastSessionDate() {
        return sessions.isEmpty() ? null : sessions.get(sessions.size() - 1).sessionDate();
    }
}
