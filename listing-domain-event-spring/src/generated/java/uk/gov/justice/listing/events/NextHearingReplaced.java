package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class NextHearingReplaced implements Serializable {
  private static final long serialVersionUID = -2994372247440567972L;

  private final UUID newHearingId;

  private final List<UUID> oldHearingIds;

  private final UUID seedingHearingId;

  @JsonCreator
  public NextHearingReplaced(final UUID newHearingId, final List<UUID> oldHearingIds, final UUID seedingHearingId) {
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

  public static Builder nextHearingReplaced() {
    return new uk.gov.justice.listing.events.NextHearingReplaced.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NextHearingReplaced that = (uk.gov.justice.listing.events.NextHearingReplaced) obj;

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

    public Builder withValuesFrom(final NextHearingReplaced nextHearingReplaced) {
      this.newHearingId = nextHearingReplaced.getNewHearingId();
      this.oldHearingIds = nextHearingReplaced.getOldHearingIds();
      this.seedingHearingId = nextHearingReplaced.getSeedingHearingId();
      return this;
    }

    public NextHearingReplaced build() {
      return new uk.gov.justice.listing.events.NextHearingReplaced(newHearingId, oldHearingIds, seedingHearingId);
    }
  }
}
