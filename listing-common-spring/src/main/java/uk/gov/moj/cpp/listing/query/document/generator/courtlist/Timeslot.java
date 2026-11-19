package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.List;
import java.util.Objects;

@SuppressWarnings({"squid:S1067", "squid:S00121"})
public class Timeslot {


    private List<Hearing> hearings;

    public List<Hearing> getHearings() {
        return hearings;
    }

    public static Builder timeslot() {
        return new Timeslot.Builder();
    }


    public static final class Builder {

        private List<Hearing> hearings;

        private Builder() {
        }


        public Builder withHearings(List<Hearing> hearings) {
            this.hearings = hearings;
            return this;
        }

        public Timeslot build() {
            final Timeslot timeslot = new Timeslot();
            timeslot.hearings = this.hearings;

            return timeslot;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Timeslot)) return false;
        final Timeslot timeslot = (Timeslot) o;
        return Objects.equals(hearings, timeslot.hearings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearings);
    }

    @Override
    public String toString() {
        return "Timeslot{" +
                "hearings=" + hearings +
                '}';
    }
}
