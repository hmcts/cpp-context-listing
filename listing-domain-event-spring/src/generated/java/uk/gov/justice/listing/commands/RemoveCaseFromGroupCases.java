package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCase;

public class RemoveCaseFromGroupCases implements Serializable {
  private static final long serialVersionUID = 5831617285896704541L;

  private final UUID groupId;

  private final UUID masterCaseId;

  private final ProsecutionCase newGroupMaster;

  private final Integer numberOfGroupCases;

  private final ProsecutionCase removedCase;

  @JsonCreator
  public RemoveCaseFromGroupCases(final UUID groupId, final UUID masterCaseId, final ProsecutionCase newGroupMaster, final Integer numberOfGroupCases, final ProsecutionCase removedCase) {
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

  public static Builder removeCaseFromGroupCases() {
    return new uk.gov.justice.listing.commands.RemoveCaseFromGroupCases.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.RemoveCaseFromGroupCases that = (uk.gov.justice.listing.commands.RemoveCaseFromGroupCases) obj;

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

    public Builder withValuesFrom(final RemoveCaseFromGroupCases removeCaseFromGroupCases) {
      this.groupId = removeCaseFromGroupCases.getGroupId();
      this.masterCaseId = removeCaseFromGroupCases.getMasterCaseId();
      this.newGroupMaster = removeCaseFromGroupCases.getNewGroupMaster();
      this.numberOfGroupCases = removeCaseFromGroupCases.getNumberOfGroupCases();
      this.removedCase = removeCaseFromGroupCases.getRemovedCase();
      return this;
    }

    public RemoveCaseFromGroupCases build() {
      return new uk.gov.justice.listing.commands.RemoveCaseFromGroupCases(groupId, masterCaseId, newGroupMaster, numberOfGroupCases, removedCase);
    }
  }
}
