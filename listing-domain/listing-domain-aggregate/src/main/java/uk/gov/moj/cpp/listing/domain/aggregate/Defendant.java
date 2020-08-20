package uk.gov.moj.cpp.listing.domain.aggregate;

import uk.gov.justice.listing.events.HearingLanguageNeeds;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S1067", "PMD.BeanMembersShouldSerialize", "squid:S2384"})
public class Defendant implements Serializable {
    private final Address address;

    private final AssociatedDefenceOrganisation associatedDefenceOrganisation;

    private final BailStatus bailStatus;

    private final ZonedDateTime courtProceedingsInitiated;

    private final String custodyTimeLimit;

    private final String dateOfBirth;

    private final String datesToAvoid;

    private final String defenceOrganisation;

    private final String firstName;

    private final HearingLanguageNeeds hearingLanguageNeeds;

    private final UUID id;

    private final Boolean isYouth;

    private final String lastName;

    private final String legalAidStatus;

    private final UUID masterDefendantId;

    private final String nationalityDescription;

    private final List<Offence> offences;

    private final String organisationName;

    private final Boolean proceedingsConcluded;

    private final Boolean restrictFromCourtList;

    private final String specificRequirements;

    public Defendant(final Address address, final AssociatedDefenceOrganisation associatedDefenceOrganisation, final BailStatus bailStatus, final ZonedDateTime courtProceedingsInitiated, final String custodyTimeLimit, final String dateOfBirth, final String datesToAvoid, final String defenceOrganisation, final String firstName, final HearingLanguageNeeds hearingLanguageNeeds, final UUID id, final Boolean isYouth, final String lastName, final String legalAidStatus, final UUID masterDefendantId, final String nationalityDescription, final List<Offence> offences, final String organisationName, final Boolean proceedingsConcluded, final Boolean restrictFromCourtList, final String specificRequirements) {
        this.address = address;
        this.associatedDefenceOrganisation = associatedDefenceOrganisation;
        this.bailStatus = bailStatus;
        this.courtProceedingsInitiated = courtProceedingsInitiated;
        this.custodyTimeLimit = custodyTimeLimit;
        this.dateOfBirth = dateOfBirth;
        this.datesToAvoid = datesToAvoid;
        this.defenceOrganisation = defenceOrganisation;
        this.firstName = firstName;
        this.hearingLanguageNeeds = hearingLanguageNeeds;
        this.id = id;
        this.isYouth = isYouth;
        this.lastName = lastName;
        this.legalAidStatus = legalAidStatus;
        this.masterDefendantId = masterDefendantId;
        this.nationalityDescription = nationalityDescription;
        this.offences = offences;
        this.organisationName = organisationName;
        this.proceedingsConcluded = proceedingsConcluded;
        this.restrictFromCourtList = restrictFromCourtList;
        this.specificRequirements = specificRequirements;
    }

    public Address getAddress() {
        return address;
    }

    public AssociatedDefenceOrganisation getAssociatedDefenceOrganisation() {
        return associatedDefenceOrganisation;
    }

    public BailStatus getBailStatus() {
        return bailStatus;
    }

    public ZonedDateTime getCourtProceedingsInitiated() {
        return courtProceedingsInitiated;
    }

    public String getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getDatesToAvoid() {
        return datesToAvoid;
    }

    public String getDefenceOrganisation() {
        return defenceOrganisation;
    }

    public String getFirstName() {
        return firstName;
    }

    public HearingLanguageNeeds getHearingLanguageNeeds() {
        return hearingLanguageNeeds;
    }

    public UUID getId() {
        return id;
    }

    public Boolean getIsYouth() {
        return isYouth;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLegalAidStatus() {
        return legalAidStatus;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public String getNationalityDescription() {
        return nationalityDescription;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public Boolean getProceedingsConcluded() {
        return proceedingsConcluded;
    }

    public Boolean getRestrictFromCourtList() {
        return restrictFromCourtList;
    }

    public String getSpecificRequirements() {
        return specificRequirements;
    }

    public static Builder defendant() {
        return new Builder();
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

        return java.util.Objects.equals(this.address, that.address) &&
                java.util.Objects.equals(this.associatedDefenceOrganisation, that.associatedDefenceOrganisation) &&
                java.util.Objects.equals(this.bailStatus, that.bailStatus) &&
                java.util.Objects.equals(this.courtProceedingsInitiated, that.courtProceedingsInitiated) &&
                java.util.Objects.equals(this.custodyTimeLimit, that.custodyTimeLimit) &&
                java.util.Objects.equals(this.dateOfBirth, that.dateOfBirth) &&
                java.util.Objects.equals(this.datesToAvoid, that.datesToAvoid) &&
                java.util.Objects.equals(this.defenceOrganisation, that.defenceOrganisation) &&
                java.util.Objects.equals(this.firstName, that.firstName) &&
                java.util.Objects.equals(this.hearingLanguageNeeds, that.hearingLanguageNeeds) &&
                java.util.Objects.equals(this.id, that.id) &&
                java.util.Objects.equals(this.isYouth, that.isYouth) &&
                java.util.Objects.equals(this.lastName, that.lastName) &&
                java.util.Objects.equals(this.legalAidStatus, that.legalAidStatus) &&
                java.util.Objects.equals(this.masterDefendantId, that.masterDefendantId) &&
                java.util.Objects.equals(this.nationalityDescription, that.nationalityDescription) &&
                java.util.Objects.equals(this.offences, that.offences) &&
                java.util.Objects.equals(this.organisationName, that.organisationName) &&
                java.util.Objects.equals(this.proceedingsConcluded, that.proceedingsConcluded) &&
                java.util.Objects.equals(this.restrictFromCourtList, that.restrictFromCourtList) &&
                java.util.Objects.equals(this.specificRequirements, that.specificRequirements);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(address, associatedDefenceOrganisation, bailStatus, courtProceedingsInitiated, custodyTimeLimit, dateOfBirth, datesToAvoid, defenceOrganisation, firstName, hearingLanguageNeeds, id, isYouth, lastName, legalAidStatus, masterDefendantId, nationalityDescription, offences, organisationName, proceedingsConcluded, restrictFromCourtList, specificRequirements);
    }

    @Override
    public String toString() {
        return "Defendant{" +
                "address='" + address + "'," +
                "associatedDefenceOrganisation='" + associatedDefenceOrganisation + "'," +
                "bailStatus='" + bailStatus + "'," +
                "courtProceedingsInitiated='" + courtProceedingsInitiated + "'," +
                "custodyTimeLimit='" + custodyTimeLimit + "'," +
                "dateOfBirth='" + dateOfBirth + "'," +
                "datesToAvoid='" + datesToAvoid + "'," +
                "defenceOrganisation='" + defenceOrganisation + "'," +
                "firstName='" + firstName + "'," +
                "hearingLanguageNeeds='" + hearingLanguageNeeds + "'," +
                "id='" + id + "'," +
                "isYouth='" + isYouth + "'," +
                "lastName='" + lastName + "'," +
                "legalAidStatus='" + legalAidStatus + "'," +
                "masterDefendantId='" + masterDefendantId + "'," +
                "nationalityDescription='" + nationalityDescription + "'," +
                "offences='" + offences + "'," +
                "organisationName='" + organisationName + "'," +
                "proceedingsConcluded='" + proceedingsConcluded + "'," +
                "restrictFromCourtList='" + restrictFromCourtList + "'," +
                "specificRequirements='" + specificRequirements + "'" +
                "}";
    }

    @SuppressWarnings({"PMD.BeanMembersShouldSerialize", "squid:S2384"})
    public static final class Builder {
        private Address address;

        private AssociatedDefenceOrganisation associatedDefenceOrganisation;

        private BailStatus bailStatus;

        private ZonedDateTime courtProceedingsInitiated;

        private String custodyTimeLimit;

        private String dateOfBirth;

        private String datesToAvoid;

        private String defenceOrganisation;

        private String firstName;

        private HearingLanguageNeeds hearingLanguageNeeds;

        private UUID id;

        private Boolean isYouth;

        private String lastName;

        private String legalAidStatus;

        private UUID masterDefendantId;

        private String nationalityDescription;

        private List<Offence> offences;

        private String organisationName;

        private Boolean proceedingsConcluded;

        private Boolean restrictFromCourtList;

        private String specificRequirements;

        public Builder withAddress(final Address address) {
            this.address = address;
            return this;
        }

        public Builder withAssociatedDefenceOrganisation(final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
            this.associatedDefenceOrganisation = associatedDefenceOrganisation;
            return this;
        }

        public Builder withBailStatus(final BailStatus bailStatus) {
            this.bailStatus = bailStatus;
            return this;
        }

        public Builder withCourtProceedingsInitiated(final ZonedDateTime courtProceedingsInitiated) {
            this.courtProceedingsInitiated = courtProceedingsInitiated;
            return this;
        }

        public Builder withCustodyTimeLimit(final String custodyTimeLimit) {
            this.custodyTimeLimit = custodyTimeLimit;
            return this;
        }

        public Builder withDateOfBirth(final String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder withDatesToAvoid(final String datesToAvoid) {
            this.datesToAvoid = datesToAvoid;
            return this;
        }

        public Builder withDefenceOrganisation(final String defenceOrganisation) {
            this.defenceOrganisation = defenceOrganisation;
            return this;
        }

        public Builder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withHearingLanguageNeeds(final HearingLanguageNeeds hearingLanguageNeeds) {
            this.hearingLanguageNeeds = hearingLanguageNeeds;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withIsYouth(final Boolean isYouth) {
            this.isYouth = isYouth;
            return this;
        }


        public Builder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withLegalAidStatus(final String legalAidStatus) {
            this.legalAidStatus = legalAidStatus;
            return this;
        }

        public Builder withMasterDefendantId(final UUID masterDefendantId) {
            this.masterDefendantId = masterDefendantId;
            return this;
        }

        public Builder withNationalityDescription(final String nationalityDescription) {
            this.nationalityDescription = nationalityDescription;
            return this;
        }

        public Builder withOffences(final List<Offence> offences) {
            this.offences = offences;
            return this;
        }

        public Builder withOrganisationName(final String organisationName) {
            this.organisationName = organisationName;
            return this;
        }

        public Builder withProceedingsConcluded(final Boolean proceedingsConcluded) {
            this.proceedingsConcluded = proceedingsConcluded;
            return this;
        }

        public Builder withRestrictFromCourtList(final Boolean restrictFromCourtList) {
            this.restrictFromCourtList = restrictFromCourtList;
            return this;
        }


        public Builder withSpecificRequirements(final String specificRequirements) {
            this.specificRequirements = specificRequirements;
            return this;
        }

        public Defendant build() {
            return new Defendant(address, associatedDefenceOrganisation, bailStatus, courtProceedingsInitiated, custodyTimeLimit, dateOfBirth, datesToAvoid, defenceOrganisation, firstName, hearingLanguageNeeds, id, isYouth, lastName, legalAidStatus, masterDefendantId, nationalityDescription, offences, organisationName, proceedingsConcluded, restrictFromCourtList, specificRequirements);
        }
    }
}
