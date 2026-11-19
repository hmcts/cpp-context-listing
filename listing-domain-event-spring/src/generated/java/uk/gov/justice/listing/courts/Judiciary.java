package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.JudicialRoleType;

public class Judiciary implements Serializable {
  private static final long serialVersionUID = -2582361536581928800L;

  private final IsBenchChairman isBenchChairman;

  private final IsDeputy isDeputy;

  private final UUID judicialId;

  private final JudicialRoleType judicialRoleType;

  private final UUID userId;

  @JsonCreator
  public Judiciary(final IsBenchChairman isBenchChairman, final IsDeputy isDeputy, final UUID judicialId, final JudicialRoleType judicialRoleType, final UUID userId) {
    this.isBenchChairman = isBenchChairman;
    this.isDeputy = isDeputy;
    this.judicialId = judicialId;
    this.judicialRoleType = judicialRoleType;
    this.userId = userId;
  }

  public IsBenchChairman getIsBenchChairman() {
    return isBenchChairman;
  }

  public IsDeputy getIsDeputy() {
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

  public static Builder judiciary() {
    return new uk.gov.justice.listing.courts.Judiciary.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.Judiciary that = (uk.gov.justice.listing.courts.Judiciary) obj;

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
    private IsBenchChairman isBenchChairman;

    private IsDeputy isDeputy;

    private UUID judicialId;

    private JudicialRoleType judicialRoleType;

    private UUID userId;

    public Builder withIsBenchChairman(final IsBenchChairman isBenchChairman) {
      this.isBenchChairman = isBenchChairman;
      return this;
    }

    public Builder withIsDeputy(final IsDeputy isDeputy) {
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

    public Builder withValuesFrom(final Judiciary judiciary) {
      this.isBenchChairman = judiciary.getIsBenchChairman();
      this.isDeputy = judiciary.getIsDeputy();
      this.judicialId = judiciary.getJudicialId();
      this.judicialRoleType = judiciary.getJudicialRoleType();
      this.userId = judiciary.getUserId();
      return this;
    }

    public Judiciary build() {
      return new uk.gov.justice.listing.courts.Judiciary(isBenchChairman, isDeputy, judicialId, judicialRoleType, userId);
    }
  }
}
