package uk.gov.justice.hearing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.JurisdictionType;

public class TrialVacated implements Serializable {
  private static final long serialVersionUID = 5433828913769985343L;

  private final List<ApplicationDetails> applicationDetails;

  private final List<CaseDetails> caseDetails;

  private final Boolean hasInterpreter;

  private final ZonedDateTime hearingDateTime;

  private final UUID hearingId;

  private final JurisdictionType jurisdictionType;

  private final UUID vacatedTrialReasonId;

  @JsonCreator
  public TrialVacated(final List<ApplicationDetails> applicationDetails, final List<CaseDetails> caseDetails, final Boolean hasInterpreter, final ZonedDateTime hearingDateTime, final UUID hearingId, final JurisdictionType jurisdictionType, final UUID vacatedTrialReasonId) {
    this.applicationDetails = applicationDetails;
    this.caseDetails = caseDetails;
    this.hasInterpreter = hasInterpreter;
    this.hearingDateTime = hearingDateTime;
    this.hearingId = hearingId;
    this.jurisdictionType = jurisdictionType;
    this.vacatedTrialReasonId = vacatedTrialReasonId;
  }

  public List<ApplicationDetails> getApplicationDetails() {
    return applicationDetails;
  }

  public List<CaseDetails> getCaseDetails() {
    return caseDetails;
  }

  public Boolean getHasInterpreter() {
    return hasInterpreter;
  }

  public ZonedDateTime getHearingDateTime() {
    return hearingDateTime;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public UUID getVacatedTrialReasonId() {
    return vacatedTrialReasonId;
  }

  public static Builder trialVacated() {
    return new uk.gov.justice.hearing.courts.TrialVacated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.hearing.courts.TrialVacated that = (uk.gov.justice.hearing.courts.TrialVacated) obj;

    return java.util.Objects.equals(this.applicationDetails, that.applicationDetails) &&
    java.util.Objects.equals(this.caseDetails, that.caseDetails) &&
    java.util.Objects.equals(this.hasInterpreter, that.hasInterpreter) &&
    java.util.Objects.equals(this.hearingDateTime, that.hearingDateTime) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.vacatedTrialReasonId, that.vacatedTrialReasonId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationDetails, caseDetails, hasInterpreter, hearingDateTime, hearingId, jurisdictionType, vacatedTrialReasonId);}

  public static class Builder {
    private List<ApplicationDetails> applicationDetails;

    private List<CaseDetails> caseDetails;

    private Boolean hasInterpreter;

    private ZonedDateTime hearingDateTime;

    private UUID hearingId;

    private JurisdictionType jurisdictionType;

    private UUID vacatedTrialReasonId;

    public Builder withApplicationDetails(final List<ApplicationDetails> applicationDetails) {
      this.applicationDetails = applicationDetails;
      return this;
    }

    public Builder withCaseDetails(final List<CaseDetails> caseDetails) {
      this.caseDetails = caseDetails;
      return this;
    }

    public Builder withHasInterpreter(final Boolean hasInterpreter) {
      this.hasInterpreter = hasInterpreter;
      return this;
    }

    public Builder withHearingDateTime(final ZonedDateTime hearingDateTime) {
      this.hearingDateTime = hearingDateTime;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withJurisdictionType(final JurisdictionType jurisdictionType) {
      this.jurisdictionType = jurisdictionType;
      return this;
    }

    public Builder withVacatedTrialReasonId(final UUID vacatedTrialReasonId) {
      this.vacatedTrialReasonId = vacatedTrialReasonId;
      return this;
    }

    public Builder withValuesFrom(final TrialVacated trialVacated) {
      this.applicationDetails = trialVacated.getApplicationDetails();
      this.caseDetails = trialVacated.getCaseDetails();
      this.hasInterpreter = trialVacated.getHasInterpreter();
      this.hearingDateTime = trialVacated.getHearingDateTime();
      this.hearingId = trialVacated.getHearingId();
      this.jurisdictionType = trialVacated.getJurisdictionType();
      this.vacatedTrialReasonId = trialVacated.getVacatedTrialReasonId();
      return this;
    }

    public TrialVacated build() {
      return new uk.gov.justice.hearing.courts.TrialVacated(applicationDetails, caseDetails, hasInterpreter, hearingDateTime, hearingId, jurisdictionType, vacatedTrialReasonId);
    }
  }
}
