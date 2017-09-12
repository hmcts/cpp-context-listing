package uk.gov.moj.cpp.listing.persistence.entity;

public class StatementOfOffenceBuilder {
    private String title;
    private String legislation;

    public StatementOfOffenceBuilder setTitle(final String title) {
        this.title = title;
        return this;
    }

    public StatementOfOffenceBuilder setLegislation(final String legislation) {
        this.legislation = legislation;
        return this;
    }

    public StatementOfOffence build() {
        return new StatementOfOffence(title, legislation);
    }
}