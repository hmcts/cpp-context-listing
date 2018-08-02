package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S00121"})
public class DefendantOffenceIds implements Serializable {

    private final UUID id;

    private final List<UUID> offenceIds;

    public DefendantOffenceIds(final UUID id, final List<UUID> offenceIds) {
        this.id = id;
        this.offenceIds = offenceIds;
    }

    public UUID getId() {
        return id;
    }

    public List<UUID> getOffenceIds() {
        return offenceIds;
    }

    public static Builder defendantOffenceIds() {
        return new DefendantOffenceIds.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final DefendantOffenceIds that = (DefendantOffenceIds) obj;

        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.offenceIds, that.offenceIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, offenceIds);
    }


    public static class Builder {
        private UUID id;

        private List<UUID> offenceIds;

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withOffenceIds(final List<UUID> offenceIds) {
            this.offenceIds = offenceIds;
            return this;
        }

        public DefendantOffenceIds build() {
            return new DefendantOffenceIds(id, offenceIds);
        }
    }
}