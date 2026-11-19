package uk.gov.justice.progression.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;

public class CaseLinked implements Serializable {
  private static final long serialVersionUID = 3020055580338048662L;

  private final List<Cases> cases;

  private final LinkActionType linkActionType;

  @JsonCreator
  public CaseLinked(final List<Cases> cases, final LinkActionType linkActionType) {
    this.cases = cases;
    this.linkActionType = linkActionType;
  }

  public List<Cases> getCases() {
    return cases;
  }

  public LinkActionType getLinkActionType() {
    return linkActionType;
  }

  public static Builder caseLinked() {
    return new uk.gov.justice.progression.courts.CaseLinked.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.progression.courts.CaseLinked that = (uk.gov.justice.progression.courts.CaseLinked) obj;

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

    public Builder withValuesFrom(final CaseLinked caseLinked) {
      this.cases = caseLinked.getCases();
      this.linkActionType = caseLinked.getLinkActionType();
      return this;
    }

    public CaseLinked build() {
      return new uk.gov.justice.progression.courts.CaseLinked(cases, linkActionType);
    }
  }
}
