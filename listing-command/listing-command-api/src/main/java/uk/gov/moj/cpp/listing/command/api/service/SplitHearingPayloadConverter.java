package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Objects.isNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Converts listing's split-hearing payload into progression's list-new-hearing shape
 * ({@code {listNewHearing: CourtHearingRequest, sendNotificationToParties}}).
 *
 * <p>Deliberately JSON-to-JSON. The proxy performs no enrichment, touches no aggregate and emits no
 * events, so there is nothing to gain from deserialising into either side's generated types — and
 * doing so would couple listing's build to progression's contract for a payload it only forwards.
 *
 * <p>The one field that must be reshaped rather than copied is the virtual block descriptor:
 * listing carries booked sessions as {@code nonDefaultDays[virtual=true]}, but progression's list
 * path reads {@code courtScheduleId} only from {@code bookedSlots}/{@code hearingDays} and never
 * from {@code nonDefaultDays}, so a straight pass-through would silently drop the booking.
 */
public final class SplitHearingPayloadConverter {

    private static final String VIRTUAL = "virtual";
    private static final String DURATION = "duration";
    private static final String START_TIME = "startTime";
    private static final String NON_DEFAULT_DAYS = "nonDefaultDays";
    private static final String BOOKED_SLOTS = "bookedSlots";
    private static final String SEND_NOTIFICATION_TO_PARTIES = "sendNotificationToParties";
    private static final String LIST_NEW_HEARING = "listNewHearing";

    // Copied field-for-field from a virtual nonDefaultDay onto a bookedSlot; `virtual` is dropped.
    private static final List<String> BOOKED_SLOT_FIELDS = List.of(
            START_TIME, DURATION, "courtScheduleId", "session", "oucode",
            "courtRoomId", "courtCentreId", "roomId");

    private static final List<String> PASS_THROUGH_FIELDS = List.of(
            "jurisdictionType", "judiciary", "priority", "bookingType", "specialRequirements");

    private SplitHearingPayloadConverter() {
    }

    public static JsonObject toProgressionSplitRequest(final JsonObject splitHearing,
                                                       final String courtCentreName,
                                                       final String courtRoomName,
                                                       final Integer hearingTypeDefaultMinutes) {
        final JsonArray bookedSlots = bookedSlots(splitHearing);

        final JsonObjectBuilder listNewHearing = createObjectBuilder();

        copyIfPresent(splitHearing, "type", listNewHearing, "hearingType");
        PASS_THROUGH_FIELDS.forEach(field -> copyIfPresent(splitHearing, field, listNewHearing, field));

        if (!bookedSlots.isEmpty()) {
            listNewHearing.add(BOOKED_SLOTS, bookedSlots);
        }
        listNewHearing.add("estimatedMinutes", estimatedMinutes(bookedSlots, hearingTypeDefaultMinutes));

        earliestStartDateTime(splitHearing, bookedSlots)
                .ifPresent(value -> listNewHearing.add("earliestStartDateTime", value));
        copyIfPresent(splitHearing, "endDate", listNewHearing, "endDate");

        weekCommencingDate(splitHearing).ifPresent(value -> listNewHearing.add("weekCommencingDate", value));

        listNewHearing.add("courtCentre", courtCentre(splitHearing, courtCentreName, courtRoomName));

        final JsonArray realNonDefaultDays = realNonDefaultDays(splitHearing);
        if (!realNonDefaultDays.isEmpty()) {
            listNewHearing.add(NON_DEFAULT_DAYS, realNonDefaultDays);
        }

        listNewHearing.add("listDefendantRequests", listDefendantRequests(splitHearing));

        final JsonObjectBuilder request = createObjectBuilder().add(LIST_NEW_HEARING, listNewHearing);
        if (splitHearing.containsKey(SEND_NOTIFICATION_TO_PARTIES)
                && !splitHearing.isNull(SEND_NOTIFICATION_TO_PARTIES)) {
            request.add(SEND_NOTIFICATION_TO_PARTIES, splitHearing.getBoolean(SEND_NOTIFICATION_TO_PARTIES));
        }
        return request.build();
    }

    private static JsonArray bookedSlots(final JsonObject splitHearing) {
        final JsonArrayBuilder slots = createArrayBuilder();
        for (final JsonObject day : nonDefaultDays(splitHearing)) {
            if (!isVirtual(day)) {
                continue;
            }
            final JsonObjectBuilder slot = createObjectBuilder();
            BOOKED_SLOT_FIELDS.forEach(field -> copyIfPresent(day, field, slot, field));
            slots.add(slot);
        }
        return slots.build();
    }

    private static JsonArray realNonDefaultDays(final JsonObject splitHearing) {
        final JsonArrayBuilder days = createArrayBuilder();
        nonDefaultDays(splitHearing).stream()
                .filter(day -> !isVirtual(day))
                .forEach(days::add);
        return days.build();
    }

    private static List<JsonObject> nonDefaultDays(final JsonObject splitHearing) {
        final List<JsonObject> days = new ArrayList<>();
        if (splitHearing.containsKey(NON_DEFAULT_DAYS) && !splitHearing.isNull(NON_DEFAULT_DAYS)) {
            splitHearing.getJsonArray(NON_DEFAULT_DAYS).getValuesAs(JsonObject.class).forEach(days::add);
        }
        return days;
    }

    private static boolean isVirtual(final JsonObject day) {
        return day.containsKey(VIRTUAL) && !day.isNull(VIRTUAL) && day.getBoolean(VIRTUAL);
    }

    private static int estimatedMinutes(final JsonArray bookedSlots, final Integer hearingTypeDefaultMinutes) {
        if (bookedSlots.isEmpty()) {
            return isNull(hearingTypeDefaultMinutes) ? 0 : hearingTypeDefaultMinutes;
        }
        return bookedSlots.getValuesAs(JsonObject.class).stream()
                .filter(slot -> slot.containsKey(DURATION) && !slot.isNull(DURATION))
                .mapToInt(slot -> slot.getInt(DURATION))
                .sum();
    }

    // The hearing starts when its first booked session starts; the date alone is not enough for
    // progression, which wants a full timestamp.
    private static java.util.Optional<JsonValue> earliestStartDateTime(final JsonObject splitHearing,
                                                                      final JsonArray bookedSlots) {
        return bookedSlots.getValuesAs(JsonObject.class).stream()
                .filter(slot -> slot.containsKey(START_TIME) && !slot.isNull(START_TIME))
                .map(slot -> slot.get(START_TIME))
                .findFirst();
    }

    private static java.util.Optional<JsonValue> weekCommencingDate(final JsonObject splitHearing) {
        if (!splitHearing.containsKey("weekCommencingStartDate") || splitHearing.isNull("weekCommencingStartDate")) {
            return java.util.Optional.empty();
        }
        final JsonObjectBuilder weekCommencing = createObjectBuilder()
                .add("startDate", splitHearing.get("weekCommencingStartDate"));
        if (splitHearing.containsKey("weekCommencingDurationInWeeks")
                && !splitHearing.isNull("weekCommencingDurationInWeeks")) {
            weekCommencing.add(DURATION, splitHearing.get("weekCommencingDurationInWeeks"));
        }
        return java.util.Optional.of(weekCommencing.build());
    }

    private static JsonObject courtCentre(final JsonObject splitHearing,
                                          final String courtCentreName,
                                          final String courtRoomName) {
        final JsonObjectBuilder courtCentre = createObjectBuilder();
        copyIfPresent(splitHearing, "courtCentreId", courtCentre, "id");
        if (!isNull(courtCentreName)) {
            courtCentre.add("name", courtCentreName);
        }
        copyIfPresent(splitHearing, "courtRoomId", courtCentre, "roomId");
        if (!isNull(courtRoomName)) {
            courtCentre.add("roomName", courtRoomName);
        }
        return courtCentre.build();
    }

    // prosecutionCases[].defendants[].offences[] flattens to one entry per defendant, with the
    // offence ids that move.
    private static JsonArray listDefendantRequests(final JsonObject splitHearing) {
        final JsonArrayBuilder requests = createArrayBuilder();
        if (!splitHearing.containsKey("prosecutionCases") || splitHearing.isNull("prosecutionCases")) {
            return requests.build();
        }
        for (final JsonObject prosecutionCase : splitHearing.getJsonArray("prosecutionCases").getValuesAs(JsonObject.class)) {
            if (!prosecutionCase.containsKey("defendants") || prosecutionCase.isNull("defendants")) {
                continue;
            }
            for (final JsonObject defendant : prosecutionCase.getJsonArray("defendants").getValuesAs(JsonObject.class)) {
                final JsonObjectBuilder request = createObjectBuilder();
                copyIfPresent(prosecutionCase, "caseId", request, "prosecutionCaseId");
                copyIfPresent(defendant, "defendantId", request, "defendantId");
                request.add("defendantOffences", defendantOffences(defendant));
                requests.add(request);
            }
        }
        return requests.build();
    }

    private static JsonArray defendantOffences(final JsonObject defendant) {
        final JsonArrayBuilder offenceIds = createArrayBuilder();
        if (defendant.containsKey("offences") && !defendant.isNull("offences")) {
            defendant.getJsonArray("offences").getValuesAs(JsonObject.class).stream()
                    .filter(offence -> offence.containsKey("offenceId") && !offence.isNull("offenceId"))
                    .forEach(offence -> offenceIds.add(offence.get("offenceId")));
        }
        return offenceIds.build();
    }

    private static void copyIfPresent(final JsonObject source, final String sourceField,
                                      final JsonObjectBuilder target, final String targetField) {
        if (source.containsKey(sourceField) && !source.isNull(sourceField)) {
            target.add(targetField, source.get(sourceField));
        }
    }
}
