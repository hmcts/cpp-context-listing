package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class CourtApplicationAddedToHearing implements Serializable {
  private static final long serialVersionUID = 8286659549049166263L;

  private final UUID applicationId;

  private final UUID hearingId;

  @JsonCreator
  public CourtApplicationAddedToHearing(final UUID applicationId, final UUID hearingId) {
    this.applicationId = applicationId;
    this.hearingId = hearingId;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder courtApplicationAddedToHearing() {
    return new uk.gov.justice.listing.events.CourtApplicationAddedToHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CourtApplicationAddedToHearing that = (uk.gov.justice.listing.events.CourtApplicationAddedToHearing) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, hearingId);}

  public static class Builder {
    private UUID applicationId;

    private UUID hearingId;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationAddedToHearing courtApplicationAddedToHearing) {
      this.applicationId = courtApplicationAddedToHearing.getApplicationId();
      this.hearingId = courtApplicationAddedToHearing.getHearingId();
      return this;
    }

    public CourtApplicationAddedToHearing build() {
      return new uk.gov.justice.listing.events.CourtApplicationAddedToHearing(applicationId, hearingId);
    }
  }
}
