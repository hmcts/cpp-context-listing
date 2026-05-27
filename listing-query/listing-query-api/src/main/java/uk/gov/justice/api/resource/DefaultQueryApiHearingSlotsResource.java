package uk.gov.justice.api.resource;

import org.apache.http.HttpStatus;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.moj.cpp.listing.common.NoteUUIDService;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toMap;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.*;
import static uk.gov.justice.services.common.converter.LocalDates.from;

@SuppressWarnings({"squid:S1612"})
@Adapter(Component.QUERY_API)
public class DefaultQueryApiHearingSlotsResource implements QueryApiHearingSlotsResource {

    @Inject
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Inject
    private NotesService notesService;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Override
    public Response getHearingSlots(final String panel,
                                    final String sessionStartDate,
                                    final String sessionEndDate,
                                    final String hearingStartTime,
                                    final String oucodeL2Code,
                                    final String ouCode,
                                    final String courtRoomId,
                                    final String courtRoomNumber,
                                    final String businessType,
                                    final String courtSession,
                                    final Boolean isSlotBased,
                                    final Boolean showOverbookedSlots,
                                    final String pageSize,
                                    final String pageNumber,
                                    final Integer availableDurationMins,
                                    final String status,
                                    final String jurisdiction) {

        final Map<String, String> params = buildParamsMap(panel, sessionStartDate, sessionEndDate, hearingStartTime, oucodeL2Code, ouCode, courtRoomId, courtRoomNumber, businessType, courtSession, isSlotBased, showOverbookedSlots, pageSize, pageNumber, availableDurationMins, status, jurisdiction);
        final Response response = courtSchedulerServiceAdapter.hearingSlotsSearch(params);
        if(response.getStatusInfo().getStatusCode() != HttpStatus.SC_OK ){
            return response;
        }
        final JsonArray notes = Optional.ofNullable(response.getEntity()).
                map(e -> (JsonObject) e).
                map(p -> p.getJsonArray("hearingSlots")).
                map(this::convertToNotes).
                orElse(JsonObjects.createArrayBuilder().build());

        final JsonObjectBuilder builder = buildJsonBuilderFromJsonObject((JsonObject) response.getEntity());
        builder.add("notes", notes);
        final JsonObject payload =  builder.build();

        return Response.fromResponse(response).entity(payload).build();
    }

    private Map<String, String> buildParamsMap(final String panel,
                                               final String sessionStartDate,
                                               final String sessionEndDate,
                                               final String hearingStartTime,
                                               final String oucodeL2Code,
                                               final String ouCode,
                                               final String courtRoomId,
                                               final String courtRoomNumber,
                                               final String businessType,
                                               final String courtSession,
                                               final Boolean isSlotBased,
                                               final Boolean showOverbookedSlots,
                                               final String pageSize,
                                               final String pageNumber,
                                               final Integer availableDurationMins,
                                               final String status,
                                               final String jurisdiction) {
        final Map<String, String> params = new HashMap<>();
        params.put(PANEL, panel);
        params.put(SESSION_START_DATE, sessionStartDate);
        params.put(SESSION_END_DATE, sessionEndDate);
        params.put(HEARING_START_TIME, hearingStartTime);
        params.put(OU_L2_CODE, oucodeL2Code);
        params.put(OUCODE, ouCode);
        params.put(COURT_ROOM_ID, courtRoomId);
        params.put(COURT_ROOM_NUMBER, courtRoomNumber);
        params.put(BUSINESS_TYPE, businessType);
        params.put(COURT_SESSION, courtSession);
        if(isSlotBased != null) {
            params.put(IS_SLOT_BASED, String.valueOf(isSlotBased));
        }
        if(showOverbookedSlots != null) {
            params.put(SHOW_OVERBOOKED_SLOTS, String.valueOf(showOverbookedSlots));
        }
        params.put(PAGE_SIZE, pageSize);
        params.put(PAGE_NUMBER, pageNumber);
        if(availableDurationMins != null)
            params.put(AVAILABLE_DURATION_MINS, String.valueOf(availableDurationMins));
        params.put(STATUS, status != null ? status : "ALL");
        params.put(JURISDICTION, jurisdiction);

        return params.entrySet().stream().filter(entry -> entry.getValue() != null).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static JsonObjectBuilder buildJsonBuilderFromJsonObject(final JsonObject origin) {
        final JsonObjectBuilder builder = JsonObjects.createObjectBuilder();
        origin.entrySet().forEach( entry -> builder.add(entry.getKey(), entry.getValue()));
        return builder;
    }

    private  JsonArray convertToNotes(JsonArray hearings){
        final List<NoteUUIDService.ListingNotesCollection> notes = hearings.stream().map(h -> (JsonObject) h).
                filter(h -> h.containsKey(COURT_ROOM_ID) && !h.isNull(COURT_ROOM_ID)).
                map( h -> new NoteUUIDService.ListingNotesCollection(fromString(h.getString(COURT_ROOM_ID)), from(h.getString("sessionDate"))))
                .toList();
        return listToJsonArrayConverter.convert(notesService.findNotes(notes));
    }
}
