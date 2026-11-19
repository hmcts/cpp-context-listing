package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class OffencesRemovedFromHearing implements Serializable {
  private static final long serialVersionUID = 8575946458083994674L;

  private final List<UUID> caseIdsSeededByOnlySeedingHearingId;

  private final UUID hearingId;

  private final List<UUID> seededOffences;

  private final UUID seedingHearingId;

  private final Boolean unallocated;

  @JsonCreator
  public OffencesRemovedFromHearing(final List<UUID> caseIdsSeededByOnlySeedingHearingId, final UUID hearingId, final List<UUID> seededOffences, final UUID seedingHearingId, final Boolean unallocated) {
    this.caseIdsSeededByOnlySeedingHearingId = caseIdsSeededByOnlySeedingHearingId;
    this.hearingId = hearingId;
    this.seededOffences = seededOffences;
    this.seedingHearingId = seedingHearingId;
    this.unallocated = unallocated;
  }

  public List<UUID> getCaseIdsSeededByOnlySeedingHearingId() {
    return caseIdsSeededByOnlySeedingHearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<UUID> getSeededOffences() {
    return seededOffences;
  }

  public UUID getSeedingHearingId() {
    return seedingHearingId;
  }

  public Boolean getUnallocated() {
    return unallocated;
  }

  public static Builder offencesRemovedFromHearing() {
    return new uk.gov.justice.listing.events.OffencesRemovedFromHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.OffencesRemovedFromHearing that = (uk.gov.justice.listing.events.OffencesRemovedFromHearing) obj;

    return java.util.Objects.equals(this.caseIdsSeededByOnlySeedingHearingId, that.caseIdsSeededByOnlySeedingHearingId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.seededOffences, that.seededOffences) &&
    java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId) &&
    java.util.Objects.equals(this.unallocated, that.unallocated);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseIdsSeededByOnlySeedingHearingId, hearingId, seededOffences, seedingHearingId, unallocated);}

  public static class Builder {
    private List<UUID> caseIdsSeededByOnlySeedingHearingId;

    private UUID hearingId;

    private List<UUID> seededOffences;

    private UUID seedingHearingId;

    private Boolean unallocated;

    public Builder withCaseIdsSeededByOnlySeedingHearingId(final List<UUID> caseIdsSeededByOnlySeedingHearingId) {
      this.caseIdsSeededByOnlySeedingHearingId = caseIdsSeededByOnlySeedingHearingId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withSeededOffences(final List<UUID> seededOffences) {
      this.seededOffences = seededOffences;
      return this;
    }

    public Builder withSeedingHearingId(final UUID seedingHearingId) {
      this.seedingHearingId = seedingHearingId;
      return this;
    }

    public Builder withUnallocated(final Boolean unallocated) {
      this.unallocated = unallocated;
      return this;
    }

    public Builder withValuesFrom(final OffencesRemovedFromHearing offencesRemovedFromHearing) {
      this.caseIdsSeededByOnlySeedingHearingId = offencesRemovedFromHearing.getCaseIdsSeededByOnlySeedingHearingId();
      this.hearingId = offencesRemovedFromHearing.getHearingId();
      this.seededOffences = offencesRemovedFromHearing.getSeededOffences();
      this.seedingHearingId = offencesRemovedFromHearing.getSeedingHearingId();
      this.unallocated = offencesRemovedFromHearing.getUnallocated();
      return this;
    }

    public OffencesRemovedFromHearing build() {
      return new uk.gov.justice.listing.events.OffencesRemovedFromHearing(caseIdsSeededByOnlySeedingHearingId, hearingId, seededOffences, seedingHearingId, unallocated);
    }
  }
}
