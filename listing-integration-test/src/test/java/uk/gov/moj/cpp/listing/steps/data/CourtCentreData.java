package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalTime;
import java.util.UUID;

public class CourtCentreData {

    private final String defaultDurationHoursMins;

    private final LocalTime defaultStartTime;

    private final UUID courtCentreId;
    private final UUID courtRoomId;
    public CourtCentreData(UUID courtCentreId, LocalTime defaultStartTime, String defaultDurationHoursMins, final UUID courtRoomId) {
        this.defaultDurationHoursMins = defaultDurationHoursMins;
        this.defaultStartTime = defaultStartTime;
        this.courtCentreId = courtCentreId;
        this.courtRoomId = courtRoomId;
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
}
