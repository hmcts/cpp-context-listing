package uk.gov.justice.listing.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class ListingNumberUpdated implements Serializable {
  private static final long serialVersionUID = 8497437155617071870L;

  private final UUID hearingId;

  private final List<UUID> offenceIds;

  @JsonCreator
  public ListingNumberUpdated(final UUID hearingId, final List<UUID> offenceIds) {
    this.hearingId = hearingId;
    this.offenceIds = offenceIds;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<UUID> getOffenceIds() {
    return offenceIds;
  }

  public static Builder listingNumberUpdated() {
    return new uk.gov.justice.listing.event.ListingNumberUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.event.ListingNumberUpdated that = (uk.gov.justice.listing.event.ListingNumberUpdated) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.offenceIds, that.offenceIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, offenceIds);}

  public static class Builder {
    private UUID hearingId;

    private List<UUID> offenceIds;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withOffenceIds(final List<UUID> offenceIds) {
      this.offenceIds = offenceIds;
      return this;
    }

    public Builder withValuesFrom(final ListingNumberUpdated listingNumberUpdated) {
      this.hearingId = listingNumberUpdated.getHearingId();
      this.offenceIds = listingNumberUpdated.getOffenceIds();
      return this;
    }

    public ListingNumberUpdated build() {
      return new uk.gov.justice.listing.event.ListingNumberUpdated(hearingId, offenceIds);
    }
  }
}
