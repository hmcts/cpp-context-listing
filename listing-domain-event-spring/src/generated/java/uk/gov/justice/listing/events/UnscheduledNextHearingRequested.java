package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.unscheduled-next-hearing-requested")
public class UnscheduledNextHearingRequested implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final List<CourtCentreDetails> courtCentreDetails;

  private final HearingUnscheduledListingNeeds hearing;

  private final String hearingDay;

  private final PtphDetail ptphDetail;

  @JsonCreator
  public UnscheduledNextHearingRequested(final List<CourtCentreDetails> courtCentreDetails, final HearingUnscheduledListingNeeds hearing, final String hearingDay, final PtphDetail ptphDetail) {
    this.courtCentreDetails = courtCentreDetails;
    this.hearing = hearing;
    this.hearingDay = hearingDay;
    this.ptphDetail = ptphDetail;
  }

  public List<CourtCentreDetails> getCourtCentreDetails() {
    return courtCentreDetails;
  }

  public HearingUnscheduledListingNeeds getHearing() {
    return hearing;
  }

  public String getHearingDay() {
    return hearingDay;
  }

  public PtphDetail getPtphDetail() {
    return ptphDetail;
  }

  public static Builder unscheduledNextHearingRequested() {
    return new uk.gov.justice.listing.events.UnscheduledNextHearingRequested.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.UnscheduledNextHearingRequested that = (uk.gov.justice.listing.events.UnscheduledNextHearingRequested) obj;

    return java.util.Objects.equals(this.courtCentreDetails, that.courtCentreDetails) &&
    java.util.Objects.equals(this.hearing, that.hearing) &&
    java.util.Objects.equals(this.hearingDay, that.hearingDay) &&
    java.util.Objects.equals(this.ptphDetail, that.ptphDetail);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreDetails, hearing, hearingDay, ptphDetail);}

  public static class Builder {
    private List<CourtCentreDetails> courtCentreDetails;

    private HearingUnscheduledListingNeeds hearing;

    private String hearingDay;

    private PtphDetail ptphDetail;

    public Builder withCourtCentreDetails(final List<CourtCentreDetails> courtCentreDetails) {
      this.courtCentreDetails = courtCentreDetails;
      return this;
    }

    public Builder withHearing(final HearingUnscheduledListingNeeds hearing) {
      this.hearing = hearing;
      return this;
    }

    public Builder withHearingDay(final String hearingDay) {
      this.hearingDay = hearingDay;
      return this;
    }

    public Builder withPtphDetail(final PtphDetail ptphDetail) {
      this.ptphDetail = ptphDetail;
      return this;
    }

    public Builder withValuesFrom(final UnscheduledNextHearingRequested unscheduledNextHearingRequested) {
      this.courtCentreDetails = unscheduledNextHearingRequested.getCourtCentreDetails();
      this.hearing = unscheduledNextHearingRequested.getHearing();
      this.hearingDay = unscheduledNextHearingRequested.getHearingDay();
      this.ptphDetail = unscheduledNextHearingRequested.getPtphDetail();
      return this;
    }

    public UnscheduledNextHearingRequested build() {
      return new uk.gov.justice.listing.events.UnscheduledNextHearingRequested(courtCentreDetails, hearing, hearingDay, ptphDetail);
    }
  }
}
