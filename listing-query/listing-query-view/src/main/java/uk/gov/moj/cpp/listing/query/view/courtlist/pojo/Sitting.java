package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import java.util.List;

import javax.json.JsonArray;

public class Sitting {

    private SittingKey sittingKey;
    private JsonArray judiciaryJson;
    private List<Hearing> hearings;

    public Sitting(final SittingKey sittingKey, final JsonArray judiciaryJson, final List<Hearing> hearings) {
        this.sittingKey = sittingKey;
        this.judiciaryJson = judiciaryJson;
        this.hearings = hearings;
    }

    public SittingKey getSittingKey() {
        return sittingKey;
    }

    public JsonArray getJudiciaryJson() {
        return judiciaryJson;
    }

    public List<Hearing> getHearings() {
        return hearings;
    }
}
