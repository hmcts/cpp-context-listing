package uk.gov.moj.cpp.listing.common.util;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.UTC;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.toIsoString;

import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.RequestedCourtSchedule;
import uk.gov.moj.cpp.listing.domain.SlotDetail;
import uk.gov.moj.cpp.listing.domain.builder.SlotDetailBuilder;
import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

@SuppressWarnings({"squid:S3655"})
public class SlotsToJsonStringConverter {

    private static final String BOOKING_ID = "bookingId";
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    public JsonArrayBuilder convertNonDefaultDaysToJson(final UUID hearingId, final List<NonDefaultDay> nonDefaultDays) {

        final List<SlotDetail> slots = nonDefaultDays.stream().
                map(nonDefaultDay -> buildSlotDetailFromNonDefaultDay(hearingId, nonDefaultDay))
                .toList();

        return buildArrayBuilder(slots);
    }

    public JsonArrayBuilder convertNonDefaultDaysToListUpdateJson(final UUID hearingId, final List<uk.gov.justice.core.courts.NonDefaultDay> nonDefaultDays) {

        final List<RequestedCourtSchedule> requestedCourtScheduleList = nonDefaultDays.stream().
                map(SlotsToJsonStringConverter::buildSlotDetailFromNonDefaultDay)
                .toList();
        JsonObjectBuilder jsonObjectBuilder = createObjectBuilder().add("courtScheduleIds", buildJsonArrayBuilder(requestedCourtScheduleList))
                .add("hearingId", hearingId.toString());
        return createArrayBuilder().add(jsonObjectBuilder.build());
    }

    public JsonArray convertHearingDaysToCourtScheduleIdsJson(final List<HearingDay> hearingDays) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        hearingDays.forEach(hearingDay -> {
            JsonObjectBuilder objectBuilder = createObjectBuilder();
            objectBuilder.add("courtScheduleId", hearingDay.getCourtScheduleId().toString());
            if (nonNull(hearingDay.getStartTime())) {
                objectBuilder.add("hearingStartTime", toIsoString(hearingDay.getStartTime().withZoneSameInstant(UTC)));
            }
            if (nonNull(hearingDay.getDurationMinutes())) {
                objectBuilder.add("durationInMinutes", hearingDay.getDurationMinutes());
            }
            arrayBuilder.add(objectBuilder.build());
        });

        return arrayBuilder.build();

    }

    private static SlotDetail buildSlotDetailFromNonDefaultDay(final UUID hearingId, final NonDefaultDay nonDefaultDay) {
        final SlotDetailBuilder builder = SlotDetailBuilder.slotDetail()
                .withHearingId(hearingId.toString())
                .withSessionDate(nonDefaultDay.getStartTime().toLocalDate().toString())
                .withHearingStartTime(toIsoString(nonDefaultDay.getStartTime()))
                .withCourtScheduleId(nonDefaultDay.getCourtScheduleId().orElse(null))
                .withDuration(nonDefaultDay.getDuration().orElse(0))
                .withOuCode(nonDefaultDay.getOucode().orElse(null))
                .withCourtRoomId(nonDefaultDay.getCourtRoomId().orElse(0))
                .withSession(nonDefaultDay.getSession().orElse(null));

        return builder.build();
    }

    private static JsonArrayBuilder buildArrayBuilder(final List<SlotDetail> slotDetail) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        slotDetail.forEach(
                slot -> addBuilderParams(slot, arrayBuilder));

        return arrayBuilder;
    }

    private static void addBuilderParams(final SlotDetail slot, final JsonArrayBuilder arrayBuilder) {
        JsonObjectBuilder objectBuilder = createObjectBuilder();
        objectBuilder.add("duration", slot.getDuration());
        objectBuilder.add("courtRoomId", slot.getCourtRoomId());
        if (nonNull(slot.getHearingStartTime())){
            objectBuilder.add("hearingStartTime", slot.getHearingStartTime());
        }
        if (nonNull(slot.getSessionDate())) {
            objectBuilder.add("sessionDate", slot.getSessionDate());
        }
        if (nonNull(slot.getSession())) {
            objectBuilder.add("session", slot.getSession());
        }
        if (nonNull(slot.getOuCode())) {
            objectBuilder.add("ouCode", slot.getOuCode());
        }
        if (nonNull(slot.getHearingId())) {
            objectBuilder.add("hearingId", slot.getHearingId());
        }
        if (nonNull(slot.getBookingId())) {
            objectBuilder.add(BOOKING_ID, slot.getBookingId());
        }
        if (nonNull(slot.getCourtScheduleId())) {
            objectBuilder.add("courtScheduleId", slot.getCourtScheduleId());
        }
        arrayBuilder.add(objectBuilder.build());
    }

    private static RequestedCourtSchedule buildSlotDetailFromNonDefaultDay(final uk.gov.justice.core.courts.NonDefaultDay nonDefaultDay) {
        final RequestedCourtSchedule requestedCourtSchedule = new RequestedCourtSchedule();

        requestedCourtSchedule.setCourtScheduleId(nonDefaultDay.getCourtScheduleId());
        requestedCourtSchedule.setHearingStartTime(DateAndTimeUtils.toIsoString(nonDefaultDay.getStartTime()));
        requestedCourtSchedule.setDurationInMinutes(nonDefaultDay.getDuration());

        return requestedCourtSchedule;
    }

    private static JsonArrayBuilder buildJsonArrayBuilder(final List<RequestedCourtSchedule> courtSchedules) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        courtSchedules.forEach(
                slot -> addBuilderParams(slot, arrayBuilder));

        return arrayBuilder;
    }

    private static void addBuilderParams(final RequestedCourtSchedule requestedCourtSchedule, final JsonArrayBuilder arrayBuilder) {
        JsonObjectBuilder objectBuilder = createObjectBuilder();
        objectBuilder.add("courtScheduleId", requestedCourtSchedule.getCourtScheduleId());
        if (nonNull(requestedCourtSchedule.getHearingStartTime())) {
            objectBuilder.add("hearingStartTime", requestedCourtSchedule.getHearingStartTime());
        }
        if (nonNull(requestedCourtSchedule.getDurationInMinutes())) {
            objectBuilder.add("durationInMinutes", requestedCourtSchedule.getDurationInMinutes());
        }
        arrayBuilder.add(objectBuilder.build());
    }
}
