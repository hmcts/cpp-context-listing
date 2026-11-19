package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class OffenceIds implements Serializable {
  private static final long serialVersionUID = -1565312484075012412L;

  private final UUID id;

  private final SeedingHearing seedingHearing;

  @JsonCreator
  public OffenceIds(final UUID id, final SeedingHearing seedingHearing) {
    this.id = id;
    this.seedingHearing = seedingHearing;
  }

  public UUID getId() {
    return id;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public static Builder offenceIds() {
    return new uk.gov.justice.listing.events.OffenceIds.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.OffenceIds that = (uk.gov.justice.listing.events.OffenceIds) obj;

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, seedingHearing);}

  public static class Builder {
    private UUID id;

    private SeedingHearing seedingHearing;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withValuesFrom(final OffenceIds offenceIds) {
      this.id = offenceIds.getId();
      this.seedingHearing = offenceIds.getSeedingHearing();
      return this;
    }

    public OffenceIds build() {
      return new uk.gov.justice.listing.events.OffenceIds(id, seedingHearing);
    }
  }
}
