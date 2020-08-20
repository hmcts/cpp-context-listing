package uk.gov.moj.cpp.listing.domain.aggregate;

import java.io.Serializable;

@SuppressWarnings({"squid:S00107", "squid:S1067", "PMD.BeanMembersShouldSerialize"})
public class StatementOfOffence implements Serializable {
    private final String legislation;

    private final String title;

    private final String welshLegislation;

    private final String welshTitle;

    public StatementOfOffence(final String legislation, final String title, final String welshLegislation, final String welshTitle) {
        this.legislation = legislation;
        this.title = title;
        this.welshLegislation = welshLegislation;
        this.welshTitle = welshTitle;
    }

    public String getLegislation() {
        return legislation;
    }

    public String getTitle() {
        return title;
    }

    public String getWelshLegislation() {
        return welshLegislation;
    }

    public String getWelshTitle() {
        return welshTitle;
    }

    public static Builder statementOfOffence() {
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

        final StatementOfOffence that = (StatementOfOffence) obj;

        return java.util.Objects.equals(this.legislation, that.legislation) &&
                java.util.Objects.equals(this.title, that.title) &&
                java.util.Objects.equals(this.welshLegislation, that.welshLegislation) &&
                java.util.Objects.equals(this.welshTitle, that.welshTitle);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(legislation, title, welshLegislation, welshTitle);
    }

    @Override
    public String toString() {
        return "StatementOfOffence{" +
                "legislation='" + legislation + "'," +
                "title='" + title + "'," +
                "welshLegislation='" + welshLegislation + "'," +
                "welshTitle='" + welshTitle + "'" +
                "}";
    }

    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static final class Builder {
        private String legislation;

        private String title;

        private String welshLegislation;

        private String welshTitle;

        public Builder withLegislation(final String legislation) {
            this.legislation = legislation;
            return this;
        }

        public Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public Builder withWelshLegislation(final String welshLegislation) {
            this.welshLegislation = welshLegislation;
            return this;
        }


        public Builder withWelshTitle(final String welshTitle) {
            this.welshTitle = welshTitle;
            return this;
        }

        public StatementOfOffence build() {
            return new StatementOfOffence(legislation, title, welshLegislation, welshTitle);
        }
    }
}
