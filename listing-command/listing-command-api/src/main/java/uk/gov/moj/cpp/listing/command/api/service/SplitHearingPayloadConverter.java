package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Objects.isNull;
import static java.util.Comparator.comparing;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import java.time.ZonedDateTime;
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
    private static final String WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    private static final String WEEK_COMMENCING_DURATION_IN_WEEKS = "weekCommencingDurationInWeeks";
    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String DEFENDANTS = "defendants";
    private static final String OFFENCES = "offences";
    private static final String OFFENCE_ID = "offenceId";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";

    // Copied field-for-field from a virtual nonDefaultDay onto a bookedSlot; `virtual` is dropped.
    private static final List<String> BOOKED_SLOT_FIELDS = List.of(
            START_TIME, DURATION, COURT_SCHEDULE_ID, "session", "oucode",
            "courtRoomId", COURT_CENTRE_ID, "roomId");

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
        // Empty arrays are dropped rather than forwarded: progression's courtHearingRequest requires
        // at least one entry wherever these appear, so passing the front end's empty judiciary on a
        // split — which carries no judiciary yet — makes progression reject the whole request.
        PASS_THROUGH_FIELDS.forEach(field -> copyIfPresentAndNotEmpty(splitHearing, field, listNewHearing, field));

        if (!bookedSlots.isEmpty()) {
            listNewHearing.add(BOOKED_SLOTS, bookedSlots);
        }
        final JsonArray virtualDays = virtualDays(splitHearing);
        listNewHearing.add("estimatedMinutes", estimatedMinutes(virtualDays, hearingTypeDefaultMinutes));

        earliestStartDateTime(virtualDays)
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
            if (!isVirtual(day) || !isBooked(day)) {
                continue;
            }
            final JsonObjectBuilder slot = createObjectBuilder();
            BOOKED_SLOT_FIELDS.forEach(field -> copyIfPresent(day, field, slot, field));
            // Listing reads courtCentreId off every booked slot unguarded when it turns them into
            // hearing days, so a slot without one fails the onward listing of the new hearing well
            // after the split was accepted. The front end sets it per day only when a day sits
            // somewhere other than the hearing's own centre, so fall back to the split's centre.
            if (!day.containsKey(COURT_CENTRE_ID) || day.isNull(COURT_CENTRE_ID)) {
                copyIfPresent(splitHearing, COURT_CENTRE_ID, slot, COURT_CENTRE_ID);
            }
            slots.add(slot);
        }
        return slots.build();
    }

    /**
     * The days progression receives as {@code nonDefaultDays}: every genuinely non-virtual day, plus
     * any virtual day that names no session. A virtual day carries a booking only when it has a
     * {@code courtScheduleId} - without one there is nothing to reshape onto a booked slot, and
     * emitting it as one produces a slot whose id is null, which listing then dereferences unguarded
     * when it converts slots to hearing days. Left here, the day keeps its duration and start time
     * and the crown search-and-book branch finds the session instead.
     */
    private static JsonArray realNonDefaultDays(final JsonObject splitHearing) {
        final JsonArrayBuilder days = createArrayBuilder();
        nonDefaultDays(splitHearing).stream()
                .filter(day -> !isVirtual(day) || !isBooked(day))
                .map(SplitHearingPayloadConverter::withoutVirtualFlag)
                .forEach(days::add);
        return days.build();
    }

    /**
     * {@code virtual} is listing's own marker for a day that stands in for a booked session; it is
     * not part of progression's nonDefaultDay shape, whose schema rejects unknown keys. The booked
     * slot path drops it for the same reason.
     */
    private static JsonObject withoutVirtualFlag(final JsonObject day) {
        if (!day.containsKey(VIRTUAL)) {
            return day;
        }
        final JsonObjectBuilder stripped = createObjectBuilder();
        day.entrySet().stream()
                .filter(entry -> !VIRTUAL.equals(entry.getKey()))
                .forEach(entry -> stripped.add(entry.getKey(), entry.getValue()));
        return stripped.build();
    }

    private static boolean isBooked(final JsonObject day) {
        return day.containsKey(COURT_SCHEDULE_ID) && !day.isNull(COURT_SCHEDULE_ID);
    }

    /** Every virtual day, booked or not - what the new hearing is asked to sit for. */
    private static JsonArray virtualDays(final JsonObject splitHearing) {
        final JsonArrayBuilder days = createArrayBuilder();
        nonDefaultDays(splitHearing).stream()
                .filter(SplitHearingPayloadConverter::isVirtual)
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

    private static int estimatedMinutes(final JsonArray days, final Integer hearingTypeDefaultMinutes) {
        if (days.isEmpty()) {
            return isNull(hearingTypeDefaultMinutes) ? 0 : hearingTypeDefaultMinutes;
        }
        return days.getValuesAs(JsonObject.class).stream()
                .filter(slot -> slot.containsKey(DURATION) && !slot.isNull(DURATION))
                .mapToInt(slot -> slot.getInt(DURATION))
                .sum();
    }

    // The hearing starts when its first booked session starts; the date alone is not enough for
    // progression, which wants a full timestamp.
    /**
     * The earliest slot's start, by time rather than by position. A split can spread the new hearing
     * over several non-consecutive days, and nothing requires the front end to send them in order -
     * taking the first of the array would then start the hearing on the wrong day.
     */
    private static java.util.Optional<JsonValue> earliestStartDateTime(final JsonArray days) {
        return days.getValuesAs(JsonObject.class).stream()
                .filter(slot -> slot.containsKey(START_TIME) && !slot.isNull(START_TIME))
                .min(comparing(slot -> ZonedDateTime.parse(slot.getString(START_TIME)).toInstant()))
                .map(slot -> slot.get(START_TIME));
    }

    private static java.util.Optional<JsonValue> weekCommencingDate(final JsonObject splitHearing) {
        if (!splitHearing.containsKey(WEEK_COMMENCING_START_DATE) || splitHearing.isNull(WEEK_COMMENCING_START_DATE)) {
            return java.util.Optional.empty();
        }
        final JsonObjectBuilder weekCommencing = createObjectBuilder()
                .add("startDate", splitHearing.get(WEEK_COMMENCING_START_DATE));
        if (splitHearing.containsKey(WEEK_COMMENCING_DURATION_IN_WEEKS)
                && !splitHearing.isNull(WEEK_COMMENCING_DURATION_IN_WEEKS)) {
            weekCommencing.add(DURATION, splitHearing.get(WEEK_COMMENCING_DURATION_IN_WEEKS));
        }
        return java.util.Optional.of(weekCommencing.build());
    }

    private static JsonObject courtCentre(final JsonObject splitHearing,
                                          final String courtCentreName,
                                          final String courtRoomName) {
        final JsonObjectBuilder courtCentre = createObjectBuilder();
        copyIfPresent(splitHearing, COURT_CENTRE_ID, courtCentre, "id");
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
        if (!splitHearing.containsKey(PROSECUTION_CASES) || splitHearing.isNull(PROSECUTION_CASES)) {
            return requests.build();
        }
        for (final JsonObject prosecutionCase : splitHearing.getJsonArray(PROSECUTION_CASES).getValuesAs(JsonObject.class)) {
            if (!prosecutionCase.containsKey(DEFENDANTS) || prosecutionCase.isNull(DEFENDANTS)) {
                continue;
            }
            for (final JsonObject defendant : prosecutionCase.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class)) {
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
        if (defendant.containsKey(OFFENCES) && !defendant.isNull(OFFENCES)) {
            defendant.getJsonArray(OFFENCES).getValuesAs(JsonObject.class).stream()
                    .filter(offence -> offence.containsKey(OFFENCE_ID) && !offence.isNull(OFFENCE_ID))
                    .forEach(offence -> offenceIds.add(offence.get(OFFENCE_ID)));
        }
        return offenceIds.build();
    }

    private static void copyIfPresentAndNotEmpty(final JsonObject source, final String sourceField,
                                                 final JsonObjectBuilder target, final String targetField) {
        if (source.containsKey(sourceField) && !source.isNull(sourceField)) {
            final JsonValue value = source.get(sourceField);
            if (value.getValueType() == JsonValue.ValueType.ARRAY && ((JsonArray) value).isEmpty()) {
                return;
            }
            target.add(targetField, value);
        }
    }

    private static void copyIfPresent(final JsonObject source, final String sourceField,
                                      final JsonObjectBuilder target, final String targetField) {
        if (source.containsKey(sourceField) && !source.isNull(sourceField)) {
            target.add(targetField, source.get(sourceField));
        }
    }
}
