package uk.gov.moj.cpp.listing.query.api;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.domain.json.LocalDates.from;
import static uk.gov.moj.cpp.listing.testutil.RandomGenerator.STRING;

import uk.gov.moj.cpp.listing.domain.json.ListToJsonArrayConverter;
import uk.gov.moj.cpp.listing.domain.json.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.testutil.JsonConverters;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import uk.gov.moj.cpp.listing.common.client.HttpResult;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtScheduleQueryServiceTest {

    private final String AZURE_RESULT = "listing.search.hearing.slots.json";
    private final String SLOT_SEARCH_RESPONSE_WITH_HEARING_START_TIME = "listing.search.hearing.slots.withhearingstarttime.json";

    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Mock
    private NotesService notesService;

    @Captor
    private ArgumentCaptor<Map<String, String>> paramsCaptor;

    private HttpResult response;

    private List<Notes> notes;

    @Spy
    private ListToJsonArrayConverter<Notes> listToJsonArrayConverter = new ListToJsonArrayConverter<>();

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new JsonConverters().stringToJsonObjectConverter();

    @InjectMocks
    private CourtScheduleQueryService queryApiHearingSlotsResource;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        response = new HttpResult(200, createJsonObject());
    }

    @Test
    void searchHearingSlots() {
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(any(Map.class))).thenReturn(response);
        when(notesService.findNotes(any(List.class))).thenReturn(new ArrayList());

        HttpResult result = queryApiHearingSlotsResource.getHearingSlots("ADULT",
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
                null,
                "20",
                "1",
                20,
                null,
                "MAGISTRATES");

        verify(courtSchedulerServiceAdapter).hearingSlotsSearch(any(Map.class));
        verify(notesService).findNotes(any(List.class));
        JsonObject payload = (JsonObject) result.entity();
        assertNotNull(payload.getJsonNumber("results"));
        assertNotNull(payload.getJsonNumber("pageCount"));
        assertNotNull(payload.getJsonArray("hearingSlots"));
        assertNotNull(payload.getJsonArray("notes"));
        assertEquals(0, payload.getJsonArray("notes").size());
    }

    @Test
    void searchHearingSlots_WithHearingStartTime() {
        response = new HttpResult(200, createJsonObject_WithHearingStartTime());
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(any(Map.class))).thenReturn(response);
        when(notesService.findNotes(any(List.class))).thenReturn(new ArrayList());

        HttpResult result = queryApiHearingSlotsResource.getHearingSlots("ADULT",
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
                null,
                "20",
                "1",
                20,
                null,
                "MAGISTRATES");

        verify(courtSchedulerServiceAdapter).hearingSlotsSearch(any(Map.class));
        verify(notesService).findNotes(any(List.class));
        JsonObject payload = (JsonObject) result.entity();
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

        HttpResult result = queryApiHearingSlotsResource.getHearingSlots("ADULT",
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
                null,
                "20",
                "1",
                20,
                null,
                "MAGISTRATES");

        verify(courtSchedulerServiceAdapter).hearingSlotsSearch(any(Map.class));
        verify(notesService).findNotes(any(List.class));
        JsonObject payload = (JsonObject) result.entity();
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

    @Test
    void searchHearingSlots_passesStatusAndJurisdictionToAdapter() {
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(any(Map.class))).thenReturn(response);
        when(notesService.findNotes(any(List.class))).thenReturn(new ArrayList());

        queryApiHearingSlotsResource.getHearingSlots("ADULT",
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
                null,
                "20",
                "1",
                20,
                "FINAL",
                "MAGISTRATES");

        verify(courtSchedulerServiceAdapter).hearingSlotsSearch(paramsCaptor.capture());
        final Map<String, String> params = paramsCaptor.getValue();
        assertEquals("FINAL", params.get("status"));
        assertEquals("MAGISTRATES", params.get("jurisdiction"));
    }

    @Test
    void searchHearingSlots_defaultsStatusToAllWhenNull() {
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(any(Map.class))).thenReturn(response);
        when(notesService.findNotes(any(List.class))).thenReturn(new ArrayList());

        queryApiHearingSlotsResource.getHearingSlots("ADULT",
                "2017-10-11",
                "2020-10-11",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "20",
                "1",
                null,
                null,
                null);

        verify(courtSchedulerServiceAdapter).hearingSlotsSearch(paramsCaptor.capture());
        final Map<String, String> params = paramsCaptor.getValue();
        assertEquals("ALL", params.get("status"));
    }

    @Test
    void searchHearingSlots_skipsDraftSlotsWithoutCourtRoomId() {
        // ADR-005: courtscheduler now strips courtRoomId from draft sessions. Note enrichment is
        // keyed on (courtRoomId, sessionDate), so a draft slot lacking courtRoomId must be skipped
        // rather than fed to UUID.fromString(null) — which would throw.
        final JsonObject draftResponseBody = Json.createObjectBuilder()
                .add("results", 2)
                .add("pageCount", 1)
                .add("hearingSlots", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("courtRoomId", "001c067d-eaca-4ce5-ad90-a366ef3e4bb6")
                                .add("sessionDate", "2020-10-11"))
                        .add(Json.createObjectBuilder()
                                .add("sessionDate", "2020-10-12")))
                .build();
        final HttpResult draftResponse = new HttpResult(200, draftResponseBody);
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(any(Map.class))).thenReturn(draftResponse);
        when(notesService.findNotes(any(List.class))).thenReturn(new ArrayList());

        final ArgumentCaptor<List> notesCaptor = ArgumentCaptor.forClass(List.class);

        final HttpResult result = queryApiHearingSlotsResource.getHearingSlots("ADULT",
                "2017-10-11", "2020-10-11", null, "BAOOUS", "BAOOUS",
                "001c067d-eaca-4ce5-ad90-a366ef3e4bb6", "1234", "BYS", "AM",
                null, null, "20", "1", 20, null, "CROWN");

        verify(notesService).findNotes(notesCaptor.capture());
        assertEquals(1, notesCaptor.getValue().size());
        final JsonObject payload = (JsonObject) result.entity();
        assertNotNull(payload.getJsonArray("hearingSlots"));
        assertEquals(2, payload.getJsonArray("hearingSlots").size());
    }

    private JsonObject createJsonObject() {
        final String payload = FileUtil.getPayload(AZURE_RESULT);
        return new StringToJsonObjectConverter().convert(payload);
    }

    private JsonObject createJsonObject_WithHearingStartTime() {
        final String payload = FileUtil.getPayload(SLOT_SEARCH_RESPONSE_WITH_HEARING_START_TIME);
        return new StringToJsonObjectConverter().convert(payload);
    }

    private List<Notes> createNotes(HttpResult response){
        final Object entity = response.entity();
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


    // --- ported from DefaultQueryApiSessionAvailabilityValidationResourceTest ---

    @Test
    void shouldForwardSessionAvailabilityPayloadToAdapterAndReturnResponse() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("courtScheduleId", "f8254db1-1683-483e-afb3-b87fde5a0a26")))
                .add("duration", 30)
                .build();
        final HttpResult adapterResponse = new HttpResult(200, Json.createObjectBuilder().build());
        when(courtSchedulerServiceAdapter.validateSessionAvailability(requestPayload)).thenReturn(adapterResponse);

        final HttpResult result = queryApiHearingSlotsResource.validateSessionAvailability(requestPayload);

        verify(courtSchedulerServiceAdapter).validateSessionAvailability(eq(requestPayload));
        assertEquals(200, result.status());
    }

    @Test
    void shouldReturnSessionAvailabilityAdapterErrorResponseUnchanged() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("courtScheduleId", "f8254db1-1683-483e-afb3-b87fde5a0a26")))
                .build();
        final JsonObject errorBody = Json.createObjectBuilder()
                .add("validationResult", Json.createObjectBuilder()
                        .add("status", "FAILURE")
                        .add("validationError", "duration is required"))
                .build();
        when(courtSchedulerServiceAdapter.validateSessionAvailability(requestPayload)).thenReturn(new HttpResult(400, errorBody));

        final HttpResult result = queryApiHearingSlotsResource.validateSessionAvailability(requestPayload);

        assertEquals(400, result.status());
        assertEquals(errorBody, result.entity());
    }

    // --- ported from DefaultQueryApiCourtScheduleDraftStatusResourceTest ---

    @Test
    void shouldForwardDraftStatusPayloadToAdapterAndReturnAnyDraftTrue() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder().add("f8254db1-1683-483e-afb3-b87fde5a0a26"))
                .build();
        final JsonObject adapterBody = Json.createObjectBuilder().add("anyDraft", true).build();
        when(courtSchedulerServiceAdapter.getCourtScheduleDraftStatus(requestPayload)).thenReturn(adapterBody);

        assertEquals(adapterBody, queryApiHearingSlotsResource.getCourtScheduleDraftStatus(requestPayload));
        verify(courtSchedulerServiceAdapter).getCourtScheduleDraftStatus(eq(requestPayload));
    }

    @Test
    void shouldReturnAnyDraftFalseWhenAdapterReportsAllNonDraft() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder().add("9e4932f7-97b2-3010-b942-ddd2624e4dd8"))
                .build();
        final JsonObject adapterBody = Json.createObjectBuilder().add("anyDraft", false).build();
        when(courtSchedulerServiceAdapter.getCourtScheduleDraftStatus(requestPayload)).thenReturn(adapterBody);

        assertEquals(adapterBody, queryApiHearingSlotsResource.getCourtScheduleDraftStatus(requestPayload));
    }

    @Test
    void searchHearingSlots_passesNonOkResponseThrough() {
        final HttpResult error = new HttpResult(503, "{\"error\":\"down\"}");
        when(courtSchedulerServiceAdapter.hearingSlotsSearch(anyMap())).thenReturn(error);

        final HttpResult result = queryApiHearingSlotsResource.getHearingSlots("ADULT", "2017-10-11", "2020-10-11", null,
                null, null, null, null, null, null, null, null, "20", "1", null, null, "CROWN");

        assertEquals(error, result);
        org.mockito.Mockito.verifyNoInteractions(notesService);
    }

}
