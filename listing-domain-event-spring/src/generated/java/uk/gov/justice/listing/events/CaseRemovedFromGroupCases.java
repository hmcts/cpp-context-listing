package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.case-removed-from-group-cases")
public class CaseRemovedFromGroupCases implements Serializable {
  private static final long serialVersionUID = -2415439462079282658L;

  private final UUID groupId;

  private final UUID hearingId;

  private final ListedCase newGroupMaster;

  private final Integer numberOfGroupCases;

  private final ListedCase removedCase;

  @JsonCreator
  public CaseRemovedFromGroupCases(final UUID groupId, final UUID hearingId, final ListedCase newGroupMaster, final Integer numberOfGroupCases, final ListedCase removedCase) {
    this.groupId = groupId;
    this.hearingId = hearingId;
    this.newGroupMaster = newGroupMaster;
    this.numberOfGroupCases = numberOfGroupCases;
    this.removedCase = removedCase;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public ListedCase getNewGroupMaster() {
    return newGroupMaster;
  }

  public Integer getNumberOfGroupCases() {
    return numberOfGroupCases;
  }

  public ListedCase getRemovedCase() {
    return removedCase;
  }

  public static Builder caseRemovedFromGroupCases() {
    return new uk.gov.justice.listing.events.CaseRemovedFromGroupCases.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CaseRemovedFromGroupCases that = (uk.gov.justice.listing.events.CaseRemovedFromGroupCases) obj;

    return java.util.Objects.equals(this.groupId, that.groupId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.newGroupMaster, that.newGroupMaster) &&
    java.util.Objects.equals(this.numberOfGroupCases, that.numberOfGroupCases) &&
    java.util.Objects.equals(this.removedCase, that.removedCase);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(groupId, hearingId, newGroupMaster, numberOfGroupCases, removedCase);}

  public static class Builder {
    private UUID groupId;

    private UUID hearingId;

    private ListedCase newGroupMaster;

    private Integer numberOfGroupCases;

    private ListedCase removedCase;

    public Builder withGroupId(final UUID groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withNewGroupMaster(final ListedCase newGroupMaster) {
      this.newGroupMaster = newGroupMaster;
      return this;
    }

    public Builder withNumberOfGroupCases(final Integer numberOfGroupCases) {
      this.numberOfGroupCases = numberOfGroupCases;
      return this;
    }

    public Builder withRemovedCase(final ListedCase removedCase) {
      this.removedCase = removedCase;
      return this;
    }

    public Builder withValuesFrom(final CaseRemovedFromGroupCases caseRemovedFromGroupCases) {
      this.groupId = caseRemovedFromGroupCases.getGroupId();
      this.hearingId = caseRemovedFromGroupCases.getHearingId();
      this.newGroupMaster = caseRemovedFromGroupCases.getNewGroupMaster();
      this.numberOfGroupCases = caseRemovedFromGroupCases.getNumberOfGroupCases();
      this.removedCase = caseRemovedFromGroupCases.getRemovedCase();
      return this;
    }

    public CaseRemovedFromGroupCases build() {
      return new uk.gov.justice.listing.events.CaseRemovedFromGroupCases(groupId, hearingId, newGroupMaster, numberOfGroupCases, removedCase);
    }
  }
}
