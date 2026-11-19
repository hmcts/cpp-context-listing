package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;

public class UpdateLinkedCases implements Serializable {
  private static final long serialVersionUID = -7705421701803235390L;

  private final List<Cases> cases;

  private final LinkActionType linkActionType;

  @JsonCreator
  public UpdateLinkedCases(final List<Cases> cases, final LinkActionType linkActionType) {
    this.cases = cases;
    this.linkActionType = linkActionType;
  }

  public List<Cases> getCases() {
    return cases;
  }

  public LinkActionType getLinkActionType() {
    return linkActionType;
  }

  public static Builder updateLinkedCases() {
    return new uk.gov.justice.listing.courts.UpdateLinkedCases.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateLinkedCases that = (uk.gov.justice.listing.courts.UpdateLinkedCases) obj;

    return java.util.Objects.equals(this.cases, that.cases) &&
    java.util.Objects.equals(this.linkActionType, that.linkActionType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(cases, linkActionType);}

  public static class Builder {
    private List<Cases> cases;

    private LinkActionType linkActionType;

    public Builder withCases(final List<Cases> cases) {
      this.cases = cases;
      return this;
    }

    public Builder withLinkActionType(final LinkActionType linkActionType) {
      this.linkActionType = linkActionType;
      return this;
    }

    public Builder withValuesFrom(final UpdateLinkedCases updateLinkedCases) {
      this.cases = updateLinkedCases.getCases();
      this.linkActionType = updateLinkedCases.getLinkActionType();
      return this;
    }

    public UpdateLinkedCases build() {
      return new uk.gov.justice.listing.courts.UpdateLinkedCases(cases, linkActionType);
    }
  }
}
