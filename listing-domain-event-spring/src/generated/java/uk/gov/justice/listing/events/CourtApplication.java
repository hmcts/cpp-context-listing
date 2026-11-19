package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.LaaReference;

public class CourtApplication implements Serializable {
  private static final long serialVersionUID = 641333116650746455L;

  private final ApplicantRespondent applicant;

  private final String applicationParticulars;

  private final String applicationReference;

  private final String applicationType;

  private final UUID id;

  private final Boolean isEjected;

  private final LaaReference laaApplnReference;

  private final List<UUID> linkedCaseIds;

  private final List<Offence> offences;

  private final UUID parentApplicationId;

  private final List<ApplicantRespondent> respondents;

  private final Boolean restrictCourtApplicationType;

  private final Boolean restrictFromCourtList;

  private final ApplicantRespondent subject;

  @JsonCreator
  public CourtApplication(final ApplicantRespondent applicant, final String applicationParticulars, final String applicationReference, final String applicationType, final UUID id, final Boolean isEjected, final LaaReference laaApplnReference, final List<UUID> linkedCaseIds, final List<Offence> offences, final UUID parentApplicationId, final List<ApplicantRespondent> respondents, final Boolean restrictCourtApplicationType, final Boolean restrictFromCourtList, final ApplicantRespondent subject) {
    this.applicant = applicant;
    this.applicationParticulars = applicationParticulars;
    this.applicationReference = applicationReference;
    this.applicationType = applicationType;
    this.id = id;
    this.isEjected = isEjected;
    this.laaApplnReference = laaApplnReference;
    this.linkedCaseIds = linkedCaseIds;
    this.offences = offences;
    this.parentApplicationId = parentApplicationId;
    this.respondents = respondents;
    this.restrictCourtApplicationType = restrictCourtApplicationType;
    this.restrictFromCourtList = restrictFromCourtList;
    this.subject = subject;
  }

  public ApplicantRespondent getApplicant() {
    return applicant;
  }

  public String getApplicationParticulars() {
    return applicationParticulars;
  }

  public String getApplicationReference() {
    return applicationReference;
  }

  public String getApplicationType() {
    return applicationType;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getIsEjected() {
    return isEjected;
  }

  public LaaReference getLaaApplnReference() {
    return laaApplnReference;
  }

  public List<UUID> getLinkedCaseIds() {
    return linkedCaseIds;
  }

  public List<Offence> getOffences() {
    return offences;
  }

  public UUID getParentApplicationId() {
    return parentApplicationId;
  }

  public List<ApplicantRespondent> getRespondents() {
    return respondents;
  }

  public Boolean getRestrictCourtApplicationType() {
    return restrictCourtApplicationType;
  }

  public Boolean getRestrictFromCourtList() {
    return restrictFromCourtList;
  }

  public ApplicantRespondent getSubject() {
    return subject;
  }

  public static Builder courtApplication() {
    return new uk.gov.justice.listing.events.CourtApplication.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CourtApplication that = (uk.gov.justice.listing.events.CourtApplication) obj;

    return java.util.Objects.equals(this.applicant, that.applicant) &&
    java.util.Objects.equals(this.applicationParticulars, that.applicationParticulars) &&
    java.util.Objects.equals(this.applicationReference, that.applicationReference) &&
    java.util.Objects.equals(this.applicationType, that.applicationType) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isEjected, that.isEjected) &&
    java.util.Objects.equals(this.laaApplnReference, that.laaApplnReference) &&
    java.util.Objects.equals(this.linkedCaseIds, that.linkedCaseIds) &&
    java.util.Objects.equals(this.offences, that.offences) &&
    java.util.Objects.equals(this.parentApplicationId, that.parentApplicationId) &&
    java.util.Objects.equals(this.respondents, that.respondents) &&
    java.util.Objects.equals(this.restrictCourtApplicationType, that.restrictCourtApplicationType) &&
    java.util.Objects.equals(this.restrictFromCourtList, that.restrictFromCourtList) &&
    java.util.Objects.equals(this.subject, that.subject);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicant, applicationParticulars, applicationReference, applicationType, id, isEjected, laaApplnReference, linkedCaseIds, offences, parentApplicationId, respondents, restrictCourtApplicationType, restrictFromCourtList, subject);}

  public static class Builder {
    private ApplicantRespondent applicant;

    private String applicationParticulars;

    private String applicationReference;

    private String applicationType;

    private UUID id;

    private Boolean isEjected;

    private LaaReference laaApplnReference;

    private List<UUID> linkedCaseIds;

    private List<Offence> offences;

    private UUID parentApplicationId;

    private List<ApplicantRespondent> respondents;

    private Boolean restrictCourtApplicationType;

    private Boolean restrictFromCourtList;

    private ApplicantRespondent subject;

    public Builder withApplicant(final ApplicantRespondent applicant) {
      this.applicant = applicant;
      return this;
    }

    public Builder withApplicationParticulars(final String applicationParticulars) {
      this.applicationParticulars = applicationParticulars;
      return this;
    }

    public Builder withApplicationReference(final String applicationReference) {
      this.applicationReference = applicationReference;
      return this;
    }

    public Builder withApplicationType(final String applicationType) {
      this.applicationType = applicationType;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withIsEjected(final Boolean isEjected) {
      this.isEjected = isEjected;
      return this;
    }

    public Builder withLaaApplnReference(final LaaReference laaApplnReference) {
      this.laaApplnReference = laaApplnReference;
      return this;
    }

    public Builder withLinkedCaseIds(final List<UUID> linkedCaseIds) {
      this.linkedCaseIds = linkedCaseIds;
      return this;
    }

    public Builder withOffences(final List<Offence> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withParentApplicationId(final UUID parentApplicationId) {
      this.parentApplicationId = parentApplicationId;
      return this;
    }

    public Builder withRespondents(final List<ApplicantRespondent> respondents) {
      this.respondents = respondents;
      return this;
    }

    public Builder withRestrictCourtApplicationType(final Boolean restrictCourtApplicationType) {
      this.restrictCourtApplicationType = restrictCourtApplicationType;
      return this;
    }

    public Builder withRestrictFromCourtList(final Boolean restrictFromCourtList) {
      this.restrictFromCourtList = restrictFromCourtList;
      return this;
    }

    public Builder withSubject(final ApplicantRespondent subject) {
      this.subject = subject;
      return this;
    }

    public Builder withValuesFrom(final CourtApplication courtApplication) {
      this.applicant = courtApplication.getApplicant();
      this.applicationParticulars = courtApplication.getApplicationParticulars();
      this.applicationReference = courtApplication.getApplicationReference();
      this.applicationType = courtApplication.getApplicationType();
      this.id = courtApplication.getId();
      this.isEjected = courtApplication.getIsEjected();
      this.laaApplnReference = courtApplication.getLaaApplnReference();
      this.linkedCaseIds = courtApplication.getLinkedCaseIds();
      this.offences = courtApplication.getOffences();
      this.parentApplicationId = courtApplication.getParentApplicationId();
      this.respondents = courtApplication.getRespondents();
      this.restrictCourtApplicationType = courtApplication.getRestrictCourtApplicationType();
      this.restrictFromCourtList = courtApplication.getRestrictFromCourtList();
      this.subject = courtApplication.getSubject();
      return this;
    }

    public CourtApplication build() {
      return new uk.gov.justice.listing.events.CourtApplication(applicant, applicationParticulars, applicationReference, applicationType, id, isEjected, laaApplnReference, linkedCaseIds, offences, parentApplicationId, respondents, restrictCourtApplicationType, restrictFromCourtList, subject);
    }
  }
}
