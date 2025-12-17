package uk.gov.moj.cpp.listing.query.view.courtlist;

import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonPropertyUtils.getOptionalUUID;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FlatHearingsConverter {

    public static final String WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    public static final String JUDICIARY = "judiciary";
    public static final String COURT_ROOM_ID = "courtRoomId";

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

    // New method for JudgeListTemplateAssembler
    public static List<FlatHearing> generateFlatHearingListForJudgeList(final JsonArray caseHearingsArray) {
        final List<FlatHearing> flatHearings = new ArrayList<>();

        for (final JsonObject caseHearings : caseHearingsArray.getValuesAs(JsonObject.class)) {
            if (isWeekCommencing(caseHearings)) {
                flatHearings.add(getFlatHearingForWeekCommencingCaseHearingForJudgeList(caseHearings));
            } else {
                flatHearings.addAll(getFlatHearingsForFixedDateCaseHearingsForJudgeList(caseHearings));
            }
        }

        return flatHearings;
    }

    // Original helper methods for RangeSearchConverter
    private static FlatHearing getFlatHearingForWeekCommencingCaseHearing(final JsonObject caseHearings) {
        return new FlatHearing(LocalDate.parse(caseHearings.getString(WEEK_COMMENCING_START_DATE)),
                caseHearings.getJsonArray(JUDICIARY),
                getOptionalUUID(caseHearings, COURT_ROOM_ID),
                caseHearings, true);
    }

    private static List<FlatHearing> getFlatHearingsForFixedDateCaseHearings(final JsonObject caseHearings) {
        final List<FlatHearing> flatHearings = new ArrayList<>();

        for (final JsonObject hearingDay : caseHearings.getJsonArray("hearingDays").getValuesAs(JsonObject.class)) {
            Optional<UUID> courtRoomId = getOptionalUUID(hearingDay, COURT_ROOM_ID);
            if(!courtRoomId.isPresent()) {
                courtRoomId = getOptionalUUID(caseHearings, COURT_ROOM_ID);
            }
            flatHearings.add(new FlatHearing(LocalDate.parse(hearingDay.getString("hearingDate")),
                    caseHearings.getJsonArray(JUDICIARY),
                    courtRoomId,
                    caseHearings,
                    false
            ));
        }

        return flatHearings;
    }

    // New helper methods for JudgeListTemplateAssembler
    private static FlatHearing getFlatHearingForWeekCommencingCaseHearingForJudgeList(final JsonObject caseHearings) {
        return new FlatHearing(LocalDate.parse(caseHearings.getString(WEEK_COMMENCING_START_DATE)),
                caseHearings.getJsonArray(JUDICIARY),
                getOptionalUUID(caseHearings, COURT_ROOM_ID),
                caseHearings, true);
    }

    private static List<FlatHearing> getFlatHearingsForFixedDateCaseHearingsForJudgeList(final JsonObject caseHearings) {
        final List<FlatHearing> flatHearings = new ArrayList<>();

        for (final JsonObject hearingDay : caseHearings.getJsonArray("hearingDays").getValuesAs(JsonObject.class)) {

            flatHearings.add(new FlatHearing(LocalDate.parse(hearingDay.getString("hearingDate")),
                    caseHearings.getJsonArray(JUDICIARY),
                    getOptionalUUID(hearingDay, COURT_ROOM_ID),
                    caseHearings,
                    false
            ));
        }

        return flatHearings;
    }

    private static boolean isWeekCommencing(final JsonObject caseHearings) {
        return caseHearings.containsKey(WEEK_COMMENCING_START_DATE);
    }



}
