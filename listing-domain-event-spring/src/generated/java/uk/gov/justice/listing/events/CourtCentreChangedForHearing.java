package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.court-centre-changed-for-hearing")
public class CourtCentreChangedForHearing implements Serializable {
  private static final long serialVersionUID = -8828458893966701804L;

  private final UUID courtCentreId;

  private final UUID hearingId;

  @JsonCreator
  public CourtCentreChangedForHearing(final UUID courtCentreId, final UUID hearingId) {
    this.courtCentreId = courtCentreId;
    this.hearingId = hearingId;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder courtCentreChangedForHearing() {
    return new uk.gov.justice.listing.events.CourtCentreChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CourtCentreChangedForHearing that = (uk.gov.justice.listing.events.CourtCentreChangedForHearing) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, hearingId);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID hearingId;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final CourtCentreChangedForHearing courtCentreChangedForHearing) {
      this.courtCentreId = courtCentreChangedForHearing.getCourtCentreId();
      this.hearingId = courtCentreChangedForHearing.getHearingId();
      return this;
    }

    public CourtCentreChangedForHearing build() {
      return new uk.gov.justice.listing.events.CourtCentreChangedForHearing(courtCentreId, hearingId);
    }
  }
}
