package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.Objects;

public class ReportingRestriction {

    private String label;

    public String getLabel() {
        return label;
    }

    public static Builder reportingRestriction() {
        return new Builder();
    }



    public static final class Builder {
        private String label;

        private Builder() {
        }


        public Builder withLabel(String label) {
            this.label = label;
            return this;
        }

        public ReportingRestriction build() {
            final ReportingRestriction restriction = new ReportingRestriction();
            restriction.label = this.label;
            return restriction;
        }

    }

    @Override
    public String toString() {
        return "ReportingRestriction{" +
                "label='" + label + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportingRestriction)) {
            return false;
        }
        final ReportingRestriction reportingRestriction = (ReportingRestriction) o;
        return Objects.equals(label, reportingRestriction.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }
}
