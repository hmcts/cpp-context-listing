package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;

public class UpdateRelatedHearing implements Serializable {
  private static final long serialVersionUID = 760730603076586394L;

  private final List<ProsecutionCase> prosecutionCases;

  private final SeedingHearing seedingHearing;

  private final List<UUID> shadowListedOffences;

  @JsonCreator
  public UpdateRelatedHearing(final List<ProsecutionCase> prosecutionCases, final SeedingHearing seedingHearing, final List<UUID> shadowListedOffences) {
    this.prosecutionCases = prosecutionCases;
    this.seedingHearing = seedingHearing;
    this.shadowListedOffences = shadowListedOffences;
  }

  public List<ProsecutionCase> getProsecutionCases() {
    return prosecutionCases;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public static Builder updateRelatedHearing() {
    return new uk.gov.justice.listing.courts.UpdateRelatedHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateRelatedHearing that = (uk.gov.justice.listing.courts.UpdateRelatedHearing) obj;

    return java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(prosecutionCases, seedingHearing, shadowListedOffences);}

  public static class Builder {
    private List<ProsecutionCase> prosecutionCases;

    private SeedingHearing seedingHearing;

    private List<UUID> shadowListedOffences;

    public Builder withProsecutionCases(final List<ProsecutionCase> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withShadowListedOffences(final List<UUID> shadowListedOffences) {
      this.shadowListedOffences = shadowListedOffences;
      return this;
    }

    public Builder withValuesFrom(final UpdateRelatedHearing updateRelatedHearing) {
      this.prosecutionCases = updateRelatedHearing.getProsecutionCases();
      this.seedingHearing = updateRelatedHearing.getSeedingHearing();
      this.shadowListedOffences = updateRelatedHearing.getShadowListedOffences();
      return this;
    }

    public UpdateRelatedHearing build() {
      return new uk.gov.justice.listing.courts.UpdateRelatedHearing(prosecutionCases, seedingHearing, shadowListedOffences);
    }
  }
}
