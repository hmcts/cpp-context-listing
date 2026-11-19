package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class LinkedCases implements Serializable {
  private static final long serialVersionUID = 641333116650746455L;

  private final UUID caseId;

  private final String caseUrn;

  @JsonCreator
  public LinkedCases(final UUID caseId, final String caseUrn) {
    this.caseId = caseId;
    this.caseUrn = caseUrn;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public String getCaseUrn() {
    return caseUrn;
  }

  public static Builder linkedCases() {
    return new uk.gov.justice.listing.events.LinkedCases.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.LinkedCases that = (uk.gov.justice.listing.events.LinkedCases) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.caseUrn, that.caseUrn);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, caseUrn);}

  public static class Builder {
    private UUID caseId;

    private String caseUrn;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withCaseUrn(final String caseUrn) {
      this.caseUrn = caseUrn;
      return this;
    }

    public Builder withValuesFrom(final LinkedCases linkedCases) {
      this.caseId = linkedCases.getCaseId();
      this.caseUrn = linkedCases.getCaseUrn();
      return this;
    }

    public LinkedCases build() {
      return new uk.gov.justice.listing.events.LinkedCases(caseId, caseUrn);
    }
  }
}
