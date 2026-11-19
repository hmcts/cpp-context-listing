package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class CourtApplicationCase implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String caseStatus;

  private final Boolean isSJP;

  private final List<Offence> offences;

  private final UUID prosecutionCaseId;

  private final ProsecutionCaseIdentifier prosecutionCaseIdentifier;

  @JsonCreator
  public CourtApplicationCase(final String caseStatus, final Boolean isSJP, final List<Offence> offences, final UUID prosecutionCaseId, final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
    this.caseStatus = caseStatus;
    this.isSJP = isSJP;
    this.offences = offences;
    this.prosecutionCaseId = prosecutionCaseId;
    this.prosecutionCaseIdentifier = prosecutionCaseIdentifier;
  }

  public String getCaseStatus() {
    return caseStatus;
  }

  public Boolean getIsSJP() {
    return isSJP;
  }

  public List<Offence> getOffences() {
    return offences;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public ProsecutionCaseIdentifier getProsecutionCaseIdentifier() {
    return prosecutionCaseIdentifier;
  }

  public static Builder courtApplicationCase() {
    return new uk.gov.justice.core.courts.CourtApplicationCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtApplicationCase that = (uk.gov.justice.core.courts.CourtApplicationCase) obj;

    return java.util.Objects.equals(this.caseStatus, that.caseStatus) &&
    java.util.Objects.equals(this.isSJP, that.isSJP) &&
    java.util.Objects.equals(this.offences, that.offences) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId) &&
    java.util.Objects.equals(this.prosecutionCaseIdentifier, that.prosecutionCaseIdentifier);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseStatus, isSJP, offences, prosecutionCaseId, prosecutionCaseIdentifier);}

  public static class Builder {
    private String caseStatus;

    private Boolean isSJP;

    private List<Offence> offences;

    private UUID prosecutionCaseId;

    private ProsecutionCaseIdentifier prosecutionCaseIdentifier;

    public Builder withCaseStatus(final String caseStatus) {
      this.caseStatus = caseStatus;
      return this;
    }

    public Builder withIsSJP(final Boolean isSJP) {
      this.isSJP = isSJP;
      return this;
    }

    public Builder withOffences(final List<Offence> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withProsecutionCaseIdentifier(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
      this.prosecutionCaseIdentifier = prosecutionCaseIdentifier;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationCase courtApplicationCase) {
      this.caseStatus = courtApplicationCase.getCaseStatus();
      this.isSJP = courtApplicationCase.getIsSJP();
      this.offences = courtApplicationCase.getOffences();
      this.prosecutionCaseId = courtApplicationCase.getProsecutionCaseId();
      this.prosecutionCaseIdentifier = courtApplicationCase.getProsecutionCaseIdentifier();
      return this;
    }

    public CourtApplicationCase build() {
      return new uk.gov.justice.core.courts.CourtApplicationCase(caseStatus, isSJP, offences, prosecutionCaseId, prosecutionCaseIdentifier);
    }
  }
}
