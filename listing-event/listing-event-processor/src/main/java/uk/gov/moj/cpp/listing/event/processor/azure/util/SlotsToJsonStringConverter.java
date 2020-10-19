package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.toIsoString;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.HearingDayDetailConverter.getHearingDayDetails;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;
import uk.gov.moj.cpp.listing.event.processor.azure.builder.SlotDetailBuilder;
import uk.gov.moj.cpp.listing.event.processor.azure.data.HearingDayDetail;
import uk.gov.moj.cpp.listing.event.processor.azure.data.SlotDetail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
public class SlotsToJsonStringConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlotsToJsonStringConverter.class);

    @Inject
    private ListingReferenceDataService listingReferenceDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    public String getSlotDetailFromHearingConfirmed(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing, final boolean isForAdjournmentHearing) {

        final CourtCentre courtCentre = confirmedHearing.getCourtCentre();

        final UUID roomId;

        if (courtCentre.getRoomId().isPresent()) {
            roomId = courtCentre.getRoomId().get();
        } else {
            throw new IllegalArgumentException(format("No room id specified %s to lookup court room number", courtCentre.getRoomId().get()));
        }

        final Map<UUID, JsonEnvelope> courtRoomPayloadMap = new HashMap<>();
        courtRoomPayloadMap.put(courtCentre.getId(), listingReferenceDataService.getPayLoadForCourtRoom(jsonEnvelope, courtCentre.getId().toString()));

        final HearingAllocatedForListing allocatedForListing = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingAllocatedForListing.class);
        final List<HearingDay> hearingDays = allocatedForListing.getHearingDays();

        final List<HearingDayDetail> hearingDayDetails = getHearingDayDetails(hearingDays, isForAdjournmentHearing);

        if (hearingDayDetails.isEmpty()) {
            return StringUtils.EMPTY;
        }

        final String hearingId = confirmedHearing.getId().toString();
        final Optional<String> bookingId = jsonEnvelope.payloadAsJsonObject().keySet().contains("bookingId") ? Optional.of(jsonEnvelope.payloadAsJsonObject().getString("bookingId")) : empty();

        hearingDayDetails.stream().filter(hearingDay -> hearingDay.getCourtCentreId().isPresent()).forEach(hearingDay -> {
            final UUID courtCentreId = UUID.fromString(hearingDay.getCourtCentreId().get());
            if (courtRoomPayloadMap.get(courtCentreId) == null) {
                final JsonEnvelope payLoadForCourtRoom = listingReferenceDataService.getPayLoadForCourtRoom(jsonEnvelope, courtCentreId.toString());
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(format("Court Room Payload = %s looked up with Court Centre Id %s", payLoadForCourtRoom, courtCentreId.toString()));
                }
                courtRoomPayloadMap.put(courtCentreId, payLoadForCourtRoom);
            }
        });

        return toJSONString(hearingDayDetails.stream()
                .map(hearingDayDetail -> {
                    final Predicate<HearingDayDetail> defaultCourtCentre = getHearingDayDetailPredicate();
                    final UUID courtCentreId = defaultCourtCentre.test(hearingDayDetail) ? courtCentre.getId() : hearingDayDetail.getCourtCentreId().map(UUID::fromString).orElse(null);
                    final UUID courtRoomUUID = defaultCourtCentre.test(hearingDayDetail) ? roomId : hearingDayDetail.getCourtRoomId().map(UUID::fromString).orElse(null);
                    if (courtCentreId == null) {
                        return null;
                    }
                    final JsonEnvelope payLoadForCourtRoom = courtRoomPayloadMap.get(courtCentreId);
                    final int courtRoomId = listingReferenceDataService.retrieveCourtRoomId(payLoadForCourtRoom.payloadAsJsonObject(), courtRoomUUID, courtCentreId);
                    final String ouCode = payLoadForCourtRoom.payloadAsJsonObject().getString("oucode");

                    return retrieveSlotDetail(hearingDayDetail, ouCode, courtRoomId, hearingId, bookingId);
                })
                .filter(Objects::nonNull)
                .collect(toList()));
    }

    private Predicate<HearingDayDetail> getHearingDayDetailPredicate() {
        return dayDetail -> !dayDetail.getCourtRoomId().isPresent() && !dayDetail.getCourtCentreId().isPresent();
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
                                                 final Optional<String> bookingId) {

        final SlotDetailBuilder slotDetailBuilder = SlotDetailBuilder.slotDetail()
                .withOuCode(ouCode)
                .withCourtRoomId(courtRoomId)
                .withHearingId(hearingId)
                .withSessionDate(hearingDayDetail.getDate())
                .withSession(hearingDayDetail.getTime())
                .withDuration(hearingDayDetail.getDuration())
                .withHearingStartTime(hearingDayDetail.getHearingStartTime());

        bookingId.ifPresent(slotDetailBuilder::withBookingId);
        hearingDayDetail.getCourtScheduleId().ifPresent(slotDetailBuilder::withCourtScheduleId);

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
        builder.withHearingStartTime(toIsoString(nonDefaultDay.getStartTime()));

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
