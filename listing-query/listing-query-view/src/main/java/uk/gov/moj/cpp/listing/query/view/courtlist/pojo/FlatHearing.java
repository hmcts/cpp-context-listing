package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FlatHearing {

    private LocalDate hearingDate;
    private JsonArray judiciary;
    private Optional<UUID> courtRoomId;
    private JsonObject caseHearings;
    private boolean weekCommencing;

    public FlatHearing(final LocalDate hearingDate, final JsonArray judiciary,
                       final Optional<UUID> courtRoomId, final JsonObject caseHearings,
                       final boolean weekCommencing) {
        this.hearingDate = hearingDate;
        this.judiciary = judiciary;
        this.courtRoomId = courtRoomId;
        this.caseHearings = caseHearings;
        this.weekCommencing = weekCommencing;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public JsonArray getJudiciary() {
        return judiciary;
    }

    public Optional<UUID> getCourtRoomId() {
        return courtRoomId;
    }

    public JsonObject getCaseHearings() {
        return caseHearings;
    }

    public boolean isWeekCommencing() {
        return weekCommencing;
    }
}
