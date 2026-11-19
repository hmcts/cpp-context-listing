package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class SeedingHearing implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final JurisdictionType jurisdictionType;

  private final UUID seedingHearingId;

  private final String sittingDay;

  @JsonCreator
  public SeedingHearing(final JurisdictionType jurisdictionType, final UUID seedingHearingId, final String sittingDay) {
    this.jurisdictionType = jurisdictionType;
    this.seedingHearingId = seedingHearingId;
    this.sittingDay = sittingDay;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public UUID getSeedingHearingId() {
    return seedingHearingId;
  }

  public String getSittingDay() {
    return sittingDay;
  }

  public static Builder seedingHearing() {
    return new uk.gov.justice.core.courts.SeedingHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.SeedingHearing that = (uk.gov.justice.core.courts.SeedingHearing) obj;

    return java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId) &&
    java.util.Objects.equals(this.sittingDay, that.sittingDay);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(jurisdictionType, seedingHearingId, sittingDay);}

  public static class Builder {
    private JurisdictionType jurisdictionType;

    private UUID seedingHearingId;

    private String sittingDay;

    public Builder withJurisdictionType(final JurisdictionType jurisdictionType) {
      this.jurisdictionType = jurisdictionType;
      return this;
    }

    public Builder withSeedingHearingId(final UUID seedingHearingId) {
      this.seedingHearingId = seedingHearingId;
      return this;
    }

    public Builder withSittingDay(final String sittingDay) {
      this.sittingDay = sittingDay;
      return this;
    }

    public Builder withValuesFrom(final SeedingHearing seedingHearing) {
      this.jurisdictionType = seedingHearing.getJurisdictionType();
      this.seedingHearingId = seedingHearing.getSeedingHearingId();
      this.sittingDay = seedingHearing.getSittingDay();
      return this;
    }

    public SeedingHearing build() {
      return new uk.gov.justice.core.courts.SeedingHearing(jurisdictionType, seedingHearingId, sittingDay);
    }
  }
}
