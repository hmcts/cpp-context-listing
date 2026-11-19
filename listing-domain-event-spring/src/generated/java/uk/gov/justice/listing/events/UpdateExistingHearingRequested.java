package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.update-existing-hearing-requested")
public class UpdateExistingHearingRequested implements Serializable {
  private static final long serialVersionUID = 9028684049799052599L;

  private final String hearingDay;

  private final UUID hearingId;

  private final String keyReason;

  private final String listType;

  private final List<ProsecutionCase> prosecutionCases;

  private final UUID seedingHearingId;

  private final List<UUID> shadowListedOffences;

  private final String tier;

  @JsonCreator
  public UpdateExistingHearingRequested(final String hearingDay, final UUID hearingId, final String keyReason, final String listType, final List<ProsecutionCase> prosecutionCases, final UUID seedingHearingId, final List<UUID> shadowListedOffences, final String tier) {
    this.hearingDay = hearingDay;
    this.hearingId = hearingId;
    this.keyReason = keyReason;
    this.listType = listType;
    this.prosecutionCases = prosecutionCases;
    this.seedingHearingId = seedingHearingId;
    this.shadowListedOffences = shadowListedOffences;
    this.tier = tier;
  }

  public String getHearingDay() {
    return hearingDay;
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

  public static Builder updateExistingHearingRequested() {
    return new uk.gov.justice.listing.events.UpdateExistingHearingRequested.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.UpdateExistingHearingRequested that = (uk.gov.justice.listing.events.UpdateExistingHearingRequested) obj;

    return java.util.Objects.equals(this.hearingDay, that.hearingDay) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.keyReason, that.keyReason) &&
    java.util.Objects.equals(this.listType, that.listType) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences) &&
    java.util.Objects.equals(this.tier, that.tier);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingDay, hearingId, keyReason, listType, prosecutionCases, seedingHearingId, shadowListedOffences, tier);}

  public static class Builder {
    private String hearingDay;

    private UUID hearingId;

    private String keyReason;

    private String listType;

    private List<ProsecutionCase> prosecutionCases;

    private UUID seedingHearingId;

    private List<UUID> shadowListedOffences;

    private String tier;

    public Builder withHearingDay(final String hearingDay) {
      this.hearingDay = hearingDay;
      return this;
    }

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

    public Builder withValuesFrom(final UpdateExistingHearingRequested updateExistingHearingRequested) {
      this.hearingDay = updateExistingHearingRequested.getHearingDay();
      this.hearingId = updateExistingHearingRequested.getHearingId();
      this.keyReason = updateExistingHearingRequested.getKeyReason();
      this.listType = updateExistingHearingRequested.getListType();
      this.prosecutionCases = updateExistingHearingRequested.getProsecutionCases();
      this.seedingHearingId = updateExistingHearingRequested.getSeedingHearingId();
      this.shadowListedOffences = updateExistingHearingRequested.getShadowListedOffences();
      this.tier = updateExistingHearingRequested.getTier();
      return this;
    }

    public UpdateExistingHearingRequested build() {
      return new uk.gov.justice.listing.events.UpdateExistingHearingRequested(hearingDay, hearingId, keyReason, listType, prosecutionCases, seedingHearingId, shadowListedOffences, tier);
    }
  }
}
