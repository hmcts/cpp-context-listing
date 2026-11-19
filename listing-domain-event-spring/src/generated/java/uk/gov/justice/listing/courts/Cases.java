package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class Cases implements Serializable {
  private static final long serialVersionUID = -7705421701803235390L;

  private final UUID caseId;

  private final String caseUrn;

  private final List<LinkedToCases> linkedToCases;

  @JsonCreator
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
    return new uk.gov.justice.listing.courts.Cases.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.Cases that = (uk.gov.justice.listing.courts.Cases) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.caseUrn, that.caseUrn) &&
    java.util.Objects.equals(this.linkedToCases, that.linkedToCases);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, caseUrn, linkedToCases);}

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

    public Builder withValuesFrom(final Cases cases) {
      this.caseId = cases.getCaseId();
      this.caseUrn = cases.getCaseUrn();
      this.linkedToCases = cases.getLinkedToCases();
      return this;
    }

    public Cases build() {
      return new uk.gov.justice.listing.courts.Cases(caseId, caseUrn, linkedToCases);
    }
  }
}
