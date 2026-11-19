package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import uk.gov.justice.core.courts.CourtApplication;

public class UpdateCourtApplicationCommand implements Serializable {
  private static final long serialVersionUID = 2186951862418979171L;

  private final CourtApplication courtApplication;

  @JsonCreator
  public UpdateCourtApplicationCommand(final CourtApplication courtApplication) {
    this.courtApplication = courtApplication;
  }

  public CourtApplication getCourtApplication() {
    return courtApplication;
  }

  public static Builder updateCourtApplicationCommand() {
    return new uk.gov.justice.listing.courts.UpdateCourtApplicationCommand.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateCourtApplicationCommand that = (uk.gov.justice.listing.courts.UpdateCourtApplicationCommand) obj;

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

    public Builder withValuesFrom(final UpdateCourtApplicationCommand updateCourtApplicationCommand) {
      this.courtApplication = updateCourtApplicationCommand.getCourtApplication();
      return this;
    }

    public UpdateCourtApplicationCommand build() {
      return new uk.gov.justice.listing.courts.UpdateCourtApplicationCommand(courtApplication);
    }
  }
}
