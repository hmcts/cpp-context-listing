package uk.gov.moj.cpp.progression.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCase;

public class CaseRemovedFromGroupCases implements Serializable {
  private static final long serialVersionUID = 756553334324638398L;

  private final UUID groupId;

  private final UUID masterCaseId;

  private final ProsecutionCase newGroupMaster;

  private final Integer numberOfGroupCases;

  private final ProsecutionCase removedCase;

  @JsonCreator
  public CaseRemovedFromGroupCases(final UUID groupId, final UUID masterCaseId, final ProsecutionCase newGroupMaster, final Integer numberOfGroupCases, final ProsecutionCase removedCase) {
    this.groupId = groupId;
    this.masterCaseId = masterCaseId;
    this.newGroupMaster = newGroupMaster;
    this.numberOfGroupCases = numberOfGroupCases;
    this.removedCase = removedCase;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public UUID getMasterCaseId() {
    return masterCaseId;
  }

  public ProsecutionCase getNewGroupMaster() {
    return newGroupMaster;
  }

  public Integer getNumberOfGroupCases() {
    return numberOfGroupCases;
  }

  public ProsecutionCase getRemovedCase() {
    return removedCase;
  }

  public static Builder caseRemovedFromGroupCases() {
    return new uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases that = (uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases) obj;

    return java.util.Objects.equals(this.groupId, that.groupId) &&
    java.util.Objects.equals(this.masterCaseId, that.masterCaseId) &&
    java.util.Objects.equals(this.newGroupMaster, that.newGroupMaster) &&
    java.util.Objects.equals(this.numberOfGroupCases, that.numberOfGroupCases) &&
    java.util.Objects.equals(this.removedCase, that.removedCase);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(groupId, masterCaseId, newGroupMaster, numberOfGroupCases, removedCase);}

  public static class Builder {
    private UUID groupId;

    private UUID masterCaseId;

    private ProsecutionCase newGroupMaster;

    private Integer numberOfGroupCases;

    private ProsecutionCase removedCase;

    public Builder withGroupId(final UUID groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder withMasterCaseId(final UUID masterCaseId) {
      this.masterCaseId = masterCaseId;
      return this;
    }

    public Builder withNewGroupMaster(final ProsecutionCase newGroupMaster) {
      this.newGroupMaster = newGroupMaster;
      return this;
    }

    public Builder withNumberOfGroupCases(final Integer numberOfGroupCases) {
      this.numberOfGroupCases = numberOfGroupCases;
      return this;
    }

    public Builder withRemovedCase(final ProsecutionCase removedCase) {
      this.removedCase = removedCase;
      return this;
    }

    public Builder withValuesFrom(final CaseRemovedFromGroupCases caseRemovedFromGroupCases) {
      this.groupId = caseRemovedFromGroupCases.getGroupId();
      this.masterCaseId = caseRemovedFromGroupCases.getMasterCaseId();
      this.newGroupMaster = caseRemovedFromGroupCases.getNewGroupMaster();
      this.numberOfGroupCases = caseRemovedFromGroupCases.getNumberOfGroupCases();
      this.removedCase = caseRemovedFromGroupCases.getRemovedCase();
      return this;
    }

    public CaseRemovedFromGroupCases build() {
      return new uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases(groupId, masterCaseId, newGroupMaster, numberOfGroupCases, removedCase);
    }
  }
}
