package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class CourtApplicationDetails {

    private String applicationReference;

    private JsonObject applicant;

    private JsonArray respondents;

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(final String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public JsonObject getApplicant() {
        return applicant;
    }

    public void setApplicant(final JsonObject applicant) {
        this.applicant = applicant;
    }

    public JsonArray getRespondents() {
        return respondents;
    }

    public void setRespondents(final JsonArray respondents) {
        this.respondents = respondents;
    }
}
