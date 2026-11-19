package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class CaseUrns implements Serializable {
  private static final long serialVersionUID = -7075021598336332236L;

  private final String caseURN;

  @JsonCreator
  public CaseUrns(final String caseURN) {
    this.caseURN = caseURN;
  }

  public String getCaseURN() {
    return caseURN;
  }

  public static Builder caseUrns() {
    return new uk.gov.justice.listing.courts.CaseUrns.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.CaseUrns that = (uk.gov.justice.listing.courts.CaseUrns) obj;

    return java.util.Objects.equals(this.caseURN, that.caseURN);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseURN);}

  public static class Builder {
    private String caseURN;

    public Builder withCaseURN(final String caseURN) {
      this.caseURN = caseURN;
      return this;
    }

    public Builder withValuesFrom(final CaseUrns caseUrns) {
      this.caseURN = caseUrns.getCaseURN();
      return this;
    }

    public CaseUrns build() {
      return new uk.gov.justice.listing.courts.CaseUrns(caseURN);
    }
  }
}
