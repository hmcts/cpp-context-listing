package uk.gov.moj.cpp.listing.domain;

import java.util.UUID;

@SuppressWarnings({"squid:S1067", "PMD:BeanMembersShouldSerialize"})
public class LinkedToCases {

  private final UUID caseId;

  private final String caseUrn;

  public LinkedToCases(final UUID caseId, final String caseUrn) {
    this.caseId = caseId;
    this.caseUrn = caseUrn;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public String getCaseUrn() {
    return caseUrn;
  }

  public static Builder linkedToCases() {
    return new LinkedToCases.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final LinkedToCases that = (LinkedToCases) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.caseUrn, that.caseUrn);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, caseUrn);
  }

  @Override
  public String toString() {
    return "LinkedToCases{" +
        "caseId='" + caseId + "'," +
    	"caseUrn='" + caseUrn + "'" +
    "}";
  }

  public static class Builder {

    private UUID caseId ;

    private String caseUrn;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withCaseUrn(final String caseUrn) {
      this.caseUrn = caseUrn;
      return this;
    }

    public LinkedToCases build() {
      return new LinkedToCases(caseId, caseUrn);
    }
  }
}
