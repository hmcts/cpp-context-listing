package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.court-application-added-for-hearing")
public class CourtApplicationAddedForHearing implements Serializable {
  private static final long serialVersionUID = 4430348461943395602L;

  private final CourtApplication courtApplication;

  private final UUID hearingId;

  @JsonCreator
  public CourtApplicationAddedForHearing(final CourtApplication courtApplication, final UUID hearingId) {
    this.courtApplication = courtApplication;
    this.hearingId = hearingId;
  }

  public CourtApplication getCourtApplication() {
    return courtApplication;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder courtApplicationAddedForHearing() {
    return new uk.gov.justice.listing.events.CourtApplicationAddedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CourtApplicationAddedForHearing that = (uk.gov.justice.listing.events.CourtApplicationAddedForHearing) obj;

    return java.util.Objects.equals(this.courtApplication, that.courtApplication) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtApplication, hearingId);}

  public static class Builder {
    private CourtApplication courtApplication;

    private UUID hearingId;

    public Builder withCourtApplication(final CourtApplication courtApplication) {
      this.courtApplication = courtApplication;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationAddedForHearing courtApplicationAddedForHearing) {
      this.courtApplication = courtApplicationAddedForHearing.getCourtApplication();
      this.hearingId = courtApplicationAddedForHearing.getHearingId();
      return this;
    }

    public CourtApplicationAddedForHearing build() {
      return new uk.gov.justice.listing.events.CourtApplicationAddedForHearing(courtApplication, hearingId);
    }
  }
}
