package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.List;
import java.util.Objects;

@SuppressWarnings({"squid:S1067", "squid:S00107", "squid:S00121"})
public class StandardCourtList {

    private String listType;
    private String courtCentreName;
    private String courtCentreDefaultStartTime;
    private String courtCentreAddress1;
    private String courtCentreAddress2;
    private String welshCourtCentreName;
    private String welshCourtCentreAddress1;
    private String welshCourtCentreAddress2;
    private List<HearingDate> hearingDates;

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public String getCourtCentreDefaultStartTime() {
        return courtCentreDefaultStartTime;
    }

    public String getCourtCentreAddress1() {
        return courtCentreAddress1;
    }

    public String getCourtCentreAddress2() {
        return courtCentreAddress2;
    }

    public String getWelshCourtCentreName() {
        return welshCourtCentreName;
    }

    public String getWelshCourtCentreAddress1() {
        return welshCourtCentreAddress1;
    }

    public String getWelshCourtCentreAddress2() {
        return welshCourtCentreAddress2;
    }

    public List<HearingDate> getHearingDates() {
        return hearingDates;
    }

    public String getListType() {
        return listType;
    }

    public static Builder standardCourtList() {
        return new StandardCourtList.Builder();
    }


    public static final class Builder {
        private String listType;
        private String courtCentreName;
        private String courtCentreDefaultStartTime;
        private String courtCentreAddress1;
        private String courtCentreAddress2;
        private String welshCourtCentreName;
        private String welshCourtCentreAddress1;
        private String welshCourtCentreAddress2;
        private List<HearingDate> hearingDates;

        private Builder() {
        }

        public Builder withCourtCentreName(String courtCentreName) {
            this.courtCentreName = courtCentreName;
            return this;
        }

        public Builder withCourtCentreDefaultStartTime(String courtCenterDefaultStartTime) {
            this.courtCentreDefaultStartTime = courtCenterDefaultStartTime;
            return this;
        }

        public Builder withCourtCentreAddress1(String courtCentreAddress1) {
            this.courtCentreAddress1 = courtCentreAddress1;
            return this;
        }

        public Builder withCourtCentreAddress2(String courtCentreAddress2) {
            this.courtCentreAddress2 = courtCentreAddress2;
            return this;
        }

        public Builder withWelshCourtCentreName(String welshCourtCentreName) {
            this.welshCourtCentreName = welshCourtCentreName;
            return this;
        }

        public Builder withWelshCourtCentreAddress1(String welshCourtCentreAddress1) {
            this.welshCourtCentreAddress1 = welshCourtCentreAddress1;
            return this;
        }

        public Builder withWelshCourtCentreAddress2(String welshCourtCentreAddress2) {
            this.welshCourtCentreAddress2 = welshCourtCentreAddress2;
            return this;
        }

        public Builder withListType(String listType) {
            this.listType = listType;
            return this;
        }

        public Builder withHearingDates(List<HearingDate> hearingDates) {
            this.hearingDates = hearingDates;
            return this;
        }

        public StandardCourtList build() {
            final StandardCourtList standardCourtList = new StandardCourtList();
            standardCourtList.courtCentreName = this.courtCentreName;
            standardCourtList.courtCentreDefaultStartTime = this.courtCentreDefaultStartTime;
            standardCourtList.courtCentreAddress1 = this.courtCentreAddress1;
            standardCourtList.courtCentreAddress2 = this.courtCentreAddress2;
            standardCourtList.welshCourtCentreName= this.welshCourtCentreName;
            standardCourtList.welshCourtCentreAddress1= this.welshCourtCentreAddress1;
            standardCourtList.welshCourtCentreAddress2= this.welshCourtCentreAddress2;
            standardCourtList.listType = this.listType;
            standardCourtList.hearingDates = this.hearingDates;
            return standardCourtList;
        }
    }

    @Override
    public String toString() {
        return "StandardCourtList{" +
                "listType='" + listType + '\'' +
                ", courtCentreName='" + courtCentreName + '\'' +
                ", courtCentreDefaultStartTime='" + courtCentreDefaultStartTime + '\'' +
                ", courtCentreAddress1='" + courtCentreAddress1 + '\'' +
                ", courtCentreAddress2='" + courtCentreAddress2 + '\'' +
                ", welshCourtCentreName='" + welshCourtCentreName + '\'' +
                ", welshCourtCentreAddress1='" + welshCourtCentreAddress1 + '\'' +
                ", welshCourtCentreAddress2='" + welshCourtCentreAddress2 + '\'' +
                ", hearingDates=" + hearingDates +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StandardCourtList)) return false;
        StandardCourtList that = (StandardCourtList) o;
        return Objects.equals(listType, that.listType) &&
                Objects.equals(courtCentreName, that.courtCentreName) &&
                Objects.equals(courtCentreDefaultStartTime, that.courtCentreDefaultStartTime) &&
                Objects.equals(courtCentreAddress1, that.courtCentreAddress1) &&
                Objects.equals(courtCentreAddress2, that.courtCentreAddress2) &&
                Objects.equals(welshCourtCentreName, that.welshCourtCentreName) &&
                Objects.equals(welshCourtCentreAddress1, that.welshCourtCentreAddress1) &&
                Objects.equals(welshCourtCentreAddress2, that.welshCourtCentreAddress2) &&
                Objects.equals(hearingDates, that.hearingDates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listType, courtCentreName, courtCentreDefaultStartTime, courtCentreAddress1, courtCentreAddress2, welshCourtCentreName, welshCourtCentreAddress1, welshCourtCentreAddress2, hearingDates);
    }
}
