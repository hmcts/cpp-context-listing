package uk.gov.moj.cpp.listing.persistence.entity;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class DefendantBuilder {
    private CompositeDefendantId compositeDefendantId;
    private UUID personId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String bailStatus;
    private LocalDate custodyTimeLimit;
    private String defenceOrganisation;
    private Hearing hearing;
    private Set<Offence> offences;

    public DefendantBuilder setCompositeDefendantId(CompositeDefendantId compositeDefendantId) {
        this.compositeDefendantId = compositeDefendantId;
        return this;
    }

    public DefendantBuilder setPersonId(UUID personId) {
        this.personId = personId;
        return this;
    }

    public DefendantBuilder setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public DefendantBuilder setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public DefendantBuilder setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public DefendantBuilder setBailStatus(String bailStatus) {
        this.bailStatus = bailStatus;
        return this;
    }

    public DefendantBuilder setCustodyTimeLimit(LocalDate custodyTimeLimit) {
        this.custodyTimeLimit = custodyTimeLimit;
        return this;
    }

    public DefendantBuilder setDefenceOrganisation(String defenceOrganisation) {
        this.defenceOrganisation = defenceOrganisation;
        return this;
    }

    public DefendantBuilder setHearing(Hearing hearing) {
        this.hearing = hearing;
        return this;
    }

    public DefendantBuilder setOffences(Set<Offence> offences) {
        this.offences = offences;
        return this;
    }

    public Defendant build() {
        return new Defendant(compositeDefendantId, personId, firstName, lastName, dateOfBirth, bailStatus, custodyTimeLimit, defenceOrganisation, hearing, offences);
    }
}