package uk.gov.moj.cpp.listing.steps.data;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;

import java.util.Arrays;
import java.util.List;
import static java.util.Optional.of;

import uk.gov.justice.core.courts.DefendantAlias;
import uk.gov.justice.core.courts.BailStatus;

import java.util.Optional;
import java.util.UUID;


public class UpdatedDefendantData {

    private final BailStatus bailStatus;
    private final String custodyTimeLimit;
    private final String dateOfBirth;
    private final String firstName;
    private final UUID defendantId;
    private final String lastName;
    private final String legalEntityName;
    private final UUID legalEntityId;
    private final String organisationName;
    private final String specificRequirements;
    private final UUID courtCentreId;
    private final String pncId;
    private final List<DefendantAlias> aliases;
    private final Boolean restrictFromCourtList ;
    private final Boolean isYouth;

    public static UpdatedDefendantData updatedDefendantData(DefendantData defendantData) {
        return UpdatedDefendantData.Builder.UpdatedDefendantData()
                .withBailStatus(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(UUID.fromString("12e69486-4d01-3403-a50a-7419ca040635")).build())
                .withCustodyTimeLimit("2018-10-10")
                .withDateOfBirth("1975-01-01")
                .withDefendantId(defendantData.getDefendantId())
                .withFirstName("First Name")
                .withLastName("Last Name")
                .withOrganisationName("withOrganisationName")
                .withLegalEntityName("withOrganisationName")
                .withLegalEntityId(fromString("55b8e1fd-085d-4236-a14f-8a35d86db8b2"))
                .withSpecificRequirements("withSpecificRequirements")
                .withCourtCentreId(randomUUID())
                .withPncId("pncId")
                .withIsYouth(Boolean.TRUE)
                .withAliases(Arrays.asList(DefendantAlias.defendantAlias()
                										.withFirstName(of("Alias First Name"))
                										.withLastName(of("Alias Last Name"))
                										.build()))
                .build();
    }

    public UpdatedDefendantData(final BailStatus bailStatus,
                                final String custodyTimeLimit,
                                final String dateOfBirth,
                                final String firstName,
                                final UUID defendantId,
                                final String lastName,
                                final String organisationName,
                                final String legalEntityName,
                                final UUID legalEntityId,
                                final String specificRequirements,
                                final UUID courtCentreId,
                                final String pncId,
                                final List<DefendantAlias> aliases,
                                final Boolean restrictFromCourtList,
                                final Boolean isYouth) {
        this.bailStatus = bailStatus;
        this.custodyTimeLimit = custodyTimeLimit;
        this.dateOfBirth = dateOfBirth;
        this.firstName = firstName;
        this.defendantId = defendantId;
        this.lastName = lastName;
        this.organisationName = organisationName;
        this.legalEntityName = legalEntityName;
        this.legalEntityId = legalEntityId;
        this.specificRequirements = specificRequirements;
        this.courtCentreId = courtCentreId;
        this.pncId = pncId;
        this.aliases = aliases;
        this.restrictFromCourtList = restrictFromCourtList;
        this.isYouth = isYouth;
    }

    public BailStatus getBailStatus() {
        return bailStatus;
    }

    public String getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getFirstName() {
        return firstName;
    }

	public UUID getDefendantId() { return defendantId; }

    public String getLastName() { return lastName; }

    public String getLegalEntityName() { return legalEntityName; }

    public Boolean getRestrictFromCourtList() {
        return restrictFromCourtList;
    }

    public UUID getLegalEntityId() {
        return legalEntityId;
    }

    public String getOrganisationName() { return organisationName; }

    public String getSpecificRequirements() {
        return specificRequirements;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public String getPncId() {
		return pncId;
	}

    public Optional<Boolean> getYouth() {
        return Optional.ofNullable(isYouth);
    }

    public List<DefendantAlias> getAliases() {
		return aliases;
	}

	public static class Builder {
        private BailStatus bailStatus;
        private String custodyTimeLimit;
        private String dateOfBirth;
        private String firstName;
        private UUID defendantId;
        private String lastName;
        private String legalEntityName;
        private UUID legalEntityId;
        private String organisationName;
        private String specificRequirements;
        private UUID courtCentreId;
        private String pncId;
        private List<DefendantAlias> aliases;
        private Boolean restrictFromCourtList ;
        private Boolean isYouth;

        public static Builder UpdatedDefendantData() {
            return new Builder();
        }

        public Builder withBailStatus(final BailStatus bailStatus) {
            this.bailStatus = bailStatus;
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

        public Builder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public Builder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withOrganisationName(final String organisationName) {
            this.organisationName = organisationName;
            return this;
        }

        public Builder withLegalEntityName(final String legalEntityName) {
            this.legalEntityName = legalEntityName;
            return this;
        }

        public Builder withLegalEntityId(final UUID legalEntityId) {
            this.legalEntityId = legalEntityId;
            return this;
        }

        public Builder withSpecificRequirements(final String specificRequirements) {
            this.specificRequirements = specificRequirements;
            return this;
        }

        public Builder withCourtCentreId(final UUID courtCentreId) {
            this.courtCentreId = courtCentreId;
            return this;
        }

        public Builder withPncId(final String pncId) {
            this.pncId = pncId;
            return this;
        }

        public Builder withIsYouth(final Boolean isYouth) {
            this.isYouth = isYouth;
            return this;
        }


        public Builder withAliases(final List<DefendantAlias> aliases) {
            this.aliases = aliases;
            return this;
          }

        public Builder withRestrictFromCourtList(final Boolean restrictFromCourtList ) {
            this.restrictFromCourtList = restrictFromCourtList;
            return this;
        }
        public UpdatedDefendantData build() {
            return new UpdatedDefendantData(bailStatus, custodyTimeLimit, dateOfBirth, firstName, defendantId, lastName, organisationName,
            								legalEntityName, legalEntityId, specificRequirements, courtCentreId, pncId, aliases, restrictFromCourtList, isYouth);
        }
    }
}
