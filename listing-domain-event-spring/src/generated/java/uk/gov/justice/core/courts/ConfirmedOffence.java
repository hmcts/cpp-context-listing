package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class ConfirmedOffence implements Serializable {
  private static final long serialVersionUID = -9219648287822407011L;

  private final CivilOffence civilOffence;

  private final UUID id;

  private final SeedingHearing seedingHearing;

  @JsonCreator
  public ConfirmedOffence(final CivilOffence civilOffence, final UUID id, final SeedingHearing seedingHearing) {
    this.civilOffence = civilOffence;
    this.id = id;
    this.seedingHearing = seedingHearing;
  }

  public CivilOffence getCivilOffence() {
    return civilOffence;
  }

  public UUID getId() {
    return id;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public static Builder confirmedOffence() {
    return new uk.gov.justice.core.courts.ConfirmedOffence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ConfirmedOffence that = (uk.gov.justice.core.courts.ConfirmedOffence) obj;

    return java.util.Objects.equals(this.civilOffence, that.civilOffence) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(civilOffence, id, seedingHearing);}

  public static class Builder {
    private CivilOffence civilOffence;

    private UUID id;

    private SeedingHearing seedingHearing;

    public Builder withCivilOffence(final CivilOffence civilOffence) {
      this.civilOffence = civilOffence;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withValuesFrom(final ConfirmedOffence confirmedOffence) {
      this.civilOffence = confirmedOffence.getCivilOffence();
      this.id = confirmedOffence.getId();
      this.seedingHearing = confirmedOffence.getSeedingHearing();
      return this;
    }

    public ConfirmedOffence build() {
      return new uk.gov.justice.core.courts.ConfirmedOffence(civilOffence, id, seedingHearing);
    }
  }
}
