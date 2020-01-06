package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import java.util.List;

import javax.json.JsonArray;

public class Sitting {

    private SittingKey sittingKey;
    private JsonArray judiciaryJson;
    private List<Hearing> hearings;
    private boolean weekCommencing;

    public Sitting(final SittingKey sittingKey, final JsonArray judiciaryJson, final List<Hearing> hearings,
                   final boolean weekCommencing) {
        this.sittingKey = sittingKey;
        this.judiciaryJson = judiciaryJson;
        this.hearings = hearings;
        this.weekCommencing = weekCommencing;
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

    public boolean isWeekCommencing() {
        return weekCommencing;
    }
}
