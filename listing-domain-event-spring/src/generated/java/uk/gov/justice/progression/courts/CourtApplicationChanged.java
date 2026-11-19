package uk.gov.justice.progression.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import uk.gov.justice.core.courts.CourtApplication;

public class CourtApplicationChanged implements Serializable {
  private static final long serialVersionUID = 3566189065287742994L;

  private final CourtApplication courtApplication;

  @JsonCreator
  public CourtApplicationChanged(final CourtApplication courtApplication) {
    this.courtApplication = courtApplication;
  }

  public CourtApplication getCourtApplication() {
    return courtApplication;
  }

  public static Builder courtApplicationChanged() {
    return new uk.gov.justice.progression.courts.CourtApplicationChanged.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.progression.courts.CourtApplicationChanged that = (uk.gov.justice.progression.courts.CourtApplicationChanged) obj;

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

    public Builder withValuesFrom(final CourtApplicationChanged courtApplicationChanged) {
      this.courtApplication = courtApplicationChanged.getCourtApplication();
      return this;
    }

    public CourtApplicationChanged build() {
      return new uk.gov.justice.progression.courts.CourtApplicationChanged(courtApplication);
    }
  }
}
