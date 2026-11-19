package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.court-application-to-be-updated")
public class CourtApplicationToBeUpdated implements Serializable {
  private static final long serialVersionUID = 717622880957678564L;

  private final CourtApplication courtApplication;

  private final List<UUID> hearingIds;

  @JsonCreator
  public CourtApplicationToBeUpdated(final CourtApplication courtApplication, final List<UUID> hearingIds) {
    this.courtApplication = courtApplication;
    this.hearingIds = hearingIds;
  }

  public CourtApplication getCourtApplication() {
    return courtApplication;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
  }

  public static Builder courtApplicationToBeUpdated() {
    return new uk.gov.justice.listing.events.CourtApplicationToBeUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CourtApplicationToBeUpdated that = (uk.gov.justice.listing.events.CourtApplicationToBeUpdated) obj;

    return java.util.Objects.equals(this.courtApplication, that.courtApplication) &&
    java.util.Objects.equals(this.hearingIds, that.hearingIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtApplication, hearingIds);}

  public static class Builder {
    private CourtApplication courtApplication;

    private List<UUID> hearingIds;

    public Builder withCourtApplication(final CourtApplication courtApplication) {
      this.courtApplication = courtApplication;
      return this;
    }

    public Builder withHearingIds(final List<UUID> hearingIds) {
      this.hearingIds = hearingIds;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationToBeUpdated courtApplicationToBeUpdated) {
      this.courtApplication = courtApplicationToBeUpdated.getCourtApplication();
      this.hearingIds = courtApplicationToBeUpdated.getHearingIds();
      return this;
    }

    public CourtApplicationToBeUpdated build() {
      return new uk.gov.justice.listing.events.CourtApplicationToBeUpdated(courtApplication, hearingIds);
    }
  }
}
