package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;

public class UpdateExistingHearing implements Serializable {
  private static final long serialVersionUID = -6483518391279372860L;

  private final UUID hearingId;

  private final String keyReason;

  private final String listType;

  private final List<ProsecutionCase> prosecutionCases;

  private final SeedingHearing seedingHearing;

  private final List<UUID> shadowListedOffences;

  private final String tier;

  @JsonCreator
  public UpdateExistingHearing(final UUID hearingId, final String keyReason, final String listType, final List<ProsecutionCase> prosecutionCases, final SeedingHearing seedingHearing, final List<UUID> shadowListedOffences, final String tier) {
    this.hearingId = hearingId;
    this.keyReason = keyReason;
    this.listType = listType;
    this.prosecutionCases = prosecutionCases;
    this.seedingHearing = seedingHearing;
    this.shadowListedOffences = shadowListedOffences;
    this.tier = tier;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getKeyReason() {
    return keyReason;
  }

  public String getListType() {
    return listType;
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

  public String getTier() {
    return tier;
  }

  public static Builder updateExistingHearing() {
    return new uk.gov.justice.listing.courts.UpdateExistingHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateExistingHearing that = (uk.gov.justice.listing.courts.UpdateExistingHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.keyReason, that.keyReason) &&
    java.util.Objects.equals(this.listType, that.listType) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences) &&
    java.util.Objects.equals(this.tier, that.tier);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, keyReason, listType, prosecutionCases, seedingHearing, shadowListedOffences, tier);}

  public static class Builder {
    private UUID hearingId;

    private String keyReason;

    private String listType;

    private List<ProsecutionCase> prosecutionCases;

    private SeedingHearing seedingHearing;

    private List<UUID> shadowListedOffences;

    private String tier;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withKeyReason(final String keyReason) {
      this.keyReason = keyReason;
      return this;
    }

    public Builder withListType(final String listType) {
      this.listType = listType;
      return this;
    }

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

    public Builder withTier(final String tier) {
      this.tier = tier;
      return this;
    }

    public Builder withValuesFrom(final UpdateExistingHearing updateExistingHearing) {
      this.hearingId = updateExistingHearing.getHearingId();
      this.keyReason = updateExistingHearing.getKeyReason();
      this.listType = updateExistingHearing.getListType();
      this.prosecutionCases = updateExistingHearing.getProsecutionCases();
      this.seedingHearing = updateExistingHearing.getSeedingHearing();
      this.shadowListedOffences = updateExistingHearing.getShadowListedOffences();
      this.tier = updateExistingHearing.getTier();
      return this;
    }

    public UpdateExistingHearing build() {
      return new uk.gov.justice.listing.courts.UpdateExistingHearing(hearingId, keyReason, listType, prosecutionCases, seedingHearing, shadowListedOffences, tier);
    }
  }
}
