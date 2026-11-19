package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class HearingUnallocatedForListing implements Serializable {
  private static final long serialVersionUID = -4721635558148597533L;

  private final UUID courtCentreId;

  private final UUID hearingId;

  private final Boolean seededHearing;

  private final String source;

  @JsonCreator
  public HearingUnallocatedForListing(final UUID courtCentreId, final UUID hearingId, final Boolean seededHearing, final String source) {
    this.courtCentreId = courtCentreId;
    this.hearingId = hearingId;
    this.seededHearing = seededHearing;
    this.source = source;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public Boolean getSeededHearing() {
    return seededHearing;
  }

  public String getSource() {
    return source;
  }

  public static Builder hearingUnallocatedForListing() {
    return new uk.gov.justice.listing.events.HearingUnallocatedForListing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingUnallocatedForListing that = (uk.gov.justice.listing.events.HearingUnallocatedForListing) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.seededHearing, that.seededHearing) &&
    java.util.Objects.equals(this.source, that.source);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, hearingId, seededHearing, source);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID hearingId;

    private Boolean seededHearing;

    private String source;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withSeededHearing(final Boolean seededHearing) {
      this.seededHearing = seededHearing;
      return this;
    }

    public Builder withSource(final String source) {
      this.source = source;
      return this;
    }

    public Builder withValuesFrom(final HearingUnallocatedForListing hearingUnallocatedForListing) {
      this.courtCentreId = hearingUnallocatedForListing.getCourtCentreId();
      this.hearingId = hearingUnallocatedForListing.getHearingId();
      this.seededHearing = hearingUnallocatedForListing.getSeededHearing();
      this.source = hearingUnallocatedForListing.getSource();
      return this;
    }

    public HearingUnallocatedForListing build() {
      return new uk.gov.justice.listing.events.HearingUnallocatedForListing(courtCentreId, hearingId, seededHearing, source);
    }
  }
}
