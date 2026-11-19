package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S1067", "squid:S00121", "pmd:BeanMembersShouldSerialize", "squid:S00107", "squid:S00122", "squid:S2384"})
public class OffenceIds implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID id;

    private final SeedingHearing seedingHearing;

    // The offence added the case after this hearing created so it was added into this hearing as well.
    private Boolean isNewOffence;


    public OffenceIds(final UUID id, final SeedingHearing seedingHearing, final Boolean isNewOffence) {
        this.id = id;
        this.seedingHearing = seedingHearing;
        this.isNewOffence = isNewOffence;
    }

    public UUID getId() {
        return id;
    }

    public SeedingHearing getSeedingHearing() {
        return seedingHearing;
    }

    public Boolean getIsNewOffence() {
        return isNewOffence;
    }

    public void setIsNewOffence(final Boolean isNewOffence) {
        this.isNewOffence = isNewOffence;
    }

    public static Builder offenceIds() {
        return new OffenceIds.Builder();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final OffenceIds offence = (OffenceIds) o;
        return  Objects.equals(id, offence.id) &&
                Objects.equals(seedingHearing, offence.seedingHearing) &&
                Objects.equals(isNewOffence, offence.isNewOffence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, seedingHearing);
    }

    @Override
    public String toString() {
        return "Offence{" +
                ", id=" + id +
                ", seedingHearing=" + seedingHearing +
                '}';
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {
        private UUID id;

        private SeedingHearing seedingHearing;

        private Boolean isNewOffence;

        public Builder withValues(final OffenceIds offenceIds){
            this.id = offenceIds.getId();
            this.seedingHearing = offenceIds.getSeedingHearing();
            this.isNewOffence = offenceIds.getIsNewOffence();
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
            this.seedingHearing = seedingHearing;
            return this;
        }

        public Builder withIsNewOffence(final Boolean isNewOffence) {
            this.isNewOffence = isNewOffence;
            return this;
        }

        public OffenceIds build() {
            return new OffenceIds(id, seedingHearing, isNewOffence);
        }
    }
}
