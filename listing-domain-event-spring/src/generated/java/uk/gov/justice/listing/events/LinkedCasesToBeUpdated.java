package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.linked-cases-to-be-updated")
public class LinkedCasesToBeUpdated implements Serializable {
  private static final long serialVersionUID = -8376068320449241658L;

  private final UUID caseId;

  private final String caseUrn;

  private final List<UUID> hearingIds;

  private final String linkActionType;

  private final List<LinkedToCases> linkedToCases;

  @JsonCreator
  public LinkedCasesToBeUpdated(final UUID caseId, final String caseUrn, final List<UUID> hearingIds, final String linkActionType, final List<LinkedToCases> linkedToCases) {
    this.caseId = caseId;
    this.caseUrn = caseUrn;
    this.hearingIds = hearingIds;
    this.linkActionType = linkActionType;
    this.linkedToCases = linkedToCases;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public String getCaseUrn() {
    return caseUrn;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
  }

  public String getLinkActionType() {
    return linkActionType;
  }

  public List<LinkedToCases> getLinkedToCases() {
    return linkedToCases;
  }

  public static Builder linkedCasesToBeUpdated() {
    return new uk.gov.justice.listing.events.LinkedCasesToBeUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.LinkedCasesToBeUpdated that = (uk.gov.justice.listing.events.LinkedCasesToBeUpdated) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.caseUrn, that.caseUrn) &&
    java.util.Objects.equals(this.hearingIds, that.hearingIds) &&
    java.util.Objects.equals(this.linkActionType, that.linkActionType) &&
    java.util.Objects.equals(this.linkedToCases, that.linkedToCases);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, caseUrn, hearingIds, linkActionType, linkedToCases);}

  public static class Builder {
    private UUID caseId;

    private String caseUrn;

    private List<UUID> hearingIds;

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

    public Builder withHearingIds(final List<UUID> hearingIds) {
      this.hearingIds = hearingIds;
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

    public Builder withValuesFrom(final LinkedCasesToBeUpdated linkedCasesToBeUpdated) {
      this.caseId = linkedCasesToBeUpdated.getCaseId();
      this.caseUrn = linkedCasesToBeUpdated.getCaseUrn();
      this.hearingIds = linkedCasesToBeUpdated.getHearingIds();
      this.linkActionType = linkedCasesToBeUpdated.getLinkActionType();
      this.linkedToCases = linkedCasesToBeUpdated.getLinkedToCases();
      return this;
    }

    public LinkedCasesToBeUpdated build() {
      return new uk.gov.justice.listing.events.LinkedCasesToBeUpdated(caseId, caseUrn, hearingIds, linkActionType, linkedToCases);
    }
  }
}
