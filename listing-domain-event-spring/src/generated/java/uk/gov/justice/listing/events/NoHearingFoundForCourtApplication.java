package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;

public class NoHearingFoundForCourtApplication implements Serializable {
  private static final long serialVersionUID = -7170759488945484008L;

  private final CourtApplication courtApplication;

  @JsonCreator
  public NoHearingFoundForCourtApplication(final CourtApplication courtApplication) {
    this.courtApplication = courtApplication;
  }

  public CourtApplication getCourtApplication() {
    return courtApplication;
  }

  public static Builder noHearingFoundForCourtApplication() {
    return new uk.gov.justice.listing.events.NoHearingFoundForCourtApplication.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NoHearingFoundForCourtApplication that = (uk.gov.justice.listing.events.NoHearingFoundForCourtApplication) obj;

    return java.util.Objects.equals(this.courtApplication, that.courtApplication);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtApplication);}

  public static class Builder {
    private CourtApplication courtApplication;

    public Builder withCourtApplication(final CourtApplication courtApplication) {
      this.courtApplication = courtApplication;
      return this;
    }

    public Builder withValuesFrom(final NoHearingFoundForCourtApplication noHearingFoundForCourtApplication) {
      this.courtApplication = noHearingFoundForCourtApplication.getCourtApplication();
      return this;
    }

    public NoHearingFoundForCourtApplication build() {
      return new uk.gov.justice.listing.events.NoHearingFoundForCourtApplication(courtApplication);
    }
  }
}
