package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;

public class MigrateCrownHearingsToCourtschedules implements Serializable {
  private static final long serialVersionUID = 7649634831748182249L;

  private final List<MigrateCrownHearing> hearings;

  @JsonCreator
  public MigrateCrownHearingsToCourtschedules(final List<MigrateCrownHearing> hearings) {
    this.hearings = hearings;
  }

  public List<MigrateCrownHearing> getHearings() {
    return hearings;
  }

  public static Builder migrateCrownHearingsToCourtschedules() {
    return new uk.gov.justice.listing.courts.MigrateCrownHearingsToCourtschedules.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.MigrateCrownHearingsToCourtschedules that = (uk.gov.justice.listing.courts.MigrateCrownHearingsToCourtschedules) obj;

    return java.util.Objects.equals(this.hearings, that.hearings);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearings);}

  public static class Builder {
    private List<MigrateCrownHearing> hearings;

    public Builder withHearings(final List<MigrateCrownHearing> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withValuesFrom(final MigrateCrownHearingsToCourtschedules migrateCrownHearingsToCourtschedules) {
      this.hearings = migrateCrownHearingsToCourtschedules.getHearings();
      return this;
    }

    public MigrateCrownHearingsToCourtschedules build() {
      return new uk.gov.justice.listing.courts.MigrateCrownHearingsToCourtschedules(hearings);
    }
  }
}
