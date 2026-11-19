package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.PoliceOfficerInCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

public class ProsecutionCase implements Serializable {
  private static final long serialVersionUID = -5738629687145407936L;

  private final Boolean appealProceedingsPending;

  private final Boolean breachProceedingsPending;

  private final List<Marker> caseMarkers;

  private final String caseStatus;

  private final String classOfCase;

  private final String cpsOrganisation;

  private final List<Defendant> defendants;

  private final UUID id;

  private final InitiationCode initiationCode;

  private final Boolean isCpsOrgVerifyError;

  private final String originatingOrganisation;

  private final PoliceOfficerInCase policeOfficerInCase;

  private final ProsecutionCaseIdentifier prosecutionCaseIdentifier;

  private final String removalReason;

  private final String statementOfFacts;

  private final String statementOfFactsWelsh;

  @JsonCreator
  public ProsecutionCase(final Boolean appealProceedingsPending, final Boolean breachProceedingsPending, final List<Marker> caseMarkers, final String caseStatus, final String classOfCase, final String cpsOrganisation, final List<Defendant> defendants, final UUID id, final InitiationCode initiationCode, final Boolean isCpsOrgVerifyError, final String originatingOrganisation, final PoliceOfficerInCase policeOfficerInCase, final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final String removalReason, final String statementOfFacts, final String statementOfFactsWelsh) {
    this.appealProceedingsPending = appealProceedingsPending;
    this.breachProceedingsPending = breachProceedingsPending;
    this.caseMarkers = caseMarkers;
    this.caseStatus = caseStatus;
    this.classOfCase = classOfCase;
    this.cpsOrganisation = cpsOrganisation;
    this.defendants = defendants;
    this.id = id;
    this.initiationCode = initiationCode;
    this.isCpsOrgVerifyError = isCpsOrgVerifyError;
    this.originatingOrganisation = originatingOrganisation;
    this.policeOfficerInCase = policeOfficerInCase;
    this.prosecutionCaseIdentifier = prosecutionCaseIdentifier;
    this.removalReason = removalReason;
    this.statementOfFacts = statementOfFacts;
    this.statementOfFactsWelsh = statementOfFactsWelsh;
  }

  public Boolean getAppealProceedingsPending() {
    return appealProceedingsPending;
  }

  public Boolean getBreachProceedingsPending() {
    return breachProceedingsPending;
  }

  public List<Marker> getCaseMarkers() {
    return caseMarkers;
  }

  public String getCaseStatus() {
    return caseStatus;
  }

  public String getClassOfCase() {
    return classOfCase;
  }

  public String getCpsOrganisation() {
    return cpsOrganisation;
  }

  public List<Defendant> getDefendants() {
    return defendants;
  }

  public UUID getId() {
    return id;
  }

  public InitiationCode getInitiationCode() {
    return initiationCode;
  }

  public Boolean getIsCpsOrgVerifyError() {
    return isCpsOrgVerifyError;
  }

  public String getOriginatingOrganisation() {
    return originatingOrganisation;
  }

  public PoliceOfficerInCase getPoliceOfficerInCase() {
    return policeOfficerInCase;
  }

  public ProsecutionCaseIdentifier getProsecutionCaseIdentifier() {
    return prosecutionCaseIdentifier;
  }

  public String getRemovalReason() {
    return removalReason;
  }

  public String getStatementOfFacts() {
    return statementOfFacts;
  }

  public String getStatementOfFactsWelsh() {
    return statementOfFactsWelsh;
  }

  public static Builder prosecutionCase() {
    return new uk.gov.justice.listing.events.ProsecutionCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.ProsecutionCase that = (uk.gov.justice.listing.events.ProsecutionCase) obj;

    return java.util.Objects.equals(this.appealProceedingsPending, that.appealProceedingsPending) &&
    java.util.Objects.equals(this.breachProceedingsPending, that.breachProceedingsPending) &&
    java.util.Objects.equals(this.caseMarkers, that.caseMarkers) &&
    java.util.Objects.equals(this.caseStatus, that.caseStatus) &&
    java.util.Objects.equals(this.classOfCase, that.classOfCase) &&
    java.util.Objects.equals(this.cpsOrganisation, that.cpsOrganisation) &&
    java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.initiationCode, that.initiationCode) &&
    java.util.Objects.equals(this.isCpsOrgVerifyError, that.isCpsOrgVerifyError) &&
    java.util.Objects.equals(this.originatingOrganisation, that.originatingOrganisation) &&
    java.util.Objects.equals(this.policeOfficerInCase, that.policeOfficerInCase) &&
    java.util.Objects.equals(this.prosecutionCaseIdentifier, that.prosecutionCaseIdentifier) &&
    java.util.Objects.equals(this.removalReason, that.removalReason) &&
    java.util.Objects.equals(this.statementOfFacts, that.statementOfFacts) &&
    java.util.Objects.equals(this.statementOfFactsWelsh, that.statementOfFactsWelsh);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(appealProceedingsPending, breachProceedingsPending, caseMarkers, caseStatus, classOfCase, cpsOrganisation, defendants, id, initiationCode, isCpsOrgVerifyError, originatingOrganisation, policeOfficerInCase, prosecutionCaseIdentifier, removalReason, statementOfFacts, statementOfFactsWelsh);}

  public static class Builder {
    private Boolean appealProceedingsPending;

    private Boolean breachProceedingsPending;

    private List<Marker> caseMarkers;

    private String caseStatus;

    private String classOfCase;

    private String cpsOrganisation;

    private List<Defendant> defendants;

    private UUID id;

    private InitiationCode initiationCode;

    private Boolean isCpsOrgVerifyError;

    private String originatingOrganisation;

    private PoliceOfficerInCase policeOfficerInCase;

    private ProsecutionCaseIdentifier prosecutionCaseIdentifier;

    private String removalReason;

    private String statementOfFacts;

    private String statementOfFactsWelsh;

    public Builder withAppealProceedingsPending(final Boolean appealProceedingsPending) {
      this.appealProceedingsPending = appealProceedingsPending;
      return this;
    }

    public Builder withBreachProceedingsPending(final Boolean breachProceedingsPending) {
      this.breachProceedingsPending = breachProceedingsPending;
      return this;
    }

    public Builder withCaseMarkers(final List<Marker> caseMarkers) {
      this.caseMarkers = caseMarkers;
      return this;
    }

    public Builder withCaseStatus(final String caseStatus) {
      this.caseStatus = caseStatus;
      return this;
    }

    public Builder withClassOfCase(final String classOfCase) {
      this.classOfCase = classOfCase;
      return this;
    }

    public Builder withCpsOrganisation(final String cpsOrganisation) {
      this.cpsOrganisation = cpsOrganisation;
      return this;
    }

    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withInitiationCode(final InitiationCode initiationCode) {
      this.initiationCode = initiationCode;
      return this;
    }

    public Builder withIsCpsOrgVerifyError(final Boolean isCpsOrgVerifyError) {
      this.isCpsOrgVerifyError = isCpsOrgVerifyError;
      return this;
    }

    public Builder withOriginatingOrganisation(final String originatingOrganisation) {
      this.originatingOrganisation = originatingOrganisation;
      return this;
    }

    public Builder withPoliceOfficerInCase(final PoliceOfficerInCase policeOfficerInCase) {
      this.policeOfficerInCase = policeOfficerInCase;
      return this;
    }

    public Builder withProsecutionCaseIdentifier(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
      this.prosecutionCaseIdentifier = prosecutionCaseIdentifier;
      return this;
    }

    public Builder withRemovalReason(final String removalReason) {
      this.removalReason = removalReason;
      return this;
    }

    public Builder withStatementOfFacts(final String statementOfFacts) {
      this.statementOfFacts = statementOfFacts;
      return this;
    }

    public Builder withStatementOfFactsWelsh(final String statementOfFactsWelsh) {
      this.statementOfFactsWelsh = statementOfFactsWelsh;
      return this;
    }

    public Builder withValuesFrom(final ProsecutionCase prosecutionCase) {
      this.appealProceedingsPending = prosecutionCase.getAppealProceedingsPending();
      this.breachProceedingsPending = prosecutionCase.getBreachProceedingsPending();
      this.caseMarkers = prosecutionCase.getCaseMarkers();
      this.caseStatus = prosecutionCase.getCaseStatus();
      this.classOfCase = prosecutionCase.getClassOfCase();
      this.cpsOrganisation = prosecutionCase.getCpsOrganisation();
      this.defendants = prosecutionCase.getDefendants();
      this.id = prosecutionCase.getId();
      this.initiationCode = prosecutionCase.getInitiationCode();
      this.isCpsOrgVerifyError = prosecutionCase.getIsCpsOrgVerifyError();
      this.originatingOrganisation = prosecutionCase.getOriginatingOrganisation();
      this.policeOfficerInCase = prosecutionCase.getPoliceOfficerInCase();
      this.prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
      this.removalReason = prosecutionCase.getRemovalReason();
      this.statementOfFacts = prosecutionCase.getStatementOfFacts();
      this.statementOfFactsWelsh = prosecutionCase.getStatementOfFactsWelsh();
      return this;
    }

    public ProsecutionCase build() {
      return new uk.gov.justice.listing.events.ProsecutionCase(appealProceedingsPending, breachProceedingsPending, caseMarkers, caseStatus, classOfCase, cpsOrganisation, defendants, id, initiationCode, isCpsOrgVerifyError, originatingOrganisation, policeOfficerInCase, prosecutionCaseIdentifier, removalReason, statementOfFacts, statementOfFactsWelsh);
    }
  }
}
