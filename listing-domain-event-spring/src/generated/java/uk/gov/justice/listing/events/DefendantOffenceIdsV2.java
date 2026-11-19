package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class DefendantOffenceIdsV2 implements Serializable {
  private static final long serialVersionUID = -1565312484075012412L;

  private final UUID id;

  private final List<OffenceIds> offenceIds;

  @JsonCreator
  public DefendantOffenceIdsV2(final UUID id, final List<OffenceIds> offenceIds) {
    this.id = id;
    this.offenceIds = offenceIds;
  }

  public UUID getId() {
    return id;
  }

  public List<OffenceIds> getOffenceIds() {
    return offenceIds;
  }

  public static Builder defendantOffenceIdsV2() {
    return new uk.gov.justice.listing.events.DefendantOffenceIdsV2.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.DefendantOffenceIdsV2 that = (uk.gov.justice.listing.events.DefendantOffenceIdsV2) obj;

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.offenceIds, that.offenceIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, offenceIds);}

  public static class Builder {
    private UUID id;

    private List<OffenceIds> offenceIds;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withOffenceIds(final List<OffenceIds> offenceIds) {
      this.offenceIds = offenceIds;
      return this;
    }

    public Builder withValuesFrom(final DefendantOffenceIdsV2 defendantOffenceIdsV2) {
      this.id = defendantOffenceIdsV2.getId();
      this.offenceIds = defendantOffenceIdsV2.getOffenceIds();
      return this;
    }

    public DefendantOffenceIdsV2 build() {
      return new uk.gov.justice.listing.events.DefendantOffenceIdsV2(id, offenceIds);
    }
  }
}
