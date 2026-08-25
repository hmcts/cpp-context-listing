package uk.gov.moj.cpp.listing.query.view.courtlist;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Shared by any producer of court list JSON (daily list query, publish-court-list command flow)
 * so that a hearing is dropped wherever a defendant on it has no offences, consistently.
 */
public class HearingOffenceFilter {

    private static final String COURT_LISTS = "courtLists";
    private static final String SITTINGS = "sittings";
    private static final String HEARINGS = "hearings";
    private static final String DEFENDANTS = "defendants";
    private static final String OFFENCES = "offences";

    private HearingOffenceFilter() {
        throw new IllegalStateException("Utility class");
    }

    public static JsonObject removeHearingsWithDefendantMissingOffences(final JsonObject courtListPayload) {
        if (!courtListPayload.containsKey(COURT_LISTS) || courtListPayload.isNull(COURT_LISTS)) {
            return courtListPayload;
        }

        final JsonArrayBuilder courtListsBuilder = createArrayBuilder();
        courtListPayload.getJsonArray(COURT_LISTS).getValuesAs(JsonObject.class)
                .forEach(courtList -> courtListsBuilder.add(filterCourtList(courtList)));

        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        courtListPayload.forEach((key, value) -> payloadBuilder.add(key, COURT_LISTS.equals(key) ? courtListsBuilder.build() : value));
        return payloadBuilder.build();
    }

    private static JsonObject filterCourtList(final JsonObject courtList) {
        if (!courtList.containsKey(SITTINGS) || courtList.isNull(SITTINGS)) {
            return courtList;
        }

        final JsonArrayBuilder sittingsBuilder = createArrayBuilder();
        courtList.getJsonArray(SITTINGS).getValuesAs(JsonObject.class)
                .forEach(sitting -> sittingsBuilder.add(filterSitting(sitting)));

        final JsonObjectBuilder courtListBuilder = createObjectBuilder();
        courtList.forEach((key, value) -> courtListBuilder.add(key, SITTINGS.equals(key) ? sittingsBuilder.build() : value));
        return courtListBuilder.build();
    }

    private static JsonObject filterSitting(final JsonObject sitting) {
        if (!sitting.containsKey(HEARINGS) || sitting.isNull(HEARINGS)) {
            return sitting;
        }

        final JsonArrayBuilder hearingsBuilder = createArrayBuilder();
        sitting.getJsonArray(HEARINGS).getValuesAs(JsonObject.class).stream()
                .filter(HearingOffenceFilter::allDefendantsHaveOffences)
                .forEach(hearingsBuilder::add);

        final JsonObjectBuilder sittingBuilder = createObjectBuilder();
        sitting.forEach((key, value) -> sittingBuilder.add(key, HEARINGS.equals(key) ? hearingsBuilder.build() : value));
        return sittingBuilder.build();
    }

    private static boolean allDefendantsHaveOffences(final JsonObject hearing) {
        if (!hearing.containsKey(DEFENDANTS) || hearing.isNull(DEFENDANTS)) {
            return true;
        }
        return hearing.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).stream()
                .allMatch(defendant -> defendant.containsKey(OFFENCES) && !defendant.isNull(OFFENCES) && !defendant.getJsonArray(OFFENCES).isEmpty());
    }
}
