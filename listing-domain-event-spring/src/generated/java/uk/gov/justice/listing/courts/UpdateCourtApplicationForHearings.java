package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class UpdateCourtApplicationForHearings implements Serializable {
  private static final long serialVersionUID = 8906956638457691646L;

  private final CourtApplication courtApplication;

  private final List<UUID> hearingIds;

  @JsonCreator
  public UpdateCourtApplicationForHearings(final CourtApplication courtApplication, final List<UUID> hearingIds) {
    this.courtApplication = courtApplication;
    this.hearingIds = hearingIds;
  }

  public CourtApplication getCourtApplication() {
    return courtApplication;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
  }

  public static Builder updateCourtApplicationForHearings() {
    return new uk.gov.justice.listing.courts.UpdateCourtApplicationForHearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateCourtApplicationForHearings that = (uk.gov.justice.listing.courts.UpdateCourtApplicationForHearings) obj;

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

    public Builder withValuesFrom(final UpdateCourtApplicationForHearings updateCourtApplicationForHearings) {
      this.courtApplication = updateCourtApplicationForHearings.getCourtApplication();
      this.hearingIds = updateCourtApplicationForHearings.getHearingIds();
      return this;
    }

    public UpdateCourtApplicationForHearings build() {
      return new uk.gov.justice.listing.courts.UpdateCourtApplicationForHearings(courtApplication, hearingIds);
    }
  }
}
