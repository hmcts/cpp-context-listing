package uk.gov.moj.cpp.listing.query.view.courtlist;

import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonPropertyUtils.getOptionalUUID;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FlatHearingsConverter {

    private FlatHearingsConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static List<FlatHearing> generateFlatHearingList(final JsonArray caseHearingsArray) {

        final List<FlatHearing> flatHearings = new ArrayList<>();

        for (final JsonObject caseHearings : caseHearingsArray.getValuesAs(JsonObject.class)) {
            if (isWeekCommencing(caseHearings)) {
                flatHearings.add(getFlatHearingForWeekCommencingCaseHearing(caseHearings));
            } else {
                flatHearings.addAll(getFlatHearingsForFixedDateCaseHearings(caseHearings));
            }
        }

        return flatHearings;
    }

    private static boolean isWeekCommencing(final JsonObject caseHearings) {
        return caseHearings.containsKey("weekCommencingStartDate");
    }

    private static FlatHearing getFlatHearingForWeekCommencingCaseHearing(final JsonObject caseHearings) {

        return new FlatHearing(LocalDate.parse(caseHearings.getString("weekCommencingStartDate")),
                caseHearings.getJsonArray("judiciary"),
                getOptionalUUID(caseHearings, "courtRoomId"),
                caseHearings);
    }

    private static List<FlatHearing> getFlatHearingsForFixedDateCaseHearings(JsonObject caseHearings) {
        final List<FlatHearing> flatHearings = new ArrayList<>();

        for (final JsonObject hearingDay : caseHearings.getJsonArray("hearingDays").getValuesAs(JsonObject.class)) {

            flatHearings.add(new FlatHearing(LocalDate.parse(hearingDay.getString("hearingDate")),
                    caseHearings.getJsonArray("judiciary"),
                    getOptionalUUID(caseHearings, "courtRoomId"),
                    caseHearings
            ));
        }

        return flatHearings;
    }
}
