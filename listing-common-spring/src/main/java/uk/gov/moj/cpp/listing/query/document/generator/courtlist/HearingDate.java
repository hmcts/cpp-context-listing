package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.List;
import java.util.Objects;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1700"})
public class HearingDate {

    private String hearingDate;
    private String hearingDateWelsh;
    private List<CourtRoom> courtRooms;


    public String getHearingDate() {
        return hearingDate;
    }

    public String getHearingDateWelsh() {
        return hearingDateWelsh;
    }

    public List<CourtRoom> getCourtRooms() {
        return courtRooms;
    }

    public static HearingDate.Builder hearingDate() {
        return new HearingDate.Builder();
    }

    public static final class Builder {

        private String hearingDate;
        private List<CourtRoom> courtRooms;
        private String hearingDateWelsh;

        private Builder() {
        }


        public Builder withHearingDate(String hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public Builder withHearingDateWelsh(String hearingDateWelsh) {
            this.hearingDateWelsh = hearingDateWelsh;
            return this;
        }



        public Builder withCourtRooms(List<CourtRoom> courtRooms) {
            this.courtRooms = courtRooms;
            return this;
        }

        public HearingDate build() {
            final HearingDate hearingDates = new HearingDate();
            hearingDates.hearingDate = this.hearingDate;
            hearingDates.hearingDateWelsh = this.hearingDateWelsh;
            hearingDates.courtRooms = this.courtRooms;
            return hearingDates;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HearingDate)) return false;
        HearingDate that = (HearingDate) o;
        return Objects.equals(hearingDate, that.hearingDate) &&
                Objects.equals(hearingDateWelsh, that.hearingDateWelsh) &&
                Objects.equals(courtRooms, that.courtRooms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearingDate, hearingDateWelsh, courtRooms);
    }

    @Override
    public String toString() {
        return "HearingDate{" +
                "hearingDate='" + hearingDate + '\'' +
                ", hearingDateWelsh='" + hearingDateWelsh + '\'' +
                ", courtRooms=" + courtRooms +
                '}';
    }
}
