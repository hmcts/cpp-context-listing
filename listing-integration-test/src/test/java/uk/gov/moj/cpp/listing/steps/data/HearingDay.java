package uk.gov.moj.cpp.listing.steps.data;

import java.time.ZonedDateTime;
import java.util.Optional;

public class HearingDay {

    private final Integer listedDurationMinutes;

    private final Optional<Integer> listingSequence;

    private final ZonedDateTime sittingDay;

    private final Optional<Boolean> isCancelled;

    public HearingDay(final Integer listedDurationMinutes, final Optional<Integer> listingSequence, final ZonedDateTime sittingDay, final Optional<Boolean> isCancelled) {
        this.isCancelled = isCancelled;
        this.listedDurationMinutes = listedDurationMinutes;
        this.listingSequence = listingSequence;
        this.sittingDay = sittingDay;
    }

    public Optional<Boolean> getIsCancelled() {
        return isCancelled;
    }

    public Integer getListedDurationMinutes() {
        return listedDurationMinutes;
    }

    public Optional<Integer> getListingSequence() {
        return listingSequence;
    }

    public ZonedDateTime getSittingDay() {
        return sittingDay;
    }

    public static Builder hearingDay() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final HearingDay that = (HearingDay) obj;

        return java.util.Objects.equals(this.isCancelled, that.isCancelled) &&
                java.util.Objects.equals(this.listedDurationMinutes, that.listedDurationMinutes) &&
                java.util.Objects.equals(this.listingSequence, that.listingSequence) &&
                java.util.Objects.equals(this.sittingDay, that.sittingDay);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(isCancelled, listedDurationMinutes, listingSequence, sittingDay);
    }

    @Override
    public String toString() {
        return "HearingDay{" +
                "isCancelled='" + isCancelled + "'," +
                "listedDurationMinutes='" + listedDurationMinutes + "'," +
                "listingSequence='" + listingSequence + "'," +
                "sittingDay='" + sittingDay + "'" +
                "}";
    }

    public static class Builder {
        private Boolean isCancelled;

        private Integer listedDurationMinutes;

        private Integer listingSequence;

        private ZonedDateTime sittingDay;

        public Builder withIsCancelled(final Boolean isCancelled) {
            this.isCancelled = isCancelled;
            return this;
        }

        public Builder withListedDurationMinutes(final Integer listedDurationMinutes) {
            this.listedDurationMinutes = listedDurationMinutes;
            return this;
        }

        public Builder withListingSequence(final Integer listingSequence) {
            this.listingSequence = listingSequence;
            return this;
        }

        public Builder withSittingDay(final ZonedDateTime sittingDay) {
            this.sittingDay = sittingDay;
            return this;
        }

        public HearingDay build() {
            return new HearingDay(listedDurationMinutes, Optional.ofNullable(listingSequence), sittingDay, Optional.ofNullable(isCancelled));
        }
    }
}
