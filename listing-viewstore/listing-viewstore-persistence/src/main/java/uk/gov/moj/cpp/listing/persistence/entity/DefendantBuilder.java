package uk.gov.moj.cpp.listing.persistence.entity;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class DefendantBuilder {
    private UUID listingDefendantId;
    private UUID defendantId;
    private UUID personId;
    private String firstName;
    private String lastName;
    private String bailStatus;
    private String defenceOrganisation;
    private LocalDate dateOfBirth;

    private Set<Offence> offences;

    private Hearing hearing;

    public DefendantBuilder setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
        return this;
    }

    public DefendantBuilder setListingDefendantId(final UUID listingDefendantId) {
        this.listingDefendantId = listingDefendantId;
        return this;
    }

    public DefendantBuilder setPersonId(final UUID personId) {
        this.personId = personId;
        return this;
    }

    public DefendantBuilder setFirstName(final String firstName) {
        this.firstName = firstName;
        return this;
    }

    public DefendantBuilder setLastName(final String lastName) {
        this.lastName = lastName;
        return this;
    }

    public DefendantBuilder setBailStatus(final String bailStatus) {
        this.bailStatus = bailStatus;
        return this;
    }

    public void setHearing(Hearing hearing) {
        this.hearing = hearing;
    }

    public DefendantBuilder setDefenceOrganisation(final String defenceOrganisation) {
        this.defenceOrganisation = defenceOrganisation;
        return this;
    }

    public DefendantBuilder setDateOfBirth(final LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public DefendantBuilder setOffences(final Set<Offence> offences) {
        this.offences = offences;
        return this;
    }

    public Defendant build() {
        return new Defendant(listingDefendantId, defendantId, personId, firstName, lastName, bailStatus, defenceOrganisation, dateOfBirth, offences, hearing);
    }
}