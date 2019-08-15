package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class ApplicantRespondentData {

    private final UUID id ;

    private final String firstName;

    private final Boolean respondent;

    private final String lastName;

    private final CourtApplicationPartyType courtApplicationPartyType;

    public ApplicantRespondentData(final UUID id, String firstName, Boolean respondent, String lastName, final CourtApplicationPartyType courtApplicationPartyType) {
        this.id = id;
        this.firstName = firstName;
        this.respondent = respondent;
        this.lastName = lastName;
        this.courtApplicationPartyType = courtApplicationPartyType;
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
}
