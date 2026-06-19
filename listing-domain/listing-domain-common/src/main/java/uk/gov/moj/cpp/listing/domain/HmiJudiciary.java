package uk.gov.moj.cpp.listing.domain;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067", "squid:S2065", "pmd:BeanMembersShouldSerialize"})
public class HmiJudiciary {
    private final Boolean isPresiding;

    private final String judiciaryId;

    private final String judiciaryType;

    public HmiJudiciary(final Boolean isPresiding, final String judiciaryId, final String judiciaryType) {
        this.isPresiding = isPresiding;
        this.judiciaryId = judiciaryId;
        this.judiciaryType = judiciaryType;
    }

    public Boolean getIsPresiding() {
        return isPresiding;
    }

    public String getJudiciaryId() {
        return judiciaryId;
    }

    public String getJudiciaryType() {
        return judiciaryType;
    }

    public static Builder hmiJudiciary() {
        return new uk.gov.moj.cpp.listing.domain.HmiJudiciary.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final uk.gov.moj.cpp.listing.domain.HmiJudiciary that = (uk.gov.moj.cpp.listing.domain.HmiJudiciary) obj;

        return java.util.Objects.equals(this.isPresiding, that.isPresiding) &&
                java.util.Objects.equals(this.judiciaryId, that.judiciaryId) &&
                java.util.Objects.equals(this.judiciaryType, that.judiciaryType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(isPresiding, judiciaryId, judiciaryType);
    }

    @Override
    public String toString() {
        return "HmiJudiciary{" +
                "isPresiding='" + isPresiding + "'," +
                "judiciaryId='" + judiciaryId + "'," +
                "judiciaryType='" + judiciaryType + "'" +
                "}";
    }

    public static class Builder {
        private Boolean isPresiding;

        private String judiciaryId;

        private String judiciaryType;

        public Builder withIsPresiding(final Boolean isPresiding) {
            this.isPresiding = isPresiding;
            return this;
        }

        public Builder withJudiciaryId(final String judiciaryId) {
            this.judiciaryId = judiciaryId;
            return this;
        }

        public Builder withJudiciaryType(final String judiciaryType) {
            this.judiciaryType = judiciaryType;
            return this;
        }

        public Builder withValuesFrom(final HmiJudiciary hmiJudiciary) {
            this.isPresiding = hmiJudiciary.getIsPresiding();
            this.judiciaryId = hmiJudiciary.getJudiciaryId();
            this.judiciaryType = hmiJudiciary.getJudiciaryType();
            return this;
        }

        public HmiJudiciary build() {
            return new uk.gov.moj.cpp.listing.domain.HmiJudiciary(isPresiding, judiciaryId, judiciaryType);
        }
    }
}
