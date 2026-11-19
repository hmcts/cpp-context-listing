package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class OffencesMovedToNextHearing implements Serializable {
  private static final long serialVersionUID = 7153012286367548029L;

  private final UUID newHearingId;

  private final List<UUID> oldHearingIds;

  private final UUID seedingHearingId;

  @JsonCreator
  public OffencesMovedToNextHearing(final UUID newHearingId, final List<UUID> oldHearingIds, final UUID seedingHearingId) {
    this.newHearingId = newHearingId;
    this.oldHearingIds = oldHearingIds;
    this.seedingHearingId = seedingHearingId;
  }

  public UUID getNewHearingId() {
    return newHearingId;
  }

  public List<UUID> getOldHearingIds() {
    return oldHearingIds;
  }

  public UUID getSeedingHearingId() {
    return seedingHearingId;
  }

  public static Builder offencesMovedToNextHearing() {
    return new uk.gov.justice.listing.events.OffencesMovedToNextHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.OffencesMovedToNextHearing that = (uk.gov.justice.listing.events.OffencesMovedToNextHearing) obj;

    return java.util.Objects.equals(this.newHearingId, that.newHearingId) &&
    java.util.Objects.equals(this.oldHearingIds, that.oldHearingIds) &&
    java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(newHearingId, oldHearingIds, seedingHearingId);}

  public static class Builder {
    private UUID newHearingId;

    private List<UUID> oldHearingIds;

    private UUID seedingHearingId;

    public Builder withNewHearingId(final UUID newHearingId) {
      this.newHearingId = newHearingId;
      return this;
    }

    public Builder withOldHearingIds(final List<UUID> oldHearingIds) {
      this.oldHearingIds = oldHearingIds;
      return this;
    }

    public Builder withSeedingHearingId(final UUID seedingHearingId) {
      this.seedingHearingId = seedingHearingId;
      return this;
    }

    public Builder withValuesFrom(final OffencesMovedToNextHearing offencesMovedToNextHearing) {
      this.newHearingId = offencesMovedToNextHearing.getNewHearingId();
      this.oldHearingIds = offencesMovedToNextHearing.getOldHearingIds();
      this.seedingHearingId = offencesMovedToNextHearing.getSeedingHearingId();
      return this;
    }

    public OffencesMovedToNextHearing build() {
      return new uk.gov.justice.listing.events.OffencesMovedToNextHearing(newHearingId, oldHearingIds, seedingHearingId);
    }
  }
}
