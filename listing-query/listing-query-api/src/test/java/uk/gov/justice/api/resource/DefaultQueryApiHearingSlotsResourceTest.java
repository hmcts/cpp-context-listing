package uk.gov.justice.api.resource;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.common.converter.LocalDates.from;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultQueryApiHearingSlotsResourceTest {

    private final String AZURE_RESULT = "listing.search.hearing.slots.json";
    private final String SLOT_SEARCH_RESPONSE_WITH_HEARING_START_TIME = "listing.search.hearing.slots.withhearingstarttime.json";

    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Mock
    private NotesService notesService;

    private Response response;

    private List<Notes> notes;

    private ListToJsonArrayConverter listToJsonArrayConverter = new ListToJsonArrayConverter();

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new JsonObjectConvertersFactory().stringToJsonObjectConverter();

    @InjectMocks
    private DefaultQueryApiHearingSlotsResource queryApiHearingSlotsResource;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        response = Response.status(Response.Status.OK).entity(createJsonObject()).build();
        FieldUtils.writeField(this.listToJsonArrayConverter, "mapper", objectMapper, true);
        FieldUtils.writeField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", stringToJsonObjectConverter, true);
        FieldUtils.writeField(this.queryApiHearingSlotsResource, "listToJsonArrayConverter", listToJsonArrayConverter, true);
    }

    @Test
    void searchHearingSlots() {
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(any(Map.class))).thenReturn(response);
        when(notesService.findNotes(any(List.class))).thenReturn(new ArrayList());

        Response result = queryApiHearingSlotsResource.getHearingSlots("ADULT",
                "2017-10-11",
                "2020-10-11",
                null,
                "BAOOUS",
                "BAOOUS",
                "001c067d-eaca-4ce5-ad90-a366ef3e4bb6",
                "1234",
                "BYS",
                "AM",
                null,
                "20",
                "1");

        verify(courtSchedulerServiceAdapter).hearingSlotsSearch(any(Map.class));
        verify(notesService).findNotes(any(List.class));
        JsonObject payload = (JsonObject) result.getEntity();
        assertNotNull(payload.getJsonNumber("results"));
        assertNotNull(payload.getJsonNumber("pageCount"));
        assertNotNull(payload.getJsonArray("hearingSlots"));
        assertNotNull(payload.getJsonArray("notes"));
        assertEquals(0, payload.getJsonArray("notes").size());
    }

    @Test
    void searchHearingSlots_WithHearingStartTime() {
        response = Response.status(Response.Status.OK).entity(createJsonObject_WithHearingStartTime()).build();
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(any(Map.class))).thenReturn(response);
        when(notesService.findNotes(any(List.class))).thenReturn(new ArrayList());

        Response result = queryApiHearingSlotsResource.getHearingSlots("ADULT",
                "2017-10-11",
                "2020-10-11",
                "2017-10-11T09:00:00.000Z",
                "BAOOUS",
                "BAOOUS",
                "001c067d-eaca-4ce5-ad90-a366ef3e4bb6",
                "1234",
                "BYS",
                "AM",
                null,
                "20",
                "1");

        verify(courtSchedulerServiceAdapter).hearingSlotsSearch(any(Map.class));
        verify(notesService).findNotes(any(List.class));
        JsonObject payload = (JsonObject) result.getEntity();
        assertNotNull(payload.getJsonNumber("results"));
        assertNotNull(payload.getJsonNumber("pageCount"));
        assertNotNull(payload.getJsonArray("hearingSlots"));
        assertNotNull(payload.getJsonArray("notes"));
        assertEquals(0, payload.getJsonArray("notes").size());
    }

    @Test
    void shouldReturnListingNotesWhenRelevantListingNotesExist(){
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(any(Map.class))).thenReturn(response);
        when(notesService.findNotes(any(List.class))).thenReturn(createNotes(response));

        Response result = queryApiHearingSlotsResource.getHearingSlots("ADULT",
                "2017-10-11",
                "2020-10-11",
                null,
                "BAOOUS",
                "BAOOUS",
                "001c067d-eaca-4ce5-ad90-a366ef3e4bb6",
                "1234",
                "BYS",
                "AM",
                null,
                "20",
                "1");

        verify(courtSchedulerServiceAdapter).hearingSlotsSearch(any(Map.class));
        verify(notesService).findNotes(any(List.class));
        JsonObject payload = (JsonObject) result.getEntity();
        assertNotNull(payload.getJsonNumber("results"));
        assertNotNull(payload.getJsonNumber("pageCount"));
        assertNotNull(payload.getJsonArray("hearingSlots"));
        assertNotNull(payload.getJsonArray("notes"));
        assertEquals(10, payload.getJsonArray("notes").size());
        assertNotNull(((JsonObject)payload.getJsonArray("notes").get(0)).get("courtRoomId"));
        assertNotNull(((JsonObject)payload.getJsonArray("notes").get(0)).get("date"));
        assertNotNull(((JsonObject)payload.getJsonArray("notes").get(0)).get("id"));
        assertNotNull(((JsonObject)payload.getJsonArray("notes").get(0)).get("note"));
        assertNotNull(((JsonObject)payload.getJsonArray("notes").get(9)).get("courtRoomId"));
        assertNotNull(((JsonObject)payload.getJsonArray("notes").get(9)).get("date"));
        assertNotNull(((JsonObject)payload.getJsonArray("notes").get(9)).get("id"));
        assertNotNull(((JsonObject)payload.getJsonArray("notes").get(9)).get("note"));


    }

    private JsonObject createJsonObject() {
        final String payload = FileUtil.getPayload(AZURE_RESULT);
        return new StringToJsonObjectConverter().convert(payload);
    }

    private JsonObject createJsonObject_WithHearingStartTime() {
        final String payload = FileUtil.getPayload(SLOT_SEARCH_RESPONSE_WITH_HEARING_START_TIME);
        return new StringToJsonObjectConverter().convert(payload);
    }

    private List<Notes> createNotes(Response response){
        final Object entity = response.getEntity();
        return Optional.of(entity).
                map(e -> (JsonObject) e).
                map(p -> p.getJsonArray("hearingSlots")).
                map(hearings -> convertToNotes(hearings)).get();
    }

    private  List<Notes> convertToNotes(JsonArray hearings){
        return hearings.stream().map(h->(JsonObject)h)
                .map( hearing -> getNotes(hearing)
                ).collect(toList());

    }

    private Notes getNotes(final JsonObject hearing) {
        return new Notes(UUID.randomUUID(),
                fromString(hearing.getString("courtRoomId")),
                from(hearing.getString("sessionDate")),
                STRING.next());
    }

}
