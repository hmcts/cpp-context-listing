package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("squid:S2384")
public class JudgeList {
    private String courtCentre;
    private String courtRoomName;
    private String hearingDate;
    private List<Sitting> sittings;

    public static Builder judgeList() {
        return new JudgeList.Builder();
    }

    public String getCourtCentre() {
        return courtCentre;
    }

    public String getCourtRoomName() {
        return courtRoomName;
    }

    public String getHearingDate() {
        return hearingDate;
    }

    public List<Sitting> getSittings() {
        return sittings;
    }

    public static final class Builder {
        private String courtCentre;
        private String courtRoomName;
        private String hearingDate;
        private List<Sitting> sittings;


        private Builder() {
        }

        public Builder withCourtCentre(final String courtCentre) {
            this.courtCentre = courtCentre;
            return this;
        }


        public Builder withCourtRoomName(final String courtRoomName) {
            this.courtRoomName = courtRoomName;
            return this;
        }

        public Builder withHearingDate(final String hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public Builder withSitting(final List<Sitting> sittings) {
            this.sittings = sittings;
            return this;
        }


        public JudgeList build() {
            final JudgeList judgeList = new JudgeList();
            judgeList.courtCentre = this.courtCentre;
            judgeList.courtRoomName = this.courtRoomName;
            judgeList.hearingDate = this.hearingDate;
            judgeList.sittings = this.sittings;
            return judgeList;
        }
    }

    @Override
    public String toString() {
        return "StandardCourtList{" +
                "courtCentre='" + courtCentre + '\'' +
                ", courtRoomName='" + courtRoomName + '\'' +
                ", hearingDate='" + hearingDate + '\'' +
                ", sittings='" + sittings + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JudgeList)) {
            return false;
        }
        final JudgeList that = (JudgeList) o;
        return Objects.equals(courtCentre, that.courtCentre) &&
                Objects.equals(courtRoomName, that.courtRoomName) &&
                Objects.equals(hearingDate, that.hearingDate) &&
                Objects.equals(sittings, that.sittings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courtCentre, courtRoomName, hearingDate, sittings);
    }
}
