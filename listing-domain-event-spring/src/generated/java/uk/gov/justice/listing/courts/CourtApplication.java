package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.events.Offence;

public class CourtApplication implements Serializable {
  private static final long serialVersionUID = 8906956638457691646L;

  private final Applicant applicant;

  private final String applicationParticulars;

  private final String applicationReference;

  private final String applicationType;

  private final UUID id;

  private final List<UUID> linkedCaseIds;

  private final List<Offence> offences;

  private final UUID parentApplicationId;

  private final List<Respondents> respondents;

  private final Boolean restrictCourtApplicationType;

  private final Boolean restrictFromCourtList;

  private final Subject subject;

  @JsonCreator
  public CourtApplication(final Applicant applicant, final String applicationParticulars, final String applicationReference, final String applicationType, final UUID id, final List<UUID> linkedCaseIds, final List<Offence> offences, final UUID parentApplicationId, final List<Respondents> respondents, final Boolean restrictCourtApplicationType, final Boolean restrictFromCourtList, final Subject subject) {
    this.applicant = applicant;
    this.applicationParticulars = applicationParticulars;
    this.applicationReference = applicationReference;
    this.applicationType = applicationType;
    this.id = id;
    this.linkedCaseIds = linkedCaseIds;
    this.offences = offences;
    this.parentApplicationId = parentApplicationId;
    this.respondents = respondents;
    this.restrictCourtApplicationType = restrictCourtApplicationType;
    this.restrictFromCourtList = restrictFromCourtList;
    this.subject = subject;
  }

  public Applicant getApplicant() {
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

  public List<UUID> getLinkedCaseIds() {
    return linkedCaseIds;
  }

  public List<Offence> getOffences() {
    return offences;
  }

  public UUID getParentApplicationId() {
    return parentApplicationId;
  }

  public List<Respondents> getRespondents() {
    return respondents;
  }

  public Boolean getRestrictCourtApplicationType() {
    return restrictCourtApplicationType;
  }

  public Boolean getRestrictFromCourtList() {
    return restrictFromCourtList;
  }

  public Subject getSubject() {
    return subject;
  }

  public static Builder courtApplication() {
    return new uk.gov.justice.listing.courts.CourtApplication.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.CourtApplication that = (uk.gov.justice.listing.courts.CourtApplication) obj;

    return java.util.Objects.equals(this.applicant, that.applicant) &&
    java.util.Objects.equals(this.applicationParticulars, that.applicationParticulars) &&
    java.util.Objects.equals(this.applicationReference, that.applicationReference) &&
    java.util.Objects.equals(this.applicationType, that.applicationType) &&
    java.util.Objects.equals(this.id, that.id) &&
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
    return java.util.Objects.hash(applicant, applicationParticulars, applicationReference, applicationType, id, linkedCaseIds, offences, parentApplicationId, respondents, restrictCourtApplicationType, restrictFromCourtList, subject);}

  public static class Builder {
    private Applicant applicant;

    private String applicationParticulars;

    private String applicationReference;

    private String applicationType;

    private UUID id;

    private List<UUID> linkedCaseIds;

    private List<Offence> offences;

    private UUID parentApplicationId;

    private List<Respondents> respondents;

    private Boolean restrictCourtApplicationType;

    private Boolean restrictFromCourtList;

    private Subject subject;

    public Builder withApplicant(final Applicant applicant) {
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

    public Builder withRespondents(final List<Respondents> respondents) {
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

    public Builder withSubject(final Subject subject) {
      this.subject = subject;
      return this;
    }

    public Builder withValuesFrom(final CourtApplication courtApplication) {
      this.applicant = courtApplication.getApplicant();
      this.applicationParticulars = courtApplication.getApplicationParticulars();
      this.applicationReference = courtApplication.getApplicationReference();
      this.applicationType = courtApplication.getApplicationType();
      this.id = courtApplication.getId();
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
      return new uk.gov.justice.listing.courts.CourtApplication(applicant, applicationParticulars, applicationReference, applicationType, id, linkedCaseIds, offences, parentApplicationId, respondents, restrictCourtApplicationType, restrictFromCourtList, subject);
    }
  }
}
