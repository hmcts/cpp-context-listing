package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.Objects;

public class PersonDefendant {
    private String arrestSummonsNumber;

    public String getArrestSummonsNumber() {
        return arrestSummonsNumber;
    }

    public static PersonDefendant.Builder personDefendant() {
        return new PersonDefendant.Builder();
    }

    public static final class Builder {
        private String arrestSummonsNumber;

        private Builder() {
        }

        public PersonDefendant.Builder withArrestSummonsNumber(final String arrestSummonsNumber) {
            this.arrestSummonsNumber = arrestSummonsNumber;
            return this;
        }

        public PersonDefendant build() {
            final PersonDefendant personDefendant = new PersonDefendant();
            personDefendant.arrestSummonsNumber = arrestSummonsNumber;
            return personDefendant;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonDefendant)) return false;
        final PersonDefendant that = (PersonDefendant) o;
        return Objects.equals(arrestSummonsNumber, that.arrestSummonsNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrestSummonsNumber);
    }
}
