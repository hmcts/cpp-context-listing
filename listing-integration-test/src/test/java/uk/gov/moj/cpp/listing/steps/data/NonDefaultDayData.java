package uk.gov.moj.cpp.listing.steps.data;

import static java.util.Optional.empty;

import java.util.Optional;

public class NonDefaultDayData {

    private final String startTime;

    private final Optional<Integer> duration;

    private final Optional<String> courtScheduleId;

    private final Optional<Integer> courtRoomId;

    private final Optional<String> oucode;

    private final Optional<String> session;

    public NonDefaultDayData(final String startTime) {
        this(startTime, empty());
    }

    public NonDefaultDayData(final String startTime, final Optional<Integer> duration) {
        this(startTime, duration, empty(), empty(), empty(), empty());
    }

    public NonDefaultDayData(final String startTime, final Optional<Integer> duration, final Optional<String> courtScheduleId, final Optional<Integer> courtRoomId, final Optional<String> oucode, final Optional<String> session) {
        this.startTime = startTime;
        this.duration = duration;
        this.courtScheduleId = courtScheduleId;
        this.courtRoomId = courtRoomId;
        this.oucode = oucode;
        this.session = session;
    }

    public Optional<Integer> getDuration() {
        return duration;
    }

    public String getStartTime() {
        return startTime;
    }

    public Optional<String> getCourtScheduleId() {
        return courtScheduleId;
    }

    public Optional<Integer> getCourtRoomId() {
        return courtRoomId;
    }

    public Optional<String> getOucode() {
        return oucode;
    }

    public Optional<String> getSession() {
        return session;
    }
}
