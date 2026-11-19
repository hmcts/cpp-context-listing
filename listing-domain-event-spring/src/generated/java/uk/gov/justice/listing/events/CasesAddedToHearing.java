package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class CasesAddedToHearing implements Serializable {
  private static final long serialVersionUID = -1618466693662006875L;

  private final Boolean addCasesToUnAllocatedHearing;

  private final UUID hearingId;

  private final String keyReason;

  private final String listType;

  private final UUID seedingHearingId;

  private final String tier;

  private final List<ListedCase> unAllocatedListedCases;

  @JsonCreator
  public CasesAddedToHearing(final Boolean addCasesToUnAllocatedHearing, final UUID hearingId, final String keyReason, final String listType, final UUID seedingHearingId, final String tier, final List<ListedCase> unAllocatedListedCases) {
    this.addCasesToUnAllocatedHearing = addCasesToUnAllocatedHearing;
    this.hearingId = hearingId;
    this.keyReason = keyReason;
    this.listType = listType;
    this.seedingHearingId = seedingHearingId;
    this.tier = tier;
    this.unAllocatedListedCases = unAllocatedListedCases;
  }

  public Boolean getAddCasesToUnAllocatedHearing() {
    return addCasesToUnAllocatedHearing;
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

  public UUID getSeedingHearingId() {
    return seedingHearingId;
  }

  public String getTier() {
    return tier;
  }

  public List<ListedCase> getUnAllocatedListedCases() {
    return unAllocatedListedCases;
  }

  public static Builder casesAddedToHearing() {
    return new uk.gov.justice.listing.events.CasesAddedToHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CasesAddedToHearing that = (uk.gov.justice.listing.events.CasesAddedToHearing) obj;

    return java.util.Objects.equals(this.addCasesToUnAllocatedHearing, that.addCasesToUnAllocatedHearing) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.keyReason, that.keyReason) &&
    java.util.Objects.equals(this.listType, that.listType) &&
    java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId) &&
    java.util.Objects.equals(this.tier, that.tier) &&
    java.util.Objects.equals(this.unAllocatedListedCases, that.unAllocatedListedCases);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(addCasesToUnAllocatedHearing, hearingId, keyReason, listType, seedingHearingId, tier, unAllocatedListedCases);}

  public static class Builder {
    private Boolean addCasesToUnAllocatedHearing;

    private UUID hearingId;

    private String keyReason;

    private String listType;

    private UUID seedingHearingId;

    private String tier;

    private List<ListedCase> unAllocatedListedCases;

    public Builder withAddCasesToUnAllocatedHearing(final Boolean addCasesToUnAllocatedHearing) {
      this.addCasesToUnAllocatedHearing = addCasesToUnAllocatedHearing;
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

    public Builder withSeedingHearingId(final UUID seedingHearingId) {
      this.seedingHearingId = seedingHearingId;
      return this;
    }

    public Builder withTier(final String tier) {
      this.tier = tier;
      return this;
    }

    public Builder withUnAllocatedListedCases(final List<ListedCase> unAllocatedListedCases) {
      this.unAllocatedListedCases = unAllocatedListedCases;
      return this;
    }

    public Builder withValuesFrom(final CasesAddedToHearing casesAddedToHearing) {
      this.addCasesToUnAllocatedHearing = casesAddedToHearing.getAddCasesToUnAllocatedHearing();
      this.hearingId = casesAddedToHearing.getHearingId();
      this.keyReason = casesAddedToHearing.getKeyReason();
      this.listType = casesAddedToHearing.getListType();
      this.seedingHearingId = casesAddedToHearing.getSeedingHearingId();
      this.tier = casesAddedToHearing.getTier();
      this.unAllocatedListedCases = casesAddedToHearing.getUnAllocatedListedCases();
      return this;
    }

    public CasesAddedToHearing build() {
      return new uk.gov.justice.listing.events.CasesAddedToHearing(addCasesToUnAllocatedHearing, hearingId, keyReason, listType, seedingHearingId, tier, unAllocatedListedCases);
    }
  }
}
