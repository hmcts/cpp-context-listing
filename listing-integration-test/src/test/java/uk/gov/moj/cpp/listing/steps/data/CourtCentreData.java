package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalTime;
import java.util.UUID;

public class CourtCentreData {

    private final String defaultDurationHoursMins;

    private final LocalTime defaultStartTime;

    private final UUID courtCentreId;
    private final String name;

    private final UUID courtRoomId;

    public CourtCentreData(final UUID courtCentreId, final LocalTime defaultStartTime, final String defaultDurationHoursMins, final UUID courtRoomId, final String name) {
        this.defaultDurationHoursMins = defaultDurationHoursMins;
        this.defaultStartTime = defaultStartTime;
        this.courtCentreId = courtCentreId;
        this.courtRoomId = courtRoomId;
        this.name = name;
    }

    public String getDefaultDurationHoursMins() {
        return defaultDurationHoursMins;
    }

    public LocalTime getDefaultStartTime() {
        return defaultStartTime;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public String getName() {
        return name;
    }
}
