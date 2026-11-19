package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCase;

public class AddCasesToHearing implements Serializable {
  private static final long serialVersionUID = 5822659656525901303L;

  private final UUID hearingId;

  private final String keyReason;

  private final String listType;

  private final List<ProsecutionCase> prosecutionCases;

  private final UUID seedingHearingId;

  private final List<UUID> shadowListedOffences;

  private final String tier;

  @JsonCreator
  public AddCasesToHearing(final UUID hearingId, final String keyReason, final String listType, final List<ProsecutionCase> prosecutionCases, final UUID seedingHearingId, final List<UUID> shadowListedOffences, final String tier) {
    this.hearingId = hearingId;
    this.keyReason = keyReason;
    this.listType = listType;
    this.prosecutionCases = prosecutionCases;
    this.seedingHearingId = seedingHearingId;
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

  public UUID getSeedingHearingId() {
    return seedingHearingId;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public String getTier() {
    return tier;
  }

  public static Builder addCasesToHearing() {
    return new uk.gov.justice.listing.courts.AddCasesToHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.AddCasesToHearing that = (uk.gov.justice.listing.courts.AddCasesToHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.keyReason, that.keyReason) &&
    java.util.Objects.equals(this.listType, that.listType) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences) &&
    java.util.Objects.equals(this.tier, that.tier);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, keyReason, listType, prosecutionCases, seedingHearingId, shadowListedOffences, tier);}

  public static class Builder {
    private UUID hearingId;

    private String keyReason;

    private String listType;

    private List<ProsecutionCase> prosecutionCases;

    private UUID seedingHearingId;

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

    public Builder withSeedingHearingId(final UUID seedingHearingId) {
      this.seedingHearingId = seedingHearingId;
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

    public Builder withValuesFrom(final AddCasesToHearing addCasesToHearing) {
      this.hearingId = addCasesToHearing.getHearingId();
      this.keyReason = addCasesToHearing.getKeyReason();
      this.listType = addCasesToHearing.getListType();
      this.prosecutionCases = addCasesToHearing.getProsecutionCases();
      this.seedingHearingId = addCasesToHearing.getSeedingHearingId();
      this.shadowListedOffences = addCasesToHearing.getShadowListedOffences();
      this.tier = addCasesToHearing.getTier();
      return this;
    }

    public AddCasesToHearing build() {
      return new uk.gov.justice.listing.courts.AddCasesToHearing(hearingId, keyReason, listType, prosecutionCases, seedingHearingId, shadowListedOffences, tier);
    }
  }
}
