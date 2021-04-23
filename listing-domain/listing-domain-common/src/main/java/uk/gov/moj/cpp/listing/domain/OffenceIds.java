package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S1067", "squid:S00121", "pmd:BeanMembersShouldSerialize", "squid:S00107", "squid:S00122", "squid:S2384"})
public class OffenceIds implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID id;

    private final SeedingHearing seedingHearing;

    public OffenceIds(final UUID id, final SeedingHearing seedingHearing ) {
        this.id = id;
        this.seedingHearing = seedingHearing;
    }

    public UUID getId() {
        return id;
    }

    public SeedingHearing getSeedingHearing() {
        return seedingHearing;
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
                Objects.equals(seedingHearing, offence.seedingHearing);
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

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
            this.seedingHearing = seedingHearing;
            return this;
        }

        public OffenceIds build() {
            return new OffenceIds(id, seedingHearing);
        }
    }
}
