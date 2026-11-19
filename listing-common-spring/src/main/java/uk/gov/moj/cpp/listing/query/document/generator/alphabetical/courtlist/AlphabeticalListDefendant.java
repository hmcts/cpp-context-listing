package uk.gov.moj.cpp.listing.query.document.generator.alphabetical.courtlist;

public class AlphabeticalListDefendant {

    private String defendantFullName;
    private String caseReference;
    private String courtRoomName;
    private String hearingStartTime;
    private String courtRoomNameWelsh;
    public String getDefendantFullName() {
        return defendantFullName;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public String getCourtRoomName() {
        return courtRoomName;
    }

    public String getHearingStartTime() {
        return hearingStartTime;
    }

    public String getCourtRoomNameWelsh() {
        return courtRoomNameWelsh;
    }

    public static final class AlphabeticalListDefendantBuilder {
        private String defendantFullName;
        private String caseReference;
        private String courtRoomName;
        private String hearingStartTime;
        private String courtRoomNameWelsh;

        private AlphabeticalListDefendantBuilder() {
        }

        public static AlphabeticalListDefendantBuilder anAlphabeticalListDefendant() {
            return new AlphabeticalListDefendantBuilder();
        }

        public AlphabeticalListDefendantBuilder withDefendantFullName(String defendantFullName) {
            this.defendantFullName = defendantFullName;
            return this;
        }

        public AlphabeticalListDefendantBuilder withCaseReference(String caseReference) {
            this.caseReference = caseReference;
            return this;
        }

        public AlphabeticalListDefendantBuilder withCourtRoomName(String courtRoomName) {
            this.courtRoomName = courtRoomName;
            return this;
        }

        public AlphabeticalListDefendantBuilder withHearingStartTime(String hearingStartTime) {
            this.hearingStartTime = hearingStartTime;
            return this;
        }

        public AlphabeticalListDefendantBuilder withCourtRoomNameWelsh(String courtRoomNameWelsh) {
            this.courtRoomNameWelsh = courtRoomNameWelsh;
            return this;
        }

        public AlphabeticalListDefendant build() {
            AlphabeticalListDefendant alphabeticalListDefendant = new AlphabeticalListDefendant();
            alphabeticalListDefendant.courtRoomNameWelsh = this.courtRoomNameWelsh;
            alphabeticalListDefendant.caseReference = this.caseReference;
            alphabeticalListDefendant.defendantFullName = this.defendantFullName;
            alphabeticalListDefendant.hearingStartTime = this.hearingStartTime;
            alphabeticalListDefendant.courtRoomName = this.courtRoomName;
            return alphabeticalListDefendant;
        }
    }
}
