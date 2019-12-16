package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class CaseDetails {

    private JsonObject caseIdentifier;

    private JsonArray defendants;

    public JsonObject getCaseIdentifier() {
        return caseIdentifier;
    }

    public void setCaseIdentifier(final JsonObject caseIdentifier) {
        this.caseIdentifier = caseIdentifier;
    }

    public JsonArray getDefendants() {
        return defendants;
    }

    public void setDefendants(final JsonArray defendants) {
        this.defendants = defendants;
    }
}
