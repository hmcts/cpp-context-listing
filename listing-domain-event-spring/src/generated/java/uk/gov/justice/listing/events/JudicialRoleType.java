package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class JudicialRoleType implements Serializable {
  private static final long serialVersionUID = 7964617314504523615L;

  private final UUID judicialRoleTypeId;

  private final String judiciaryType;

  @JsonCreator
  public JudicialRoleType(final UUID judicialRoleTypeId, final String judiciaryType) {
    this.judicialRoleTypeId = judicialRoleTypeId;
    this.judiciaryType = judiciaryType;
  }

  public UUID getJudicialRoleTypeId() {
    return judicialRoleTypeId;
  }

  public String getJudiciaryType() {
    return judiciaryType;
  }

  public static Builder judicialRoleType() {
    return new uk.gov.justice.listing.events.JudicialRoleType.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.JudicialRoleType that = (uk.gov.justice.listing.events.JudicialRoleType) obj;

    return java.util.Objects.equals(this.judicialRoleTypeId, that.judicialRoleTypeId) &&
    java.util.Objects.equals(this.judiciaryType, that.judiciaryType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(judicialRoleTypeId, judiciaryType);}

  public static class Builder {
    private UUID judicialRoleTypeId;

    private String judiciaryType;

    public Builder withJudicialRoleTypeId(final UUID judicialRoleTypeId) {
      this.judicialRoleTypeId = judicialRoleTypeId;
      return this;
    }

    public Builder withJudiciaryType(final String judiciaryType) {
      this.judiciaryType = judiciaryType;
      return this;
    }

    public Builder withValuesFrom(final JudicialRoleType judicialRoleType) {
      this.judicialRoleTypeId = judicialRoleType.getJudicialRoleTypeId();
      this.judiciaryType = judicialRoleType.getJudiciaryType();
      return this;
    }

    public JudicialRoleType build() {
      return new uk.gov.justice.listing.events.JudicialRoleType(judicialRoleTypeId, judiciaryType);
    }
  }
}
