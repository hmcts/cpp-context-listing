package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class LinkedCasesUpdated implements Serializable {
  private static final long serialVersionUID = -936508858039683746L;

  private final UUID caseId;

  private final String caseUrn;

  private final UUID hearingId;

  private final String linkActionType;

  private final List<LinkedToCases> linkedToCases;

  @JsonCreator
  public LinkedCasesUpdated(final UUID caseId, final String caseUrn, final UUID hearingId, final String linkActionType, final List<LinkedToCases> linkedToCases) {
    this.caseId = caseId;
    this.caseUrn = caseUrn;
    this.hearingId = hearingId;
    this.linkActionType = linkActionType;
    this.linkedToCases = linkedToCases;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public String getCaseUrn() {
    return caseUrn;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getLinkActionType() {
    return linkActionType;
  }

  public List<LinkedToCases> getLinkedToCases() {
    return linkedToCases;
  }

  public static Builder linkedCasesUpdated() {
    return new uk.gov.justice.listing.events.LinkedCasesUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.LinkedCasesUpdated that = (uk.gov.justice.listing.events.LinkedCasesUpdated) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.caseUrn, that.caseUrn) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.linkActionType, that.linkActionType) &&
    java.util.Objects.equals(this.linkedToCases, that.linkedToCases);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, caseUrn, hearingId, linkActionType, linkedToCases);}

  public static class Builder {
    private UUID caseId;

    private String caseUrn;

    private UUID hearingId;

    private String linkActionType;

    private List<LinkedToCases> linkedToCases;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withCaseUrn(final String caseUrn) {
      this.caseUrn = caseUrn;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withLinkActionType(final String linkActionType) {
      this.linkActionType = linkActionType;
      return this;
    }

    public Builder withLinkedToCases(final List<LinkedToCases> linkedToCases) {
      this.linkedToCases = linkedToCases;
      return this;
    }

    public Builder withValuesFrom(final LinkedCasesUpdated linkedCasesUpdated) {
      this.caseId = linkedCasesUpdated.getCaseId();
      this.caseUrn = linkedCasesUpdated.getCaseUrn();
      this.hearingId = linkedCasesUpdated.getHearingId();
      this.linkActionType = linkedCasesUpdated.getLinkActionType();
      this.linkedToCases = linkedCasesUpdated.getLinkedToCases();
      return this;
    }

    public LinkedCasesUpdated build() {
      return new uk.gov.justice.listing.events.LinkedCasesUpdated(caseId, caseUrn, hearingId, linkActionType, linkedToCases);
    }
  }
}
