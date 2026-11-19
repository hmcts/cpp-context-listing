package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class CaseDetails implements Serializable {
  private static final long serialVersionUID = -4659675027108169311L;

  private final UUID caseId;

  private final String caseURN;

  private final List<DefendantDetails> defendantDetails;

  @JsonCreator
  public CaseDetails(final UUID caseId, final String caseURN, final List<DefendantDetails> defendantDetails) {
    this.caseId = caseId;
    this.caseURN = caseURN;
    this.defendantDetails = defendantDetails;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public String getCaseURN() {
    return caseURN;
  }

  public List<DefendantDetails> getDefendantDetails() {
    return defendantDetails;
  }

  public static Builder caseDetails() {
    return new uk.gov.justice.listing.courts.CaseDetails.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.CaseDetails that = (uk.gov.justice.listing.courts.CaseDetails) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.caseURN, that.caseURN) &&
    java.util.Objects.equals(this.defendantDetails, that.defendantDetails);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, caseURN, defendantDetails);}

  public static class Builder {
    private UUID caseId;

    private String caseURN;

    private List<DefendantDetails> defendantDetails;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withCaseURN(final String caseURN) {
      this.caseURN = caseURN;
      return this;
    }

    public Builder withDefendantDetails(final List<DefendantDetails> defendantDetails) {
      this.defendantDetails = defendantDetails;
      return this;
    }

    public Builder withValuesFrom(final CaseDetails caseDetails) {
      this.caseId = caseDetails.getCaseId();
      this.caseURN = caseDetails.getCaseURN();
      this.defendantDetails = caseDetails.getDefendantDetails();
      return this;
    }

    public CaseDetails build() {
      return new uk.gov.justice.listing.courts.CaseDetails(caseId, caseURN, defendantDetails);
    }
  }
}
