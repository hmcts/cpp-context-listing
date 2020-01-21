package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.HearingDayDetailConverter.getHearingDayDetails;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.azure.builder.SlotDetailBuilder;
import uk.gov.moj.cpp.listing.event.processor.azure.data.HearingDayDetail;
import uk.gov.moj.cpp.listing.event.processor.azure.data.SlotDetail;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
public class SlotsToJsonStringConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlotsToJsonStringConverter.class);

    @Inject
    private ListingReferenceDataService listingReferenceDataService;

    public String getSlotDetailFromHearingConfirmed(final JsonEnvelope jsonEnvelope, final HearingConfirmed hearingConfirmed) {

        final CourtCentre courtCentre = hearingConfirmed.getConfirmedHearing().getCourtCentre();

        final UUID roomId;
        if (courtCentre.getRoomId().isPresent()) {
            roomId = courtCentre.getRoomId().get();
        } else {
            throw new IllegalArgumentException(format("No room id specified %s to lookup court room number", courtCentre.getRoomId().get()));
        }

        final JsonEnvelope payLoadForCourtRoom = listingReferenceDataService.getPayLoadForCourtRoom(jsonEnvelope, courtCentre.getId().toString());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(format("Court Room Payload = %s looked up with Court Centre Id %s", payLoadForCourtRoom, courtCentre.getId().toString()));
        }

        final int courtRoomId = listingReferenceDataService.retrieveCourtRoomId(payLoadForCourtRoom.payloadAsJsonObject(), roomId, courtCentre.getId());

        final String ouCode = payLoadForCourtRoom.payloadAsJsonObject().getString("oucode");

        final List<HearingDayDetail> hearingDayDetails = getHearingDayDetails(hearingConfirmed.getConfirmedHearing().getHearingDays());
        final String hearingId = hearingConfirmed.getConfirmedHearing().getId().toString();
        final Optional<String> courtScheduleId = empty();

        return toJSONString(hearingDayDetails.stream()
                .map(hearingDayDetail -> retrieveSlotDetail(hearingDayDetail, ouCode, courtRoomId, hearingId, courtScheduleId))
                .collect(toList()));
    }

    public String convertNonDefaultDaysToJson(final UUID hearingId, final List<NonDefaultDay> nonDefaultDays) {

        final List<SlotDetail> slots = nonDefaultDays.stream().
                map(nonDefaultDay -> buildSlotDetailFromNonDefaultDay(hearingId, nonDefaultDay))
                .collect(toList());

        return toJSONString(slots);
    }

    private static SlotDetail retrieveSlotDetail(final HearingDayDetail hearingDayDetail,
                                                 final String ouCode,
                                                 final int courtRoomId,
                                                 final String hearingId,
                                                 final Optional<String> courtScheduleId) {

        final SlotDetailBuilder slotDetailBuilder = SlotDetailBuilder.slotDetail()
                .withOuCode(ouCode)
                .withCourtRoomId(courtRoomId)
                .withHearingId(hearingId)
                .withSessionDate(hearingDayDetail.getDate())
                .withSession(hearingDayDetail.getTime())
                .withDuration(hearingDayDetail.getDuration());

        courtScheduleId.ifPresent(slotDetailBuilder::withCourtScheduleId);

        return slotDetailBuilder.build();
    }

    private static SlotDetail buildSlotDetailFromNonDefaultDay(final UUID hearingId, final NonDefaultDay nonDefaultDay) {
        final SlotDetailBuilder builder = SlotDetailBuilder.slotDetail()
                .withHearingId(hearingId.toString())
                .withSessionDate(nonDefaultDay.getStartTime().toLocalDate().toString());

        nonDefaultDay.getCourtScheduleId().ifPresent(builder::withCourtScheduleId);
        nonDefaultDay.getDuration().ifPresent(builder::withDuration);
        nonDefaultDay.getOucode().ifPresent(builder::withOuCode);
        nonDefaultDay.getCourtRoomId().ifPresent(builder::withCourtRoomId);
        nonDefaultDay.getSession().ifPresent(builder::withSession);

        return builder.build();
    }

    private static String toJSONString(final List<SlotDetail> slotDetail) {

        final JSONArray jsonSlotArray = buildJsonArray(slotDetail);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(jsonSlotArray.toString());
        }

        return jsonSlotArray.toString();
    }

    private static JSONArray buildJsonArray(final List<SlotDetail> slotDetail) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode listNode = objectMapper.valueToTree(slotDetail);

        stripNulls(listNode);

        return new JSONArray(listNode.toString());
    }

    private static void stripNulls(final JsonNode node) {
        final Iterator<JsonNode> it = node.iterator();

        while (it.hasNext()) {
            final JsonNode child = it.next();

            if (child.isNull()) {
                it.remove();
            } else {
                stripNulls(child);
            }
        }
    }
}
