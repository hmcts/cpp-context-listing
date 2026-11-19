package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CommittingCourt implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final UUID courtCentreId;

  private final String courtHouseCode;

  private final String courtHouseName;

  private final String courtHouseShortName;

  private final JurisdictionType courtHouseType;

  @JsonCreator
  public CommittingCourt(final UUID courtCentreId, final String courtHouseCode, final String courtHouseName, final String courtHouseShortName, final JurisdictionType courtHouseType) {
    this.courtCentreId = courtCentreId;
    this.courtHouseCode = courtHouseCode;
    this.courtHouseName = courtHouseName;
    this.courtHouseShortName = courtHouseShortName;
    this.courtHouseType = courtHouseType;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public String getCourtHouseCode() {
    return courtHouseCode;
  }

  public String getCourtHouseName() {
    return courtHouseName;
  }

  public String getCourtHouseShortName() {
    return courtHouseShortName;
  }

  public JurisdictionType getCourtHouseType() {
    return courtHouseType;
  }

  public static Builder committingCourt() {
    return new uk.gov.justice.core.courts.CommittingCourt.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CommittingCourt that = (uk.gov.justice.core.courts.CommittingCourt) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtHouseCode, that.courtHouseCode) &&
    java.util.Objects.equals(this.courtHouseName, that.courtHouseName) &&
    java.util.Objects.equals(this.courtHouseShortName, that.courtHouseShortName) &&
    java.util.Objects.equals(this.courtHouseType, that.courtHouseType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtHouseCode, courtHouseName, courtHouseShortName, courtHouseType);}

  public static class Builder {
    private UUID courtCentreId;

    private String courtHouseCode;

    private String courtHouseName;

    private String courtHouseShortName;

    private JurisdictionType courtHouseType;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtHouseCode(final String courtHouseCode) {
      this.courtHouseCode = courtHouseCode;
      return this;
    }

    public Builder withCourtHouseName(final String courtHouseName) {
      this.courtHouseName = courtHouseName;
      return this;
    }

    public Builder withCourtHouseShortName(final String courtHouseShortName) {
      this.courtHouseShortName = courtHouseShortName;
      return this;
    }

    public Builder withCourtHouseType(final JurisdictionType courtHouseType) {
      this.courtHouseType = courtHouseType;
      return this;
    }

    public Builder withValuesFrom(final CommittingCourt committingCourt) {
      this.courtCentreId = committingCourt.getCourtCentreId();
      this.courtHouseCode = committingCourt.getCourtHouseCode();
      this.courtHouseName = committingCourt.getCourtHouseName();
      this.courtHouseShortName = committingCourt.getCourtHouseShortName();
      this.courtHouseType = committingCourt.getCourtHouseType();
      return this;
    }

    public CommittingCourt build() {
      return new uk.gov.justice.core.courts.CommittingCourt(courtCentreId, courtHouseCode, courtHouseName, courtHouseShortName, courtHouseType);
    }
  }
}
