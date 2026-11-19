package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class ProsecutionCaseDefendantOffenceIds implements Serializable {
  private static final long serialVersionUID = 7964617314504523615L;

  private final List<DefendantOffenceIds> defendants;

  private final UUID groupId;

  private final UUID id;

  private final Boolean isCivil;

  private final Boolean isGroupMaster;

  private final Boolean isGroupMember;

  @JsonCreator
  public ProsecutionCaseDefendantOffenceIds(final List<DefendantOffenceIds> defendants, final UUID groupId, final UUID id, final Boolean isCivil, final Boolean isGroupMaster, final Boolean isGroupMember) {
    this.defendants = defendants;
    this.groupId = groupId;
    this.id = id;
    this.isCivil = isCivil;
    this.isGroupMaster = isGroupMaster;
    this.isGroupMember = isGroupMember;
  }

  public List<DefendantOffenceIds> getDefendants() {
    return defendants;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getIsCivil() {
    return isCivil;
  }

  public Boolean getIsGroupMaster() {
    return isGroupMaster;
  }

  public Boolean getIsGroupMember() {
    return isGroupMember;
  }

  public static Builder prosecutionCaseDefendantOffenceIds() {
    return new uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds that = (uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds) obj;

    return java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.groupId, that.groupId) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isCivil, that.isCivil) &&
    java.util.Objects.equals(this.isGroupMaster, that.isGroupMaster) &&
    java.util.Objects.equals(this.isGroupMember, that.isGroupMember);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendants, groupId, id, isCivil, isGroupMaster, isGroupMember);}

  public static class Builder {
    private List<DefendantOffenceIds> defendants;

    private UUID groupId;

    private UUID id;

    private Boolean isCivil;

    private Boolean isGroupMaster;

    private Boolean isGroupMember;

    public Builder withDefendants(final List<DefendantOffenceIds> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withGroupId(final UUID groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withIsCivil(final Boolean isCivil) {
      this.isCivil = isCivil;
      return this;
    }

    public Builder withIsGroupMaster(final Boolean isGroupMaster) {
      this.isGroupMaster = isGroupMaster;
      return this;
    }

    public Builder withIsGroupMember(final Boolean isGroupMember) {
      this.isGroupMember = isGroupMember;
      return this;
    }

    public Builder withValuesFrom(final ProsecutionCaseDefendantOffenceIds prosecutionCaseDefendantOffenceIds) {
      this.defendants = prosecutionCaseDefendantOffenceIds.getDefendants();
      this.groupId = prosecutionCaseDefendantOffenceIds.getGroupId();
      this.id = prosecutionCaseDefendantOffenceIds.getId();
      this.isCivil = prosecutionCaseDefendantOffenceIds.getIsCivil();
      this.isGroupMaster = prosecutionCaseDefendantOffenceIds.getIsGroupMaster();
      this.isGroupMember = prosecutionCaseDefendantOffenceIds.getIsGroupMember();
      return this;
    }

    public ProsecutionCaseDefendantOffenceIds build() {
      return new uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds(defendants, groupId, id, isCivil, isGroupMaster, isGroupMember);
    }
  }
}
