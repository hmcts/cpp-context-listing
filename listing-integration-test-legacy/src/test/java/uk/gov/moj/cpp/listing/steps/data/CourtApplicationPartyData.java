package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.moj.cpp.listing.domain.Address;

import java.util.UUID;

public class CourtApplicationPartyData {

    private final UUID id;

    private final String firstName;

    private final Boolean respondent;

    private final String lastName;

    private final CourtApplicationPartyType courtApplicationPartyType;

    private final LegalEntityDefendantData legalEntityDefendant;

    private final Address address;

    private final UUID masterDefendantId;

    private final java.time.LocalDate dateOfBirth;

    public CourtApplicationPartyData(final UUID id, String firstName, Boolean respondent, String lastName, final CourtApplicationPartyType courtApplicationPartyType, final LegalEntityDefendantData legalEntityDefendant, final Address address) {
        this(id, firstName, respondent, lastName, courtApplicationPartyType, legalEntityDefendant, address, null, null);
    }

    public CourtApplicationPartyData(final UUID id, String firstName, Boolean respondent, String lastName, final CourtApplicationPartyType courtApplicationPartyType, final LegalEntityDefendantData legalEntityDefendant, final Address address, final UUID masterDefendantId, final java.time.LocalDate dateOfBirth) {
        this.id = id;
        this.firstName = firstName;
        this.respondent = respondent;
        this.lastName = lastName;
        this.courtApplicationPartyType = courtApplicationPartyType;
        this.legalEntityDefendant = legalEntityDefendant;
        this.address = address;
        this.masterDefendantId = masterDefendantId;
        this.dateOfBirth = dateOfBirth;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Boolean getRespondent() {
        return respondent;
    }

    public CourtApplicationPartyType getCourtApplicationPartyType() {
        return courtApplicationPartyType;
    }

    public UUID getId() {
        return id;
    }

    public LegalEntityDefendantData getLegalEntityDefendant() {
        return legalEntityDefendant;
    }

    public Address getAddress() {
        return address;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public java.time.LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
}
