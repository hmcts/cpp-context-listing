package uk.gov.justice.api.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertNotNull;
import static uk.gov.justice.services.common.converter.LocalDates.from;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.json.JsonArray;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.mockito.Spy;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.util.Map;


import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultQueryApiHearingSlotsResourceTest {
    private final String AZURE_RESULT = "listing.search.hearing.slots.json";


    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Mock
    private NotesService notesService;

    private Response response;

    private List<Notes> notes;

    private ListToJsonArrayConverter listToJsonArrayConverter = new ListToJsonArrayConverter();

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @InjectMocks
    private DefaultQueryApiHearingSlotsResource resource;

    @Before
    public void setup() throws IllegalAccessException {
        final ObjectMapper objectMapper = new ObjectMapper();
        response = Response.status(Response.Status.OK).entity(createJsonObject()).build();
        FieldUtils.writeField(this.listToJsonArrayConverter, "mapper", objectMapper, true);
        FieldUtils.writeField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", stringToJsonObjectConverter, true);
        FieldUtils.writeField(this.resource, "listToJsonArrayConverter", listToJsonArrayConverter, true);
    }

    @Test
    public void searchHearingSlots() {
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(any(Map.class))).thenReturn(response);
        when(notesService.findNotes(any(List.class))).thenReturn(new ArrayList());

        Response result = resource.getHearingSlots("ADULT",
                "2017-10-11",
                "2020-10-11",
                "BAOOUS",
                "BAOOUS",
                "001c067d-eaca-4ce5-ad90-a366ef3e4bb6",
                "1234",
                "BYS",
                "AM",
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
    public void shouldReturnListingNotesWhenRelevantListingNotesExist(){
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(any(Map.class))).thenReturn(response);
        when(notesService.findNotes(any(List.class))).thenReturn(createNotes(response));

        Response result = resource.getHearingSlots("ADULT",
                "2017-10-11",
                "2020-10-11",
                "BAOOUS",
                "BAOOUS",
                "001c067d-eaca-4ce5-ad90-a366ef3e4bb6",
                "1234",
                "BYS",
                "AM",
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

    private List<Notes> createNotes(Response response){
        return Optional.of(response.getEntity()).
                map(e -> (JsonObject) e).
                map(p -> p.getJsonArray("hearingSlots")).
                map(hearings -> convertToNotes(hearings)).get();
    }

    private  List<Notes> convertToNotes(JsonArray hearings){
        return hearings.stream().map(h->(JsonObject)h)
                .map( hearing -> new Notes(UUID.randomUUID(),
                                            UUID.fromString(hearing.getString("courtRoomId")),
                                            from(hearing.getString("sessionDate")),
                                            STRING.next())).collect(Collectors.toList());

    }

}
