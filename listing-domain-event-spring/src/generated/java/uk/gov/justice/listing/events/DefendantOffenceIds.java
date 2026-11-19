package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class DefendantOffenceIds implements Serializable {
  private static final long serialVersionUID = 7964617314504523615L;

  private final UUID id;

  private final List<UUID> offenceIds;

  @JsonCreator
  public DefendantOffenceIds(final UUID id, final List<UUID> offenceIds) {
    this.id = id;
    this.offenceIds = offenceIds;
  }

  public UUID getId() {
    return id;
  }

  public List<UUID> getOffenceIds() {
    return offenceIds;
  }

  public static Builder defendantOffenceIds() {
    return new uk.gov.justice.listing.events.DefendantOffenceIds.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.DefendantOffenceIds that = (uk.gov.justice.listing.events.DefendantOffenceIds) obj;

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.offenceIds, that.offenceIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, offenceIds);}

  public static class Builder {
    private UUID id;

    private List<UUID> offenceIds;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withOffenceIds(final List<UUID> offenceIds) {
      this.offenceIds = offenceIds;
      return this;
    }

    public Builder withValuesFrom(final DefendantOffenceIds defendantOffenceIds) {
      this.id = defendantOffenceIds.getId();
      this.offenceIds = defendantOffenceIds.getOffenceIds();
      return this;
    }

    public DefendantOffenceIds build() {
      return new uk.gov.justice.listing.events.DefendantOffenceIds(id, offenceIds);
    }
  }
}
