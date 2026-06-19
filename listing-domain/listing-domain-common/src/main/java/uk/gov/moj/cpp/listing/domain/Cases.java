package uk.gov.moj.cpp.listing.domain;

import java.util.List;
import java.util.UUID;

public class Cases {

  private final UUID caseId;

  private final String caseUrn;

  private final List<LinkedToCases> linkedToCases;

  public Cases(final UUID caseId, final String caseUrn, final List<LinkedToCases> linkedToCases) {
    this.caseId = caseId;
    this.caseUrn = caseUrn;
    this.linkedToCases = linkedToCases;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public String getCaseUrn() {
    return caseUrn;
  }

  public List<LinkedToCases> getLinkedToCases() {
    return linkedToCases;
  }

  public static Builder cases() {
    return new Cases.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Cases that = (Cases) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.caseUrn, that.caseUrn) &&
    java.util.Objects.equals(this.linkedToCases, that.linkedToCases);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, caseUrn, linkedToCases);
  }

  @Override
  public String toString() {
    return "Cases{" +
    	"caseId='" + caseId + "'," +
    	"caseUrn='" + caseUrn + "'," +
        "linkedToCases='" + linkedToCases + "'" +
    "}";
  }

  public static class Builder {

    private UUID caseId;

    private String caseUrn;

    private List<LinkedToCases> linkedToCases;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withCaseUrn(final String caseUrn) {
      this.caseUrn = caseUrn;
      return this;
    }

    public Builder withLinkedToCases(final List<LinkedToCases> linkedToCases) {
      this.linkedToCases = linkedToCases;
      return this;
    }

    public Cases build() {
      return new Cases(caseId, caseUrn, linkedToCases);
    }
  }
}
