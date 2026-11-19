package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.PtphDetail;

public class ListUnscheduledNextHearing implements Serializable {
  private static final long serialVersionUID = -6968812807367547307L;

  private final List<CourtCentreDetails> courtCentresDetails;

  private final HearingUnscheduledListingNeeds hearing;

  private final PtphDetail ptphDetail;

  @JsonCreator
  public ListUnscheduledNextHearing(final List<CourtCentreDetails> courtCentresDetails, final HearingUnscheduledListingNeeds hearing, final PtphDetail ptphDetail) {
    this.courtCentresDetails = courtCentresDetails;
    this.hearing = hearing;
    this.ptphDetail = ptphDetail;
  }

  public List<CourtCentreDetails> getCourtCentresDetails() {
    return courtCentresDetails;
  }

  public HearingUnscheduledListingNeeds getHearing() {
    return hearing;
  }

  public PtphDetail getPtphDetail() {
    return ptphDetail;
  }

  public static Builder listUnscheduledNextHearing() {
    return new uk.gov.justice.listing.courts.ListUnscheduledNextHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ListUnscheduledNextHearing that = (uk.gov.justice.listing.courts.ListUnscheduledNextHearing) obj;

    return java.util.Objects.equals(this.courtCentresDetails, that.courtCentresDetails) &&
    java.util.Objects.equals(this.hearing, that.hearing) &&
    java.util.Objects.equals(this.ptphDetail, that.ptphDetail);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentresDetails, hearing, ptphDetail);}

  public static class Builder {
    private List<CourtCentreDetails> courtCentresDetails;

    private HearingUnscheduledListingNeeds hearing;

    private PtphDetail ptphDetail;

    public Builder withCourtCentresDetails(final List<CourtCentreDetails> courtCentresDetails) {
      this.courtCentresDetails = courtCentresDetails;
      return this;
    }

    public Builder withHearing(final HearingUnscheduledListingNeeds hearing) {
      this.hearing = hearing;
      return this;
    }

    public Builder withPtphDetail(final PtphDetail ptphDetail) {
      this.ptphDetail = ptphDetail;
      return this;
    }

    public Builder withValuesFrom(final ListUnscheduledNextHearing listUnscheduledNextHearing) {
      this.courtCentresDetails = listUnscheduledNextHearing.getCourtCentresDetails();
      this.hearing = listUnscheduledNextHearing.getHearing();
      this.ptphDetail = listUnscheduledNextHearing.getPtphDetail();
      return this;
    }

    public ListUnscheduledNextHearing build() {
      return new uk.gov.justice.listing.courts.ListUnscheduledNextHearing(courtCentresDetails, hearing, ptphDetail);
    }
  }
}
