package uk.gov.moj.cpp.listing.domain;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067"})
public class Defendant {
    private final Optional<BailStatus> bailStatus;

    private final Optional<String> custodyTimeLimit;

    private final Optional<String> dateOfBirth;

    private final Optional<String> datesToAvoid;

    private final Optional<String> defenceOrganisation;

    private final Optional<String> firstName;

    private final Optional<HearingLanguageNeeds> hearingLanguageNeeds;

    private final UUID id;

    private final Optional<String> lastName;

    private final UUID prosecutionCaseId;

    private final List<Offence> offences;

    private final Optional<String> organisationName;

    private final Optional<String> specificRequirements;

  private final Optional<Boolean> isYouth;

  public Defendant(final Optional<BailStatus> bailStatus, final Optional<String> custodyTimeLimit, final Optional<String> dateOfBirth, final Optional<String> datesToAvoid, final Optional<String> defenceOrganisation, final Optional<String> firstName, final Optional<HearingLanguageNeeds> hearingLanguageNeeds, final UUID id, final Optional<String> lastName, final List<Offence> offences, final Optional<String> organisationName, final Optional<String> specificRequirements, final UUID prosecutionCaseId, final Optional<Boolean> isYouth) {
    this.bailStatus = bailStatus;
    this.custodyTimeLimit = custodyTimeLimit;
    this.dateOfBirth = dateOfBirth;
    this.datesToAvoid = datesToAvoid;
    this.defenceOrganisation = defenceOrganisation;
    this.firstName = firstName;
    this.hearingLanguageNeeds = hearingLanguageNeeds;
    this.id = id;
    this.lastName = lastName;
    this.prosecutionCaseId = prosecutionCaseId;
    this.offences = offences == null ? emptyList() : offences;
    this.organisationName = organisationName;
    this.specificRequirements = specificRequirements;
    this.isYouth = isYouth;

    }

    public Optional<BailStatus> getBailStatus() {
        return bailStatus;
    }

    public Optional<String> getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public Optional<String> getDateOfBirth() {
        return dateOfBirth;
    }

    public Optional<String> getDatesToAvoid() {
        return datesToAvoid;
    }

    public Optional<String> getDefenceOrganisation() {
        return defenceOrganisation;
    }

    public Optional<String> getFirstName() {
        return firstName;
    }

    public Optional<HearingLanguageNeeds> getHearingLanguageNeeds() {
        return hearingLanguageNeeds;
    }

    public UUID getId() {
        return id;
    }

    public Optional<String> getLastName() {
        return lastName;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public Optional<String> getOrganisationName() {
        return organisationName;
    }

    public Optional<String> getSpecificRequirements() {
        return specificRequirements;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

  public Optional<Boolean> getIsYouth() { return isYouth;  }

  public static Builder defendant() {
    return new Defendant.Builder();
  }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Defendant that = (Defendant) obj;

        return java.util.Objects.equals(this.bailStatus, that.bailStatus) &&
                java.util.Objects.equals(this.custodyTimeLimit, that.custodyTimeLimit) &&
                java.util.Objects.equals(this.dateOfBirth, that.dateOfBirth) &&
                java.util.Objects.equals(this.datesToAvoid, that.datesToAvoid) &&
                java.util.Objects.equals(this.defenceOrganisation, that.defenceOrganisation) &&
                java.util.Objects.equals(this.firstName, that.firstName) &&
                java.util.Objects.equals(this.hearingLanguageNeeds, that.hearingLanguageNeeds) &&
                java.util.Objects.equals(this.id, that.id) &&
                java.util.Objects.equals(this.lastName, that.lastName) &&
                java.util.Objects.equals(this.offences, that.offences) &&
                java.util.Objects.equals(this.organisationName, that.organisationName) &&
                java.util.Objects.equals(this.specificRequirements, that.specificRequirements) &&
                java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(bailStatus, custodyTimeLimit, dateOfBirth, datesToAvoid, defenceOrganisation, firstName, hearingLanguageNeeds, id, lastName, offences, organisationName, specificRequirements, prosecutionCaseId);
    }

    @Override
    public String toString() {
        return "Defendant{" +
                "bailStatus='" + bailStatus + "'," +
                "custodyTimeLimit='" + custodyTimeLimit + "'," +
                "dateOfBirth='" + dateOfBirth + "'," +
                "datesToAvoid='" + datesToAvoid + "'," +
                "defenceOrganisation='" + defenceOrganisation + "'," +
                "firstName='" + firstName + "'," +
                "hearingLanguageNeeds='" + hearingLanguageNeeds + "'," +
                "id='" + id + "'," +
                "lastName='" + lastName + "'," +
                "offences='" + offences + "'," +
                "organisationName='" + organisationName + "'," +
                "specificRequirements='" + specificRequirements + "'," +
                "prosecutionCaseId='" + prosecutionCaseId + "'" +
                "}";
    }

    public static class Builder {
        private Optional<BailStatus> bailStatus;

        private Optional<String> custodyTimeLimit;

        private Optional<String> dateOfBirth;

        private Optional<String> datesToAvoid;

        private Optional<String> defenceOrganisation;

        private Optional<String> firstName;

        private Optional<HearingLanguageNeeds> hearingLanguageNeeds;

        private UUID id;

        private Optional<String> lastName;

        private List<Offence> offences;

        private Optional<String> organisationName;

        private Optional<String> specificRequirements;

        private UUID prosecutionCaseId;

    private Optional<Boolean> isYouth;

    public Builder withBailStatus(final Optional<BailStatus> bailStatus) {
      this.bailStatus = bailStatus;
      return this;
    }

        public Builder withCustodyTimeLimit(final Optional<String> custodyTimeLimit) {
            this.custodyTimeLimit = custodyTimeLimit;
            return this;
        }

        public Builder withDateOfBirth(final Optional<String> dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder withDatesToAvoid(final Optional<String> datesToAvoid) {
            this.datesToAvoid = datesToAvoid;
            return this;
        }

        public Builder withDefenceOrganisation(final Optional<String> defenceOrganisation) {
            this.defenceOrganisation = defenceOrganisation;
            return this;
        }

        public Builder withFirstName(final Optional<String> firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withHearingLanguageNeeds(final Optional<HearingLanguageNeeds> hearingLanguageNeeds) {
            this.hearingLanguageNeeds = hearingLanguageNeeds;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withLastName(final Optional<String> lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withOffences(final List<Offence> offences) {
            this.offences = offences;
            return this;
        }

        public Builder withOrganisationName(final Optional<String> organisationName) {
            this.organisationName = organisationName;
            return this;
        }

        public Builder withSpecificRequirements(final Optional<String> specificRequirements) {
            this.specificRequirements = specificRequirements;
            return this;
        }

        public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
            this.prosecutionCaseId = prosecutionCaseId;
            return this;
        }

    public Builder withIsYouth(final Optional<Boolean> isYouth) {
      this.isYouth = isYouth;
      return this;
    }

    public Defendant build() {
      return new Defendant(bailStatus, custodyTimeLimit, dateOfBirth, datesToAvoid, defenceOrganisation, firstName, hearingLanguageNeeds, id, lastName, offences, organisationName, specificRequirements, prosecutionCaseId, isYouth);
    }
  }
}
