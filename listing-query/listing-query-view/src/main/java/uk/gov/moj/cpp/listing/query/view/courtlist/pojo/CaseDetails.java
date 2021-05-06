package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class CaseDetails {

    private JsonObject caseIdentifier;

    private JsonArray defendants;

    private JsonObject prosecutor;

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

    public JsonObject getProsecutor() {
        return prosecutor;
    }

    public void setProsecutor(final JsonObject prosecutor) {
        this.prosecutor = prosecutor;
    }
}
