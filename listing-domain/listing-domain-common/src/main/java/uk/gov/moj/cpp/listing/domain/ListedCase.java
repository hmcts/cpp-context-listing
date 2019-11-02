package uk.gov.moj.cpp.listing.domain;

import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class ListedCase {
  private final CaseIdentifier caseIdentifier;

  private final List<Defendant> defendants;

  private final List<CaseMarker> caseMarkers;

  private final UUID id;

  public ListedCase(final CaseIdentifier caseIdentifier, final List<Defendant> defendants, final List<CaseMarker> caseMarkers, final UUID id) {
    this.caseIdentifier = caseIdentifier;
    this.defendants = defendants;
    this.caseMarkers = caseMarkers;
    this.id = id;
  }

  public CaseIdentifier getCaseIdentifier() {
    return caseIdentifier;
  }

  public List<Defendant> getDefendants() {
    return defendants;
  }

  public List<CaseMarker> getCaseMarkers() {
    return caseMarkers;
  }

  public UUID getId() {
    return id;
  }


  public static Builder listedCase() {
    return new ListedCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final ListedCase that = (ListedCase) obj;

    return java.util.Objects.equals(this.caseIdentifier, that.caseIdentifier) &&
    java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.id, that.id) ;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseIdentifier, defendants, id);}

  @Override
  public String toString() {
    return "ListedCase{" +
    	"caseIdentifier='" + caseIdentifier + "'," +
    	"defendants='" + defendants + "'," +
    	"id='" + id + "'" +
    "}";
  }

  public static class Builder {
    private CaseIdentifier caseIdentifier;

    private List<Defendant> defendants;

    private UUID id;
    private List<CaseMarker> caseMarkers;



    public Builder withCaseIdentifier(final CaseIdentifier caseIdentifier) {
      this.caseIdentifier = caseIdentifier;
      return this;
    }

    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withCaseMarkers(final List<CaseMarker> caseMarkers) {
      this.caseMarkers = caseMarkers;
      return this;
    }


    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }



    public ListedCase build() {
      return new ListedCase(caseIdentifier, defendants, caseMarkers, id);
    }
  }
}
