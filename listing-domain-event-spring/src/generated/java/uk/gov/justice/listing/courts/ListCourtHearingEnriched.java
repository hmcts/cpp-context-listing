package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.ListCourtHearing;

public class ListCourtHearingEnriched implements Serializable {
  private static final long serialVersionUID = 7316061296234048358L;

  private final String adjournedFromDate;

  private final List<CourtCentreDetails> courtCentresDetails;

  private final ListCourtHearing listCourtHearing;

  @JsonCreator
  public ListCourtHearingEnriched(final String adjournedFromDate, final List<CourtCentreDetails> courtCentresDetails, final ListCourtHearing listCourtHearing) {
    this.adjournedFromDate = adjournedFromDate;
    this.courtCentresDetails = courtCentresDetails;
    this.listCourtHearing = listCourtHearing;
  }

  public String getAdjournedFromDate() {
    return adjournedFromDate;
  }

  public List<CourtCentreDetails> getCourtCentresDetails() {
    return courtCentresDetails;
  }

  public ListCourtHearing getListCourtHearing() {
    return listCourtHearing;
  }

  public static Builder listCourtHearingEnriched() {
    return new uk.gov.justice.listing.courts.ListCourtHearingEnriched.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ListCourtHearingEnriched that = (uk.gov.justice.listing.courts.ListCourtHearingEnriched) obj;

    return java.util.Objects.equals(this.adjournedFromDate, that.adjournedFromDate) &&
    java.util.Objects.equals(this.courtCentresDetails, that.courtCentresDetails) &&
    java.util.Objects.equals(this.listCourtHearing, that.listCourtHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(adjournedFromDate, courtCentresDetails, listCourtHearing);}

  public static class Builder {
    private String adjournedFromDate;

    private List<CourtCentreDetails> courtCentresDetails;

    private ListCourtHearing listCourtHearing;

    public Builder withAdjournedFromDate(final String adjournedFromDate) {
      this.adjournedFromDate = adjournedFromDate;
      return this;
    }

    public Builder withCourtCentresDetails(final List<CourtCentreDetails> courtCentresDetails) {
      this.courtCentresDetails = courtCentresDetails;
      return this;
    }

    public Builder withListCourtHearing(final ListCourtHearing listCourtHearing) {
      this.listCourtHearing = listCourtHearing;
      return this;
    }

    public Builder withValuesFrom(final ListCourtHearingEnriched listCourtHearingEnriched) {
      this.adjournedFromDate = listCourtHearingEnriched.getAdjournedFromDate();
      this.courtCentresDetails = listCourtHearingEnriched.getCourtCentresDetails();
      this.listCourtHearing = listCourtHearingEnriched.getListCourtHearing();
      return this;
    }

    public ListCourtHearingEnriched build() {
      return new uk.gov.justice.listing.courts.ListCourtHearingEnriched(adjournedFromDate, courtCentresDetails, listCourtHearing);
    }
  }
}
