package uk.gov.justice.api.resource;

import static java.util.stream.Collectors.toMap;
import static uk.gov.justice.services.common.converter.LocalDates.from;
import static java.util.UUID.fromString;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import javax.json.JsonObjectBuilder;
import org.apache.http.HttpStatus;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.moj.cpp.listing.common.NoteUUIDService;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;

@SuppressWarnings({"squid:S1612"})
@Adapter(Component.QUERY_API)
public class DefaultQueryApiHearingSlotsResource implements QueryApiHearingSlotsResource {

    @Inject
    private HearingSlotsService hearingSlotsService;

    @Inject
    private NotesService notesService;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Override
    public Response getHearingSlots(final String panel,
                                    final String sessionStartDate,
                                    final String sessionEndDate,
                                    final String oucodeL2Code,
                                    final String ouCode,
                                    final String courtRoomId,
                                    final String courtRoomNumber,
                                    final String businessType,
                                    final String courtSession,
                                    final String pageSize,
                                    final String pageNumber) {

        final Map<String, String> params = buildParamsMap(panel, sessionStartDate, sessionEndDate, oucodeL2Code, ouCode, courtRoomId, courtRoomNumber, businessType, courtSession, pageSize, pageNumber);
        final Response response = hearingSlotsService.search(params);
        if(response.getStatusInfo().getStatusCode() != HttpStatus.SC_OK ){
            return response;
        }
        final JsonArray notes = Optional.ofNullable(response.getEntity()).
                filter( e -> e != null).
                map(e -> (JsonObject) e).
                map(p -> p.getJsonArray("hearingSlots")).
                map(hearings -> convertToNotes(hearings)).
                orElse(Json.createArrayBuilder().build());

        final JsonObjectBuilder builder = buildJsonBuilderFromJsonObject((JsonObject) response.getEntity());
        builder.add("notes", notes);
        final JsonObject payload =  builder.build();

        return Response.fromResponse(response).entity(payload).build();
    }

    private Map<String, String> buildParamsMap(final String panel,
                                               final String sessionStartDate,
                                               final String sessionEndDate,
                                               final String oucodeL2Code,
                                               final String ouCode,
                                               final String courtRoomId,
                                               final String courtRoomNumber,
                                               final String businessType,
                                               final String courtSession,
                                               final String pageSize,
                                               final String pageNumber) {
        final Map<String, String> params = new HashMap<>();
        params.put(PANEL, panel);
        params.put(SESSION_START_DATE, sessionStartDate);
        params.put(SESSION_END_DATE, sessionEndDate);
        params.put(OU_L2_CODE, oucodeL2Code);
        params.put(OUCODE, ouCode);
        params.put(COURT_ROOM_ID, courtRoomId);
        params.put(COURT_ROOM_NUMBER, courtRoomNumber);
        params.put(BUSINESS_TYPE, businessType);
        params.put(COURT_SESSION, courtSession);
        params.put(PAGE_SIZE, pageSize);
        params.put(PAGE_NUMBER, pageNumber);

        return params.entrySet().stream().filter(entry -> entry.getValue() != null).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static JsonObjectBuilder buildJsonBuilderFromJsonObject(final JsonObject origin) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        origin.entrySet().forEach( entry -> builder.add(entry.getKey(), entry.getValue()));
        return builder;
    }

    private  JsonArray convertToNotes(JsonArray hearings){
        final List<NoteUUIDService.ListingNotesCollection> notes = hearings.stream().map(h -> (JsonObject) h).
                map( h -> new NoteUUIDService.ListingNotesCollection(fromString(h.getString("courtRoomId")), from(h.getString("sessionDate")))).
                collect(Collectors.toList());
        return listToJsonArrayConverter.convert(notesService.findNotes(notes));
    }
}
