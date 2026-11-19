package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class JudicialRole implements Serializable {
  private static final long serialVersionUID = 7964617314504523615L;

  private final Boolean isBenchChairman;

  private final Boolean isDeputy;

  private final UUID judicialId;

  private final JudicialRoleType judicialRoleType;

  private final UUID userId;

  @JsonCreator
  public JudicialRole(final Boolean isBenchChairman, final Boolean isDeputy, final UUID judicialId, final JudicialRoleType judicialRoleType, final UUID userId) {
    this.isBenchChairman = isBenchChairman;
    this.isDeputy = isDeputy;
    this.judicialId = judicialId;
    this.judicialRoleType = judicialRoleType;
    this.userId = userId;
  }

  public Boolean getIsBenchChairman() {
    return isBenchChairman;
  }

  public Boolean getIsDeputy() {
    return isDeputy;
  }

  public UUID getJudicialId() {
    return judicialId;
  }

  public JudicialRoleType getJudicialRoleType() {
    return judicialRoleType;
  }

  public UUID getUserId() {
    return userId;
  }

  public static Builder judicialRole() {
    return new uk.gov.justice.listing.events.JudicialRole.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.JudicialRole that = (uk.gov.justice.listing.events.JudicialRole) obj;

    return java.util.Objects.equals(this.isBenchChairman, that.isBenchChairman) &&
    java.util.Objects.equals(this.isDeputy, that.isDeputy) &&
    java.util.Objects.equals(this.judicialId, that.judicialId) &&
    java.util.Objects.equals(this.judicialRoleType, that.judicialRoleType) &&
    java.util.Objects.equals(this.userId, that.userId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(isBenchChairman, isDeputy, judicialId, judicialRoleType, userId);}

  public static class Builder {
    private Boolean isBenchChairman;

    private Boolean isDeputy;

    private UUID judicialId;

    private JudicialRoleType judicialRoleType;

    private UUID userId;

    public Builder withIsBenchChairman(final Boolean isBenchChairman) {
      this.isBenchChairman = isBenchChairman;
      return this;
    }

    public Builder withIsDeputy(final Boolean isDeputy) {
      this.isDeputy = isDeputy;
      return this;
    }

    public Builder withJudicialId(final UUID judicialId) {
      this.judicialId = judicialId;
      return this;
    }

    public Builder withJudicialRoleType(final JudicialRoleType judicialRoleType) {
      this.judicialRoleType = judicialRoleType;
      return this;
    }

    public Builder withUserId(final UUID userId) {
      this.userId = userId;
      return this;
    }

    public Builder withValuesFrom(final JudicialRole judicialRole) {
      this.isBenchChairman = judicialRole.getIsBenchChairman();
      this.isDeputy = judicialRole.getIsDeputy();
      this.judicialId = judicialRole.getJudicialId();
      this.judicialRoleType = judicialRole.getJudicialRoleType();
      this.userId = judicialRole.getUserId();
      return this;
    }

    public JudicialRole build() {
      return new uk.gov.justice.listing.events.JudicialRole(isBenchChairman, isDeputy, judicialId, judicialRoleType, userId);
    }
  }
}
