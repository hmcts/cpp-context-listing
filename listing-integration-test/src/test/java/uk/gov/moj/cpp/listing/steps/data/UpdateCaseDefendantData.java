package uk.gov.moj.cpp.listing.steps.data;


import uk.gov.justice.core.courts.Defendant;

public class UpdateCaseDefendantData {
    private final Defendant defendant;

    public UpdateCaseDefendantData(final Defendant defendant) {
        this.defendant = defendant;
    }

    public Defendant getDefendant() {
        return defendant;
    }

    public static Builder updateCaseDefendantDetails() {
        return new UpdateCaseDefendantData.Builder();
    }


    public static class Builder {
        private Defendant defendant;

        public Builder withDefendant(final Defendant defendant) {
            this.defendant = defendant;
            return this;
        }

        public UpdateCaseDefendantData build() {
            return new UpdateCaseDefendantData(defendant);
        }
    }
}