package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ListHearingRequest;

public class AddDefendantsToCourtProceedings implements Serializable {
  private static final long serialVersionUID = 1923407196461966438L;

  private final List<Defendant> defendants;

  private final List<ListHearingRequest> listHearingRequests;

  @JsonCreator
  public AddDefendantsToCourtProceedings(final List<Defendant> defendants, final List<ListHearingRequest> listHearingRequests) {
    this.defendants = defendants;
    this.listHearingRequests = listHearingRequests;
  }

  public List<Defendant> getDefendants() {
    return defendants;
  }

  public List<ListHearingRequest> getListHearingRequests() {
    return listHearingRequests;
  }

  public static Builder addDefendantsToCourtProceedings() {
    return new uk.gov.justice.listing.courts.AddDefendantsToCourtProceedings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.AddDefendantsToCourtProceedings that = (uk.gov.justice.listing.courts.AddDefendantsToCourtProceedings) obj;

    return java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.listHearingRequests, that.listHearingRequests);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendants, listHearingRequests);}

  public static class Builder {
    private List<Defendant> defendants;

    private List<ListHearingRequest> listHearingRequests;

    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withListHearingRequests(final List<ListHearingRequest> listHearingRequests) {
      this.listHearingRequests = listHearingRequests;
      return this;
    }

    public Builder withValuesFrom(final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings) {
      this.defendants = addDefendantsToCourtProceedings.getDefendants();
      this.listHearingRequests = addDefendantsToCourtProceedings.getListHearingRequests();
      return this;
    }

    public AddDefendantsToCourtProceedings build() {
      return new uk.gov.justice.listing.courts.AddDefendantsToCourtProceedings(defendants, listHearingRequests);
    }
  }
}
