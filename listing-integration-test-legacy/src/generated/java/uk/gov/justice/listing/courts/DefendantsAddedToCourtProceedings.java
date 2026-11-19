package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ListHearingRequest;

public class DefendantsAddedToCourtProceedings implements Serializable {
  private static final long serialVersionUID = -2013694293470485741L;

  private final List<Defendant> defendants;

  private final List<ListHearingRequest> listHearingRequests;

  @JsonCreator
  public DefendantsAddedToCourtProceedings(final List<Defendant> defendants, final List<ListHearingRequest> listHearingRequests) {
    this.defendants = defendants;
    this.listHearingRequests = listHearingRequests;
  }

  public List<Defendant> getDefendants() {
    return defendants;
  }

  public List<ListHearingRequest> getListHearingRequests() {
    return listHearingRequests;
  }

  public static Builder defendantsAddedToCourtProceedings() {
    return new uk.gov.justice.listing.courts.DefendantsAddedToCourtProceedings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.DefendantsAddedToCourtProceedings that = (uk.gov.justice.listing.courts.DefendantsAddedToCourtProceedings) obj;

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

    public Builder withValuesFrom(final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings) {
      this.defendants = defendantsAddedToCourtProceedings.getDefendants();
      this.listHearingRequests = defendantsAddedToCourtProceedings.getListHearingRequests();
      return this;
    }

    public DefendantsAddedToCourtProceedings build() {
      return new uk.gov.justice.listing.courts.DefendantsAddedToCourtProceedings(defendants, listHearingRequests);
    }
  }
}
