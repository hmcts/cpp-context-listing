package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;

public class UpdateHearingsForListingEnriched implements Serializable {
  private static final long serialVersionUID = 2712783408397862382L;

  private final List<UpdateHearingsForListing> updateHearingsForListing;

  @JsonCreator
  public UpdateHearingsForListingEnriched(final List<UpdateHearingsForListing> updateHearingsForListing) {
    this.updateHearingsForListing = updateHearingsForListing;
  }

  public List<UpdateHearingsForListing> getUpdateHearingsForListing() {
    return updateHearingsForListing;
  }

  public static Builder updateHearingsForListingEnriched() {
    return new uk.gov.justice.listing.courts.UpdateHearingsForListingEnriched.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateHearingsForListingEnriched that = (uk.gov.justice.listing.courts.UpdateHearingsForListingEnriched) obj;

    return java.util.Objects.equals(this.updateHearingsForListing, that.updateHearingsForListing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(updateHearingsForListing);}

  public static class Builder {
    private List<UpdateHearingsForListing> updateHearingsForListing;

    public Builder withUpdateHearingsForListing(final List<UpdateHearingsForListing> updateHearingsForListing) {
      this.updateHearingsForListing = updateHearingsForListing;
      return this;
    }

    public Builder withValuesFrom(final UpdateHearingsForListingEnriched updateHearingsForListingEnriched) {
      this.updateHearingsForListing = updateHearingsForListingEnriched.getUpdateHearingsForListing();
      return this;
    }

    public UpdateHearingsForListingEnriched build() {
      return new uk.gov.justice.listing.courts.UpdateHearingsForListingEnriched(updateHearingsForListing);
    }
  }
}
