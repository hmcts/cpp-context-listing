package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class CourtApplicationParty implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Boolean appointmentNotificationRequired;

  private final AssociatedDefenceOrganisation associatedDefenceOrganisation;

  private final UUID id;

  private final MasterDefendant masterDefendant;

  private final Boolean notificationRequired;

  private final Organisation organisation;

  private final List<AssociatedPerson> organisationPersons;

  private final Person personDetails;

  private final ProsecutingAuthority prosecutingAuthority;

  private final Organisation representationOrganisation;

  private final Boolean summonsRequired;

  private final String synonym;

  private final String updatedOn;

  @JsonCreator
  public CourtApplicationParty(final Boolean appointmentNotificationRequired, final AssociatedDefenceOrganisation associatedDefenceOrganisation, final UUID id, final MasterDefendant masterDefendant, final Boolean notificationRequired, final Organisation organisation, final List<AssociatedPerson> organisationPersons, final Person personDetails, final ProsecutingAuthority prosecutingAuthority, final Organisation representationOrganisation, final Boolean summonsRequired, final String synonym, final String updatedOn) {
    this.appointmentNotificationRequired = appointmentNotificationRequired;
    this.associatedDefenceOrganisation = associatedDefenceOrganisation;
    this.id = id;
    this.masterDefendant = masterDefendant;
    this.notificationRequired = notificationRequired;
    this.organisation = organisation;
    this.organisationPersons = organisationPersons;
    this.personDetails = personDetails;
    this.prosecutingAuthority = prosecutingAuthority;
    this.representationOrganisation = representationOrganisation;
    this.summonsRequired = summonsRequired;
    this.synonym = synonym;
    this.updatedOn = updatedOn;
  }

  public Boolean getAppointmentNotificationRequired() {
    return appointmentNotificationRequired;
  }

  public AssociatedDefenceOrganisation getAssociatedDefenceOrganisation() {
    return associatedDefenceOrganisation;
  }

  public UUID getId() {
    return id;
  }

  public MasterDefendant getMasterDefendant() {
    return masterDefendant;
  }

  public Boolean getNotificationRequired() {
    return notificationRequired;
  }

  public Organisation getOrganisation() {
    return organisation;
  }

  public List<AssociatedPerson> getOrganisationPersons() {
    return organisationPersons;
  }

  public Person getPersonDetails() {
    return personDetails;
  }

  public ProsecutingAuthority getProsecutingAuthority() {
    return prosecutingAuthority;
  }

  public Organisation getRepresentationOrganisation() {
    return representationOrganisation;
  }

  public Boolean getSummonsRequired() {
    return summonsRequired;
  }

  public String getSynonym() {
    return synonym;
  }

  public String getUpdatedOn() {
    return updatedOn;
  }

  public static Builder courtApplicationParty() {
    return new uk.gov.justice.core.courts.CourtApplicationParty.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtApplicationParty that = (uk.gov.justice.core.courts.CourtApplicationParty) obj;

    return java.util.Objects.equals(this.appointmentNotificationRequired, that.appointmentNotificationRequired) &&
    java.util.Objects.equals(this.associatedDefenceOrganisation, that.associatedDefenceOrganisation) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.masterDefendant, that.masterDefendant) &&
    java.util.Objects.equals(this.notificationRequired, that.notificationRequired) &&
    java.util.Objects.equals(this.organisation, that.organisation) &&
    java.util.Objects.equals(this.organisationPersons, that.organisationPersons) &&
    java.util.Objects.equals(this.personDetails, that.personDetails) &&
    java.util.Objects.equals(this.prosecutingAuthority, that.prosecutingAuthority) &&
    java.util.Objects.equals(this.representationOrganisation, that.representationOrganisation) &&
    java.util.Objects.equals(this.summonsRequired, that.summonsRequired) &&
    java.util.Objects.equals(this.synonym, that.synonym) &&
    java.util.Objects.equals(this.updatedOn, that.updatedOn);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(appointmentNotificationRequired, associatedDefenceOrganisation, id, masterDefendant, notificationRequired, organisation, organisationPersons, personDetails, prosecutingAuthority, representationOrganisation, summonsRequired, synonym, updatedOn);}

  public static class Builder {
    private Boolean appointmentNotificationRequired;

    private AssociatedDefenceOrganisation associatedDefenceOrganisation;

    private UUID id;

    private MasterDefendant masterDefendant;

    private Boolean notificationRequired;

    private Organisation organisation;

    private List<AssociatedPerson> organisationPersons;

    private Person personDetails;

    private ProsecutingAuthority prosecutingAuthority;

    private Organisation representationOrganisation;

    private Boolean summonsRequired;

    private String synonym;

    private String updatedOn;

    public Builder withAppointmentNotificationRequired(final Boolean appointmentNotificationRequired) {
      this.appointmentNotificationRequired = appointmentNotificationRequired;
      return this;
    }

    public Builder withAssociatedDefenceOrganisation(final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
      this.associatedDefenceOrganisation = associatedDefenceOrganisation;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withMasterDefendant(final MasterDefendant masterDefendant) {
      this.masterDefendant = masterDefendant;
      return this;
    }

    public Builder withNotificationRequired(final Boolean notificationRequired) {
      this.notificationRequired = notificationRequired;
      return this;
    }

    public Builder withOrganisation(final Organisation organisation) {
      this.organisation = organisation;
      return this;
    }

    public Builder withOrganisationPersons(final List<AssociatedPerson> organisationPersons) {
      this.organisationPersons = organisationPersons;
      return this;
    }

    public Builder withPersonDetails(final Person personDetails) {
      this.personDetails = personDetails;
      return this;
    }

    public Builder withProsecutingAuthority(final ProsecutingAuthority prosecutingAuthority) {
      this.prosecutingAuthority = prosecutingAuthority;
      return this;
    }

    public Builder withRepresentationOrganisation(final Organisation representationOrganisation) {
      this.representationOrganisation = representationOrganisation;
      return this;
    }

    public Builder withSummonsRequired(final Boolean summonsRequired) {
      this.summonsRequired = summonsRequired;
      return this;
    }

    public Builder withSynonym(final String synonym) {
      this.synonym = synonym;
      return this;
    }

    public Builder withUpdatedOn(final String updatedOn) {
      this.updatedOn = updatedOn;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationParty courtApplicationParty) {
      this.appointmentNotificationRequired = courtApplicationParty.getAppointmentNotificationRequired();
      this.associatedDefenceOrganisation = courtApplicationParty.getAssociatedDefenceOrganisation();
      this.id = courtApplicationParty.getId();
      this.masterDefendant = courtApplicationParty.getMasterDefendant();
      this.notificationRequired = courtApplicationParty.getNotificationRequired();
      this.organisation = courtApplicationParty.getOrganisation();
      this.organisationPersons = courtApplicationParty.getOrganisationPersons();
      this.personDetails = courtApplicationParty.getPersonDetails();
      this.prosecutingAuthority = courtApplicationParty.getProsecutingAuthority();
      this.representationOrganisation = courtApplicationParty.getRepresentationOrganisation();
      this.summonsRequired = courtApplicationParty.getSummonsRequired();
      this.synonym = courtApplicationParty.getSynonym();
      this.updatedOn = courtApplicationParty.getUpdatedOn();
      return this;
    }

    public CourtApplicationParty build() {
      return new uk.gov.justice.core.courts.CourtApplicationParty(appointmentNotificationRequired, associatedDefenceOrganisation, id, masterDefendant, notificationRequired, organisation, organisationPersons, personDetails, prosecutingAuthority, representationOrganisation, summonsRequired, synonym, updatedOn);
    }
  }
}
