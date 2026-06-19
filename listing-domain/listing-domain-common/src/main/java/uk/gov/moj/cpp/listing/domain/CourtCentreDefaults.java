package uk.gov.moj.cpp.listing.domain;

import java.time.LocalTime;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class CourtCentreDefaults {

    private final Integer defaultDuration;

    private final LocalTime defaultStartTime;

    private final UUID courtCentreId;
    public CourtCentreDefaults(final Integer defaultDuration, final LocalTime defaultStartTime, final UUID courtCentreId) {
        this.defaultDuration = defaultDuration;
        this.defaultStartTime = defaultStartTime;
        this.courtCentreId = courtCentreId;
    }

    public Integer getDefaultDuration() {
        return defaultDuration;
    }

    public LocalTime getDefaultStartTime() {
        return defaultStartTime;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public static Builder courtCentreDefaults() {
        return new CourtCentreDefaults.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final CourtCentreDefaults that = (CourtCentreDefaults) obj;

        return java.util.Objects.equals(this.defaultDuration, that.defaultDuration) &&
                java.util.Objects.equals(this.defaultStartTime, that.defaultStartTime) &&
                java.util.Objects.equals(this.courtCentreId, that.courtCentreId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(defaultDuration, defaultStartTime, courtCentreId);}

    @Override
    public String toString() {
        return "CourtCentreDefaults{" +
                "defaultDuration='" + defaultDuration + "'," +
                "defaultStartTime='" + defaultStartTime + "'," +
                "courtCentreId='" + courtCentreId + "'" +
                "}";
    }

    public static class Builder {
        private Integer defaultDuration;

        private LocalTime defaultStartTime;

        private UUID courtCentreId;

        public Builder withDefaultDuration(final Integer defaultDuration) {
            this.defaultDuration = defaultDuration;
            return this;
        }

        public Builder withDefaultStartTime(final LocalTime defaultStartTime) {
            this.defaultStartTime = defaultStartTime;
            return this;
        }

        public Builder withCourtCentreId(final UUID id) {
            this.courtCentreId = id;
            return this;
        }

        public CourtCentreDefaults build() {
            return new CourtCentreDefaults(defaultDuration, defaultStartTime, courtCentreId);
        }
    }
}


