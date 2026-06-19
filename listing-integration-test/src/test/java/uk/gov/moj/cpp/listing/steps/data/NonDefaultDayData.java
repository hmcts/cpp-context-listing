package uk.gov.moj.cpp.listing.steps.data;

import static java.util.Optional.empty;

import java.util.Optional;
import java.util.UUID;

public class NonDefaultDayData {

    private final String startTime;

    private final Optional<Integer> duration;

    private final Optional<String> courtScheduleId;

    private final Optional<Integer> courtRoomId;

    private final Optional<String> oucode;

    private final Optional<String> session;

    private final Optional<String> roomId;

    private final Optional<String> courtCentreId;

    private final Optional<Boolean> virtual;

    public NonDefaultDayData(final String startTime, final Optional<String> courtCentreId, final Optional<String> roomId) {
        this(startTime, empty(), courtCentreId, roomId);
    }

    public NonDefaultDayData(final String startTime, final Optional<Integer> duration, final Optional<String> courtCentreId, final Optional<String> roomId) {
        this(startTime, duration, empty(), empty(), empty(), empty(), courtCentreId, roomId);
    }

    public NonDefaultDayData(final String startTime, final Optional<Integer> duration, final Optional<String> courtCentreId, final UUID courtScheduleId, final Optional<String> roomId) {
        this(startTime, duration, empty(), empty(), empty(), empty(), courtCentreId, roomId, Optional.empty());
    }
    public NonDefaultDayData(final String startTime, final Optional<Integer> duration, final Optional<String> courtCentreId, final UUID courtScheduleId, final Optional<String> roomId, final Optional<Boolean> virtual) {
        this(startTime, duration, empty(), empty(), empty(), empty(), courtCentreId, roomId, virtual);
    }

    public NonDefaultDayData(final String startTime, final Optional<Integer> duration, final Optional<String> courtScheduleId, final Optional<Integer> courtRoomId, final Optional<String> oucode, final Optional<String> session, final Optional<String> courtCentreId, final Optional<String> roomId) {
        this(startTime, duration, courtScheduleId, courtRoomId, oucode, session, courtCentreId, roomId, empty());
    }

    public NonDefaultDayData(final String startTime, final Optional<Integer> duration, final Optional<String> courtScheduleId, final Optional<Integer> courtRoomId, final Optional<String> oucode, final Optional<String> session, final Optional<String> courtCentreId, final Optional<String> roomId, final Optional<Boolean> virtual) {
        this.startTime = startTime;
        this.duration = duration;
        this.courtScheduleId = courtScheduleId;
        this.courtRoomId = courtRoomId;
        this.oucode = oucode;
        this.session = session;
        this.courtCentreId = courtCentreId;
        this.roomId = roomId;
        this.virtual = virtual;
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

    public Optional<String> getRoomId() {
        return roomId;
    }

    public Optional<String> getCourtCentreId() {
        return courtCentreId;
    }

    public Optional<Boolean> getVirtual() {
        return virtual;
    }
}
