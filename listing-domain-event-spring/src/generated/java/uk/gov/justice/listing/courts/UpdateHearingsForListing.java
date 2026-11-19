package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.UpdateHearingForListing;

public class UpdateHearingsForListing implements Serializable {
  private static final long serialVersionUID = 2712783408397862382L;

  private final CourtCentreDetails courtCentreDetails;

  private final List<ProsecutionCases> prosecutionCases;

  private final UpdateHearingForListing updateHearingForListing;

  @JsonCreator
  public UpdateHearingsForListing(final CourtCentreDetails courtCentreDetails, final List<ProsecutionCases> prosecutionCases, final UpdateHearingForListing updateHearingForListing) {
    this.courtCentreDetails = courtCentreDetails;
    this.prosecutionCases = prosecutionCases;
    this.updateHearingForListing = updateHearingForListing;
  }

  public CourtCentreDetails getCourtCentreDetails() {
    return courtCentreDetails;
  }

  public List<ProsecutionCases> getProsecutionCases() {
    return prosecutionCases;
  }

  public UpdateHearingForListing getUpdateHearingForListing() {
    return updateHearingForListing;
  }

  public static Builder updateHearingsForListing() {
    return new uk.gov.justice.listing.courts.UpdateHearingsForListing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateHearingsForListing that = (uk.gov.justice.listing.courts.UpdateHearingsForListing) obj;

    return java.util.Objects.equals(this.courtCentreDetails, that.courtCentreDetails) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.updateHearingForListing, that.updateHearingForListing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreDetails, prosecutionCases, updateHearingForListing);}

  public static class Builder {
    private CourtCentreDetails courtCentreDetails;

    private List<ProsecutionCases> prosecutionCases;

    private UpdateHearingForListing updateHearingForListing;

    public Builder withCourtCentreDetails(final CourtCentreDetails courtCentreDetails) {
      this.courtCentreDetails = courtCentreDetails;
      return this;
    }

    public Builder withProsecutionCases(final List<ProsecutionCases> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
      return this;
    }

    public Builder withUpdateHearingForListing(final UpdateHearingForListing updateHearingForListing) {
      this.updateHearingForListing = updateHearingForListing;
      return this;
    }

    public Builder withValuesFrom(final UpdateHearingsForListing updateHearingsForListing) {
      this.courtCentreDetails = updateHearingsForListing.getCourtCentreDetails();
      this.prosecutionCases = updateHearingsForListing.getProsecutionCases();
      this.updateHearingForListing = updateHearingsForListing.getUpdateHearingForListing();
      return this;
    }

    public UpdateHearingsForListing build() {
      return new uk.gov.justice.listing.courts.UpdateHearingsForListing(courtCentreDetails, prosecutionCases, updateHearingForListing);
    }
  }
}
