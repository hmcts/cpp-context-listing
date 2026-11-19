package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class StatementOfOffence implements Serializable {
  private static final long serialVersionUID = -2619644771746664968L;

  private final String legislation;

  private final String title;

  private final String welshLegislation;

  private final String welshTitle;

  @JsonCreator
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
    return new uk.gov.justice.listing.commands.StatementOfOffence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.StatementOfOffence that = (uk.gov.justice.listing.commands.StatementOfOffence) obj;

    return java.util.Objects.equals(this.legislation, that.legislation) &&
    java.util.Objects.equals(this.title, that.title) &&
    java.util.Objects.equals(this.welshLegislation, that.welshLegislation) &&
    java.util.Objects.equals(this.welshTitle, that.welshTitle);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(legislation, title, welshLegislation, welshTitle);}

  public static class Builder {
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

    public Builder withValuesFrom(final StatementOfOffence statementOfOffence) {
      this.legislation = statementOfOffence.getLegislation();
      this.title = statementOfOffence.getTitle();
      this.welshLegislation = statementOfOffence.getWelshLegislation();
      this.welshTitle = statementOfOffence.getWelshTitle();
      return this;
    }

    public StatementOfOffence build() {
      return new uk.gov.justice.listing.commands.StatementOfOffence(legislation, title, welshLegislation, welshTitle);
    }
  }
}
