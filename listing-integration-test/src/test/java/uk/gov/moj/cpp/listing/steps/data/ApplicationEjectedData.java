package uk.gov.moj.cpp.listing.steps.data;

import java.util.List;
import java.util.UUID;

public class ApplicationEjectedData {
    private final List<UUID> hearingIds;

    private final UUID applicationId;

    private final String removalReason;

    public ApplicationEjectedData(final List<UUID> hearingIds, final UUID applicationId, final String removalReason) {
        this.hearingIds = hearingIds;
        this.applicationId = applicationId;
        this.removalReason = removalReason;
    }

    public static Builder applicationEjected() {
        return new ApplicationEjectedData.Builder();
    }

    public List<UUID> getHearingIds() {
        return hearingIds;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    public static class Builder {
        private List<UUID> hearingIds;

        private UUID applicationId;

        private String removalReason;

        public Builder withHearingIds(final List<UUID> hearingIds) {
            this.hearingIds = hearingIds;
            return this;
        }

        public Builder withApplicationId(final UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }
        public Builder withRemovalReason(final String removalReason) {
            this.removalReason = removalReason;
            return this;
        }

        public ApplicationEjectedData build() {
            return new ApplicationEjectedData(hearingIds, applicationId, removalReason);
        }
    }
}
