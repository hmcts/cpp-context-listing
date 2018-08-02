package uk.gov.moj.cpp.listing.persistence.entity;

import java.time.LocalDate;
import java.util.UUID;

public class SimpleDefendantBuilder {
    private CompositeDefendantId compositeDefendantId;
    private UUID personId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String bailStatus;
    private LocalDate custodyTimeLimit;
    private String defenceOrganisation;

    public SimpleDefendantBuilder setCompositeDefendantId(CompositeDefendantId compositeDefendantId) {
        this.compositeDefendantId = compositeDefendantId;
        return this;
    }

    public SimpleDefendantBuilder setPersonId(UUID personId) {
        this.personId = personId;
        return this;
    }

    public SimpleDefendantBuilder setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public SimpleDefendantBuilder setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public SimpleDefendantBuilder setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public SimpleDefendantBuilder setBailStatus(String bailStatus) {
        this.bailStatus = bailStatus;
        return this;
    }

    public SimpleDefendantBuilder setCustodyTimeLimit(LocalDate custodyTimeLimit) {
        this.custodyTimeLimit = custodyTimeLimit;
        return this;
    }

    public SimpleDefendantBuilder setDefenceOrganisation(String defenceOrganisation) {
        this.defenceOrganisation = defenceOrganisation;
        return this;
    }

    public SimpleDefendant build() {
        return new SimpleDefendant(compositeDefendantId, personId, firstName, lastName, dateOfBirth, bailStatus, custodyTimeLimit, defenceOrganisation);
    }
}