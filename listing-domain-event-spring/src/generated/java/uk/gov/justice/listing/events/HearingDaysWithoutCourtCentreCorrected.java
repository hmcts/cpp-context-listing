package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class HearingDaysWithoutCourtCentreCorrected implements Serializable {
  private static final long serialVersionUID = 1689684387406321729L;

  private final List<HearingDay> hearingDays;

  private final UUID id;

  @JsonCreator
  public HearingDaysWithoutCourtCentreCorrected(final List<HearingDay> hearingDays, final UUID id) {
    this.hearingDays = hearingDays;
    this.id = id;
  }

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public UUID getId() {
    return id;
  }

  public static Builder hearingDaysWithoutCourtCentreCorrected() {
    return new uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected that = (uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected) obj;

    return java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingDays, id);}

  public static class Builder {
    private List<HearingDay> hearingDays;

    private UUID id;

    public Builder withHearingDays(final List<HearingDay> hearingDays) {
      this.hearingDays = hearingDays;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withValuesFrom(final HearingDaysWithoutCourtCentreCorrected hearingDaysWithoutCourtCentreCorrected) {
      this.hearingDays = hearingDaysWithoutCourtCentreCorrected.getHearingDays();
      this.id = hearingDaysWithoutCourtCentreCorrected.getId();
      return this;
    }

    public HearingDaysWithoutCourtCentreCorrected build() {
      return new uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected(hearingDays, id);
    }
  }
}
