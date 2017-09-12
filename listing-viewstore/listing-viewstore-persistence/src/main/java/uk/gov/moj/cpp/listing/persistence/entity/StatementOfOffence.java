package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class StatementOfOffence implements Serializable {


    @Column(name = "statement_of_offence_title")
    private String title;

    @Column(name = "statement_of_offence_legislation")
    private String legislation;

    public StatementOfOffence() {
        // for JPA
    }

    public StatementOfOffence(final String title, final String legislation) {
        this.title = title;
        this.legislation = legislation;
    }

    public String getTitle() { return title; }

    public String getLegislation() { return legislation; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatementOfOffence that = (StatementOfOffence) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(legislation, that.legislation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, legislation);
    }
}
