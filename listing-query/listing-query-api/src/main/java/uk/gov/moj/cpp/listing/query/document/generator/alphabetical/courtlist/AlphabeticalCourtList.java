package uk.gov.moj.cpp.listing.query.document.generator.alphabetical.courtlist;

import java.util.List;

public class AlphabeticalCourtList {

    private String courtCentreName;
    private String welshCourtCentreName;
    private String hearingDate;
    private String welshHearingDate;
    private String welshCourtCentreAddress1;
    private String welshCourtCentreAddress2;
    private String courtCentreAddress1;
    private String courtCentreAddress2;
    private Boolean welsh;

    private List<AlphabeticalListDefendant> defendants;

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public String getHearingDate() {
        return hearingDate;
    }

    public String getCourtCentreAddress1() {
        return courtCentreAddress1;
    }

    public String getCourtCentreAddress2() {
        return courtCentreAddress2;
    }

    public List<AlphabeticalListDefendant> getDefendants() {
        return defendants;
    }

    public String getWelshCourtCentreName() {
        return welshCourtCentreName;
    }

    public String getWelshHearingDate() {
        return welshHearingDate;
    }

    public String getWelshCourtCentreAddress1() {
        return welshCourtCentreAddress1;
    }

    public String getWelshCourtCentreAddress2() {
        return welshCourtCentreAddress2;
    }

    public Boolean getWelsh() {
        return welsh;
    }

    public static final class AlphabeticalCourtListBuilder {
        private String courtCentreName;
        private String welshCourtCentreName;
        private String hearingDate;
        private String welshHearingDate;
        private String welshCourtCentreAddress1;
        private String welshCourtCentreAddress2;
        private String courtCentreAddress1;
        private String courtCentreAddress2;
        private Boolean welsh;
        private List<AlphabeticalListDefendant> defendants;

        private AlphabeticalCourtListBuilder() {
        }

        public static AlphabeticalCourtListBuilder anAlphabeticalCourtList() {
            return new AlphabeticalCourtListBuilder();
        }

        public AlphabeticalCourtListBuilder withCourtCentreName(String courtCentreName) {
            this.courtCentreName = courtCentreName;
            return this;
        }

        public AlphabeticalCourtListBuilder withWelshCourtCentreName(String welshCourtCentreName) {
            this.welshCourtCentreName = welshCourtCentreName;
            return this;
        }

        public AlphabeticalCourtListBuilder withHearingDate(String hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public AlphabeticalCourtListBuilder withWelshHearingDate(String welshHearingDate) {
            this.welshHearingDate = welshHearingDate;
            return this;
        }

        public AlphabeticalCourtListBuilder withWelshCourtCentreAddress1(String welshCourtCentreAddress1) {
            this.welshCourtCentreAddress1 = welshCourtCentreAddress1;
            return this;
        }

        public AlphabeticalCourtListBuilder withWelshCourtCentreAddress2(String welshCourtCentreAddress2) {
            this.welshCourtCentreAddress2 = welshCourtCentreAddress2;
            return this;
        }

        public AlphabeticalCourtListBuilder withCourtCentreAddress1(String courtCentreAddress1) {
            this.courtCentreAddress1 = courtCentreAddress1;
            return this;
        }

        public AlphabeticalCourtListBuilder withCourtCentreAddress2(String courtCentreAddress2) {
            this.courtCentreAddress2 = courtCentreAddress2;
            return this;
        }

        public AlphabeticalCourtListBuilder withWelsh(Boolean welsh) {
            this.welsh = welsh;
            return this;
        }

        public AlphabeticalCourtListBuilder withDefendants(List<AlphabeticalListDefendant> defendants) {
            this.defendants = defendants;
            return this;
        }

        public AlphabeticalCourtList build() {
            AlphabeticalCourtList alphabeticalCourtList = new AlphabeticalCourtList();
            alphabeticalCourtList.hearingDate = this.hearingDate;
            alphabeticalCourtList.welshCourtCentreName = this.welshCourtCentreName;
            alphabeticalCourtList.welshCourtCentreAddress1 = this.welshCourtCentreAddress1;
            alphabeticalCourtList.welshCourtCentreAddress2 = this.welshCourtCentreAddress2;
            alphabeticalCourtList.courtCentreAddress2 = this.courtCentreAddress2;
            alphabeticalCourtList.defendants = this.defendants;
            alphabeticalCourtList.courtCentreName = this.courtCentreName;
            alphabeticalCourtList.courtCentreAddress1 = this.courtCentreAddress1;
            alphabeticalCourtList.welsh = this.welsh;
            alphabeticalCourtList.welshHearingDate = this.welshHearingDate;
            return alphabeticalCourtList;
        }
    }
}
