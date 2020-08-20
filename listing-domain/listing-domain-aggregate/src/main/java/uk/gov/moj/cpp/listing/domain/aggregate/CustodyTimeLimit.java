package uk.gov.moj.cpp.listing.domain.aggregate;

import java.io.Serializable;

@SuppressWarnings({"squid:S00107", "squid:S1067", "PMD.BeanMembersShouldSerialize", "squid:S2384"})
public class CustodyTimeLimit implements Serializable {
    private final Integer daysSpent;

    private final String timeLimit;

    public CustodyTimeLimit(final Integer daysSpent, final String timeLimit) {
        this.daysSpent = daysSpent;
        this.timeLimit = timeLimit;
    }

    public Integer getDaysSpent() {
        return daysSpent;
    }

    public String getTimeLimit() {
        return timeLimit;
    }

    public static Builder custodyTimeLimit() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final CustodyTimeLimit that = (CustodyTimeLimit) obj;

        return java.util.Objects.equals(this.daysSpent, that.daysSpent) &&
                java.util.Objects.equals(this.timeLimit, that.timeLimit);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(daysSpent, timeLimit);
    }

    @Override
    public String toString() {
        return "CustodyTimeLimit{" +
                "daysSpent='" + daysSpent + "'," +
                "timeLimit='" + timeLimit + "'" +
                "}";
    }

    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static class Builder {
        private Integer daysSpent;

        private String timeLimit;

        public Builder withDaysSpent(final Integer daysSpent) {
            this.daysSpent = daysSpent;
            return this;
        }

        public Builder withTimeLimit(final String timeLimit) {
            this.timeLimit = timeLimit;
            return this;
        }

        public CustodyTimeLimit build() {
            return new CustodyTimeLimit(daysSpent, timeLimit);
        }
    }
}
