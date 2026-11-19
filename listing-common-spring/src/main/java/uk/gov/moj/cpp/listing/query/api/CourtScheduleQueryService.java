package uk.gov.moj.cpp.listing.query.api;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toMap;

import uk.gov.moj.cpp.listing.common.NoteUUIDService;
import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.domain.json.JsonObjects;
import uk.gov.moj.cpp.listing.domain.json.ListToJsonArrayConverter;
import uk.gov.moj.cpp.listing.domain.json.LocalDates;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The three hand-written JAX-RS adapters that proxy listingcourtscheduler, ported unchanged:
 * {@code DefaultQueryApiHearingSlotsResource} (slots + the listing notes of each slot's court room
 * and day), {@code DefaultQueryApiSessionAvailabilityValidationResource} and
 * {@code DefaultQueryApiCourtScheduleDraftStatusResource}.
 */
@Component
@RequiredArgsConstructor
public class CourtScheduleQueryService {

    private static final String SESSION_DATE = "sessionDate";
    private static final String COURT_ROOM_ID = "courtRoomId";

    private final CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;
    private final NotesService notesService;
    private final ListToJsonArrayConverter<Notes> listToJsonArrayConverter;

    @SuppressWarnings("squid:S00107")
    public HttpResult getHearingSlots(final String panel, final String sessionStartDate, final String sessionEndDate,
                                      final String hearingStartTime, final String oucodeL2Code, final String ouCode,
                                      final String courtRoomId, final String courtRoomNumber, final String businessType,
                                      final String courtSession, final Boolean isSlotBased, final Boolean showOverbookedSlots,
                                      final String pageSize, final String pageNumber, final Integer availableDurationMins,
                                      final String status, final String jurisdiction) {
        final Map<String, String> params = new HashMap<>();
        params.put("panel", panel);
        params.put("sessionStartDate", sessionStartDate);
        params.put("sessionEndDate", sessionEndDate);
        params.put("hearingStartTime", hearingStartTime);
        params.put("oucodeL2Code", oucodeL2Code);
        params.put("ouCode", ouCode);
        params.put(COURT_ROOM_ID, courtRoomId);
        params.put("courtRoomNumber", courtRoomNumber);
        params.put("businessType", businessType);
        params.put("courtSession", courtSession);
        if (isSlotBased != null) {
            params.put("isSlotBased", String.valueOf(isSlotBased));
        }
        if (showOverbookedSlots != null) {
            params.put("showOverbookedSlots", String.valueOf(showOverbookedSlots));
        }
        params.put("pageSize", pageSize);
        params.put("pageNumber", pageNumber);
        if (availableDurationMins != null) {
            params.put("availableDurationMins", String.valueOf(availableDurationMins));
        }
        params.put("status", status != null ? status : "ALL");
        params.put("jurisdiction", jurisdiction);
        final Map<String, String> nonNullParams = params.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        final HttpResult response = courtSchedulerServiceAdapter.hearingSlotsSearch(nonNullParams);
        if (!response.isOk()) {
            return response;
        }
        final JsonObject entity = response.jsonEntity();
        final JsonArray notes = Optional.ofNullable(entity)
                .map(p -> p.getJsonArray("hearingSlots"))
                .map(this::convertToNotes)
                .orElse(JsonObjects.createArrayBuilder().build());

        final JsonObjectBuilder builder = JsonObjects.createObjectBuilder();
        entity.forEach(builder::add);
        builder.add("notes", notes);
        return new HttpResult(response.status(), builder.build());
    }

    public HttpResult validateSessionAvailability(final JsonObject requestPayload) {
        return courtSchedulerServiceAdapter.validateSessionAvailability(requestPayload);
    }

    public JsonObject getCourtScheduleDraftStatus(final JsonObject requestPayload) {
        return courtSchedulerServiceAdapter.getCourtScheduleDraftStatus(requestPayload);
    }

    private JsonArray convertToNotes(final JsonArray hearings) {
        final List<NoteUUIDService.ListingNotesCollection> notes = hearings.stream().map(h -> (JsonObject) h)
                .filter(h -> h.containsKey(COURT_ROOM_ID) && !h.isNull(COURT_ROOM_ID)
                        && h.containsKey(SESSION_DATE) && !h.isNull(SESSION_DATE))
                .map(h -> new NoteUUIDService.ListingNotesCollection(fromString(h.getString(COURT_ROOM_ID)), LocalDates.from(h.getString(SESSION_DATE))))
                .toList();
        return listToJsonArrayConverter.convert(notesService.findNotes(notes));
    }
}
