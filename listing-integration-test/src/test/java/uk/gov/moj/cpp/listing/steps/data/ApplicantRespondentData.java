package uk.gov.moj.cpp.listing.steps.data;

public class ApplicantRespondentData {
    private final String firstName;

    private final Boolean respondent;

    private final String lastName;

    public ApplicantRespondentData(String firstName, Boolean respondent, String lastName) {
        this.firstName = firstName;
        this.respondent = respondent;
        this.lastName = lastName;
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
}
