package uk.gov.moj.cpp.listing.domain;

import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S00121"})
public class SequenceHearing {
    private final UUID id;

    private final List<HearingDay> hearingDays;

    public SequenceHearing(final UUID id, final List<HearingDay> hearingDays) {
        this.id = id;
        this.hearingDays = hearingDays;
    }

    public UUID getId() {
        return id;
    }

    public List<HearingDay> getHearingDays() {
        return hearingDays;
    }

    public static Builder sequenceHearing() {
        return new SequenceHearing.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final SequenceHearing that = (SequenceHearing) obj;

        return java.util.Objects.equals(this.id, that.id) &&
                java.util.Objects.equals(this.hearingDays, that.hearingDays);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, hearingDays);}

    @Override
    public String toString() {
        return "SequenceHearing{" +
                "id='" + id + "'," +
                "hearingDays='" + hearingDays + "'" +
                "}";
    }

    public static class Builder {
        private UUID id;

        private List<HearingDay> hearingDayList;

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withHearingDays(final List<HearingDay> hearingDays) {
            this.hearingDayList = hearingDays;
            return this;
        }

        public SequenceHearing build() {
            return new SequenceHearing(id, hearingDayList);
        }
    }
}
