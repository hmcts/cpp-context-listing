package uk.gov.moj.cpp.listing.query.view.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionJudiciaryEnrichmentServiceTest {

    private static final String COURT_SCHEDULE_ID_1 = UUID.randomUUID().toString();
    private static final String COURT_SCHEDULE_ID_2 = UUID.randomUUID().toString();
    private static final String JUDICIARY_ID_1 = UUID.randomUUID().toString();
    private static final String JUDICIARY_ID_2 = UUID.randomUUID().toString();

    @Mock
    private HearingSlotsService hearingSlotsService;

    @InjectMocks
    private SessionJudiciaryEnrichmentService service;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldDoNothingForEmptyList() {
        service.enrichWithSessionJudiciary(Collections.emptyList());
        verify(hearingSlotsService, never()).getCourtSchedulesById(argThat(m -> true));
    }

    @Test
    void shouldDoNothingForNullList() {
        service.enrichWithSessionJudiciary(null);
        verify(hearingSlotsService, never()).getCourtSchedulesById(argThat(m -> true));
    }

    @Test
    void shouldSetJudiciarySourceHearingAndNotCallCourtSchedulerWhenJudiciaryAlreadyPresent() throws Exception {
        final Hearing hearing = hearingWithJudiciary(COURT_SCHEDULE_ID_1);

        service.enrichWithSessionJudiciary(List.of(hearing));

        assertThat(hearing.getProperties().get("judiciarySource").asText(),
                is(SessionJudiciaryEnrichmentService.JUDICIARY_SOURCE_HEARING));
        verify(hearingSlotsService, never()).getCourtSchedulesById(argThat(m -> true));
    }

    @Test
    void shouldInjectSessionJudiciaryAndMapFieldsCorrectlyWhenHearingJudiciaryIsEmpty() throws Exception {
        final Hearing hearing = hearingWithoutJudiciary(COURT_SCHEDULE_ID_1);

        when(hearingSlotsService.getCourtSchedulesById(argThat(
                m -> COURT_SCHEDULE_ID_1.equals(m.get("courtScheduleIds")))))
                .thenReturn(buildCourtSchedulerResponse(
                        COURT_SCHEDULE_ID_1, JUDICIARY_ID_1, "RECORDER", true, false));

        service.enrichWithSessionJudiciary(List.of(hearing));

        final JsonNode props = hearing.getProperties();
        assertThat(props.get("judiciarySource").asText(), is(SessionJudiciaryEnrichmentService.JUDICIARY_SOURCE_SESSION));

        final JsonNode judiciary = props.get("judiciary");
        assertThat(judiciary.isArray(), is(true));
        assertThat(judiciary.size(), is(1));

        final JsonNode role = judiciary.get(0);
        assertThat(role.get("judicialId").asText(), is(JUDICIARY_ID_1));
        assertThat(role.get("judiciaryType").asText(), is("RECORDER"));
        assertThat(role.get("isBenchChairman").asBoolean(), is(true));
        assertThat(role.get("isDeputy").asBoolean(), is(false));
    }

    @Test
    void shouldInjectSessionJudiciaryWhenHearingJudiciaryFieldIsMissingEntirely() throws Exception {
        final Hearing hearing = hearingWithNoJudiciaryField(COURT_SCHEDULE_ID_1);

        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(buildCourtSchedulerResponse(
                        COURT_SCHEDULE_ID_1, JUDICIARY_ID_1, "JUDGE", false, true));

        service.enrichWithSessionJudiciary(List.of(hearing));

        final JsonNode props = hearing.getProperties();
        assertThat(props.get("judiciarySource").asText(), is(SessionJudiciaryEnrichmentService.JUDICIARY_SOURCE_SESSION));
        assertThat(props.get("judiciary").size(), is(1));
        assertThat(props.get("judiciary").get(0).get("judicialId").asText(), is(JUDICIARY_ID_1));
    }

    @Test
    void shouldSetSessionSourceButLeaveJudiciaryEmptyWhenNoMatchingScheduleIdInResponse() throws Exception {
        final Hearing hearing = hearingWithoutJudiciary(COURT_SCHEDULE_ID_1);

        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(buildCourtSchedulerResponse(
                        COURT_SCHEDULE_ID_2, JUDICIARY_ID_1, "JUDGE", false, true));

        service.enrichWithSessionJudiciary(List.of(hearing));

        final JsonNode props = hearing.getProperties();
        assertThat(props.get("judiciarySource").asText(), is(SessionJudiciaryEnrichmentService.JUDICIARY_SOURCE_SESSION));
        assertThat(props.get("judiciary").size(), is(0));
    }

    @Test
    void shouldSetSessionSourceAndSkipCourtSchedulerCallWhenHearingDaysFieldIsAbsent() throws Exception {
        final Hearing hearing = hearingFromJson("""
                { "judiciary": [] }
                """);

        service.enrichWithSessionJudiciary(List.of(hearing));

        assertThat(hearing.getProperties().get("judiciarySource").asText(),
                is(SessionJudiciaryEnrichmentService.JUDICIARY_SOURCE_SESSION));
        verify(hearingSlotsService, never()).getCourtSchedulesById(argThat(m -> true));
    }

    @Test
    void shouldSetSessionSourceAndSkipCourtSchedulerCallWhenNoCourtScheduleIdsInHearingDays() throws Exception {
        final Hearing hearing = hearingWithoutJudiciaryAndNoCourtScheduleId();

        service.enrichWithSessionJudiciary(List.of(hearing));

        assertThat(hearing.getProperties().get("judiciarySource").asText(),
                is(SessionJudiciaryEnrichmentService.JUDICIARY_SOURCE_SESSION));
        verify(hearingSlotsService, never()).getCourtSchedulesById(argThat(m -> true));
    }

    @Test
    void shouldSetSessionSourceAndLeaveJudiciaryEmptyWhenCourtSchedulerReturnsNonOkStatus() throws Exception {
        final Hearing hearing = hearingWithoutJudiciary(COURT_SCHEDULE_ID_1);

        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

        service.enrichWithSessionJudiciary(List.of(hearing));

        assertThat(hearing.getProperties().get("judiciarySource").asText(),
                is(SessionJudiciaryEnrichmentService.JUDICIARY_SOURCE_SESSION));
        assertThat(hearing.getProperties().get("judiciary").size(), is(0));
    }

    @Test
    void shouldDeduplicateJudiciaryWhenSameCourtScheduleIdAppearsInMultipleHearingDays() throws Exception {
        final Hearing hearing = hearingWithTwoHearingDays(COURT_SCHEDULE_ID_1, COURT_SCHEDULE_ID_1);

        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(buildCourtSchedulerResponse(
                        COURT_SCHEDULE_ID_1, JUDICIARY_ID_1, "MAGISTRATE", false, false));

        service.enrichWithSessionJudiciary(List.of(hearing));

        assertThat(hearing.getProperties().get("judiciary").size(), is(1));
    }

    @Test
    void shouldBatchAllCourtScheduleIdsIntoASingleCourtSchedulerCallForMultipleHearings() throws Exception {
        final Hearing hearing1 = hearingWithoutJudiciary(COURT_SCHEDULE_ID_1);
        final Hearing hearing2 = hearingWithoutJudiciary(COURT_SCHEDULE_ID_2);

        when(hearingSlotsService.getCourtSchedulesById(argThat(
                m -> m.get("courtScheduleIds").contains(COURT_SCHEDULE_ID_1)
                  && m.get("courtScheduleIds").contains(COURT_SCHEDULE_ID_2))))
                .thenReturn(buildCourtSchedulerResponseWithTwoSessions(
                        COURT_SCHEDULE_ID_1, JUDICIARY_ID_1, "RECORDER",
                        COURT_SCHEDULE_ID_2, JUDICIARY_ID_2, "JUDGE"));

        service.enrichWithSessionJudiciary(List.of(hearing1, hearing2));

        verify(hearingSlotsService, times(1)).getCourtSchedulesById(argThat(m -> true));

        assertThat(hearing1.getProperties().get("judiciary").get(0).get("judicialId").asText(), is(JUDICIARY_ID_1));
        assertThat(hearing2.getProperties().get("judiciary").get(0).get("judicialId").asText(), is(JUDICIARY_ID_2));
    }

    @Test
    void shouldHandleMixedListOfHearingsWithAndWithoutJudiciary() throws Exception {
        final Hearing hearingWithJudiciary = hearingWithJudiciary(COURT_SCHEDULE_ID_1);
        final Hearing hearingWithoutJudiciary = hearingWithoutJudiciary(COURT_SCHEDULE_ID_2);

        when(hearingSlotsService.getCourtSchedulesById(argThat(
                m -> COURT_SCHEDULE_ID_2.equals(m.get("courtScheduleIds")))))
                .thenReturn(buildCourtSchedulerResponse(
                        COURT_SCHEDULE_ID_2, JUDICIARY_ID_2, "DISTRICT_JUDGE", false, true));

        service.enrichWithSessionJudiciary(List.of(hearingWithJudiciary, hearingWithoutJudiciary));

        assertThat(hearingWithJudiciary.getProperties().get("judiciarySource").asText(),
                is(SessionJudiciaryEnrichmentService.JUDICIARY_SOURCE_HEARING));
        assertThat(hearingWithJudiciary.getProperties().get("judiciary").get(0).get("judicialId").asText(),
                is(JUDICIARY_ID_1));

        assertThat(hearingWithoutJudiciary.getProperties().get("judiciarySource").asText(),
                is(SessionJudiciaryEnrichmentService.JUDICIARY_SOURCE_SESSION));
        assertThat(hearingWithoutJudiciary.getProperties().get("judiciary").get(0).get("judicialId").asText(),
                is(JUDICIARY_ID_2));
    }

    @Test
    void shouldNotCrashAndNotCallCourtSchedulerWhenHearingPropertiesIsNull() {
        final Hearing hearingWithNullProps = new Hearing();

        service.enrichWithSessionJudiciary(List.of(hearingWithNullProps));

        verify(hearingSlotsService, never()).getCourtSchedulesById(argThat(m -> true));
    }

    @Test
    void shouldSkipJudiciaryItemsWhoseJudiciaryIdIsNullInResponse() throws Exception {
        final Hearing hearing = hearingWithoutJudiciary(COURT_SCHEDULE_ID_1);

        final JsonObject session = Json.createObjectBuilder()
                .add("courtScheduleId", COURT_SCHEDULE_ID_1)
                .add("sessionDate", "2026-01-15")
                .add("judiciaries", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("judiciaryType", "RECORDER")
                                .add("isBenchChairman", false)
                                .add("isDeputy", false))
                        .add(Json.createObjectBuilder()
                                .add("id", JUDICIARY_ID_1)
                                .add("judiciaryType", "JUDGE")
                                .add("isBenchChairman", false)
                                .add("isDeputy", false)))
                .build();
        final JsonObject responseBody = Json.createObjectBuilder()
                .add("courtSchedules", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("courtRoomId", UUID.randomUUID().toString())
                                .add("courtRoomName", "Room 1")
                                .add("sessions", Json.createArrayBuilder().add(session))))
                .build();
        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(Response.ok(responseBody).build());

        service.enrichWithSessionJudiciary(List.of(hearing));

        final JsonNode judiciary = hearing.getProperties().get("judiciary");
        assertThat(judiciary.size(), is(1));
        assertThat(judiciary.get(0).get("judicialId").asText(), is(JUDICIARY_ID_1));
    }

    @Test
    void shouldInjectJudiciaryCorrectlyWhenOptionalFieldsAreMissingFromCourtSchedulerResponse() throws Exception {
        final Hearing hearing = hearingWithoutJudiciary(COURT_SCHEDULE_ID_1);

        final JsonObject session = Json.createObjectBuilder()
                .add("courtScheduleId", COURT_SCHEDULE_ID_1)
                .add("sessionDate", "2026-01-15")
                .add("judiciaries", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", JUDICIARY_ID_1)))
                .build();
        final JsonObject responseBody = Json.createObjectBuilder()
                .add("courtSchedules", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("courtRoomId", UUID.randomUUID().toString())
                                .add("courtRoomName", "Room 1")
                                .add("sessions", Json.createArrayBuilder().add(session))))
                .build();
        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(Response.ok(responseBody).build());

        service.enrichWithSessionJudiciary(List.of(hearing));

        final JsonNode role = hearing.getProperties().get("judiciary").get(0);
        assertThat(role.get("judicialId").asText(), is(JUDICIARY_ID_1));
        assertThat(role.has("isBenchChairman"), is(false));
        assertThat(role.has("isDeputy"), is(false));
        assertThat(role.has("judiciaryType"), is(false));
    }

    @Test
    void shouldReturnEmptyMapAndNotPropagateWhenCourtSchedulerCallThrows() throws Exception {
        final Hearing hearing = hearingWithoutJudiciary(COURT_SCHEDULE_ID_1);

        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenThrow(new IllegalStateException("contextSystemUserId missing!!!"));

        service.enrichWithSessionJudiciary(List.of(hearing));

        assertThat(hearing.getProperties().get("judiciarySource").asText(),
                is(SessionJudiciaryEnrichmentService.JUDICIARY_SOURCE_SESSION));
        assertThat(hearing.getProperties().get("judiciary").size(), is(0));
    }

    @Test
    void shouldReturnEmptyMapWhenResponseIsNotOk() {
        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(Response.status(Response.Status.NOT_FOUND).build());

        final Map<String, JsonArray> result = service.fetchJudiciaryByScheduleId(Set.of(COURT_SCHEDULE_ID_1));

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void shouldReturnEmptyMapWhenCourtSchedulesArrayIsEmpty() {
        final JsonObject emptyResponse = Json.createObjectBuilder()
                .add("courtSchedules", Json.createArrayBuilder())
                .build();
        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(Response.ok(emptyResponse).build());

        final Map<String, JsonArray> result = service.fetchJudiciaryByScheduleId(Set.of(COURT_SCHEDULE_ID_1));

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void shouldReturnEmptyMapWhenSessionsArrayIsNullOrMissing() {
        final JsonObject response = Json.createObjectBuilder()
                .add("courtSchedules", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("courtRoomId", UUID.randomUUID().toString())
                                .add("courtRoomName", "Room 1")))
                .build();
        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(Response.ok(response).build());

        final Map<String, JsonArray> result = service.fetchJudiciaryByScheduleId(Set.of(COURT_SCHEDULE_ID_1));

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void shouldIgnoreSessionsWithEmptyJudiciaryArray() {
        final JsonObject response = Json.createObjectBuilder()
                .add("courtSchedules", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("courtRoomId", UUID.randomUUID().toString())
                                .add("courtRoomName", "Room 1")
                                .add("sessions", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("courtScheduleId", COURT_SCHEDULE_ID_1)
                                                .add("sessionDate", "2026-01-15")
                                                .add("judiciaries", Json.createArrayBuilder())))))
                .build();
        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(Response.ok(response).build());

        final Map<String, JsonArray> result = service.fetchJudiciaryByScheduleId(Set.of(COURT_SCHEDULE_ID_1));

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void shouldReturnEmptyMapWhenResponseBodyIsEmptyJsonObject() {
        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(Response.ok(Json.createObjectBuilder().build()).build());

        final Map<String, JsonArray> result = service.fetchJudiciaryByScheduleId(Set.of(COURT_SCHEDULE_ID_1));

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void shouldIgnoreSessionsWhoseCourtScheduleIdWasNotRequested() {
        final JsonObject response = Json.createObjectBuilder()
                .add("courtSchedules", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("courtRoomId", UUID.randomUUID().toString())
                                .add("courtRoomName", "Room 1")
                                .add("sessions", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("courtScheduleId", COURT_SCHEDULE_ID_2)
                                                .add("sessionDate", "2026-01-15")
                                                .add("judiciaries", Json.createArrayBuilder()
                                                        .add(Json.createObjectBuilder()
                                                                .add("id", JUDICIARY_ID_1)
                                                                .add("judiciaryType", "JUDGE")
                                                                .add("isBenchChairman", false)
                                                                .add("isDeputy", false)))))))
                .build();
        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(Response.ok(response).build());

        final Map<String, JsonArray> result = service.fetchJudiciaryByScheduleId(Set.of(COURT_SCHEDULE_ID_1));

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void shouldInjectAllRefDataFieldsFromSessionJudiciaryResponse() throws Exception {
        final Hearing hearing = hearingWithoutJudiciary(COURT_SCHEDULE_ID_1);

        final JsonObject judiciary = Json.createObjectBuilder()
                .add("id", JUDICIARY_ID_1)
                .add("judiciaryType", "Senior Circuit Judge")
                .add("isBenchChairman", false)
                .add("isDeputy", false)
                .add("seqId", 143117)
                .add("titlePrefix", "His Honour Judge")
                .add("titleJudicialPrefix", "His Honour Judge")
                .add("titleJudicialPrefixWelsh", "Ei Anrhydedd y Barnwr")
                .add("personId", "131172")
                .add("specialisms", Json.createArrayBuilder().add("ATTEMPTED_MURDER").add("MURDER"))
                .add("requestedName", "HIS HONOUR JUDGE MELBOURNE INMAN KC HONORARY RECORDER OF BIRMINGHAM")
                .add("surname", "Inman")
                .add("forenames", "Melbourne Donald")
                .add("emailAddress", "HHJ.Melbourne.Inman@eJudiciary.net")
                .build();

        final JsonObject responseBody = Json.createObjectBuilder()
                .add("courtSchedules", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("courtRoomId", UUID.randomUUID().toString())
                                .add("courtRoomName", "Court Room 1")
                                .add("sessions", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("courtScheduleId", COURT_SCHEDULE_ID_1)
                                                .add("sessionDate", "2026-01-15")
                                                .add("judiciaries", Json.createArrayBuilder().add(judiciary))))))
                .build();

        when(hearingSlotsService.getCourtSchedulesById(argThat(m -> true)))
                .thenReturn(Response.ok(responseBody).build());

        service.enrichWithSessionJudiciary(List.of(hearing));

        final JsonNode role = hearing.getProperties().get("judiciary").get(0);
        assertThat(role.get("judicialId").asText(), is(JUDICIARY_ID_1));
        assertThat(role.get("judiciaryType").asText(), is("Senior Circuit Judge"));
        assertThat(role.get("isBenchChairman").asBoolean(), is(false));
        assertThat(role.get("isDeputy").asBoolean(), is(false));
        assertThat(role.get("seqId").asInt(), is(143117));
        assertThat(role.get("titlePrefix").asText(), is("His Honour Judge"));
        assertThat(role.get("titleJudicialPrefix").asText(), is("His Honour Judge"));
        assertThat(role.get("titleJudicialPrefixWelsh").asText(), is("Ei Anrhydedd y Barnwr"));
        assertThat(role.get("personId").asText(), is("131172"));
        assertThat(role.get("specialisms").get(0).asText(), is("ATTEMPTED_MURDER"));
        assertThat(role.get("specialisms").get(1).asText(), is("MURDER"));
        assertThat(role.get("requestedName").asText(), is("HIS HONOUR JUDGE MELBOURNE INMAN KC HONORARY RECORDER OF BIRMINGHAM"));
        assertThat(role.get("surname").asText(), is("Inman"));
        assertThat(role.get("forenames").asText(), is("Melbourne Donald"));
        assertThat(role.get("emailAddress").asText(), is("HHJ.Melbourne.Inman@eJudiciary.net"));
    }

    private Hearing hearingWithJudiciary(final String courtScheduleId) throws Exception {
        final String json = """
                {
                  "judiciary": [{"judicialId": "%s", "judicialRoleType": {"judiciaryType": "JUDGE"}}],
                  "hearingDays": [{"courtScheduleId": "%s"}]
                }
                """.formatted(JUDICIARY_ID_1, courtScheduleId);
        return hearingFromJson(json);
    }

    private Hearing hearingWithoutJudiciary(final String courtScheduleId) throws Exception {
        final String json = """
                {
                  "judiciary": [],
                  "hearingDays": [{"courtScheduleId": "%s"}]
                }
                """.formatted(courtScheduleId);
        return hearingFromJson(json);
    }

    private Hearing hearingWithNoJudiciaryField(final String courtScheduleId) throws Exception {
        final String json = """
                {
                  "hearingDays": [{"courtScheduleId": "%s"}]
                }
                """.formatted(courtScheduleId);
        return hearingFromJson(json);
    }

    private Hearing hearingWithTwoHearingDays(final String csId1, final String csId2) throws Exception {
        final String json = """
                {
                  "judiciary": [],
                  "hearingDays": [
                    {"courtScheduleId": "%s"},
                    {"courtScheduleId": "%s"}
                  ]
                }
                """.formatted(csId1, csId2);
        return hearingFromJson(json);
    }

    private Hearing hearingWithoutJudiciaryAndNoCourtScheduleId() throws Exception {
        final String json = """
                {
                  "judiciary": [],
                  "hearingDays": [{"hearingDate": "2026-01-15"}]
                }
                """;
        return hearingFromJson(json);
    }

    private Hearing hearingFromJson(final String json) throws Exception {
        final JsonNode props = mapper.readTree(json);
        final Hearing hearing = new Hearing();
        hearing.setProperties(props);
        return hearing;
    }

    private Response buildCourtSchedulerResponse(final String courtScheduleId,
                                                 final String judiciaryId,
                                                 final String judiciaryType,
                                                 final boolean isBenchChairman,
                                                 final boolean isDeputy) {
        final JsonObject session = Json.createObjectBuilder()
                .add("courtScheduleId", courtScheduleId)
                .add("sessionDate", "2026-01-15")
                .add("judiciaries", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", judiciaryId)
                                .add("judiciaryType", judiciaryType)
                                .add("isBenchChairman", isBenchChairman)
                                .add("isDeputy", isDeputy)))
                .build();

        final JsonObject responseBody = Json.createObjectBuilder()
                .add("courtSchedules", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("courtRoomId", UUID.randomUUID().toString())
                                .add("courtRoomName", "Court Room 1")
                                .add("sessions", Json.createArrayBuilder().add(session))))
                .build();

        return Response.ok(responseBody).build();
    }

    private Response buildCourtSchedulerResponseWithTwoSessions(
            final String csId1, final String jId1, final String jType1,
            final String csId2, final String jId2, final String jType2) {

        final JsonObject session1 = Json.createObjectBuilder()
                .add("courtScheduleId", csId1)
                .add("sessionDate", "2026-01-15")
                .add("judiciaries", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", jId1)
                                .add("judiciaryType", jType1)
                                .add("isBenchChairman", false)
                                .add("isDeputy", false)))
                .build();

        final JsonObject session2 = Json.createObjectBuilder()
                .add("courtScheduleId", csId2)
                .add("sessionDate", "2026-01-16")
                .add("judiciaries", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", jId2)
                                .add("judiciaryType", jType2)
                                .add("isBenchChairman", false)
                                .add("isDeputy", false)))
                .build();

        final JsonObject responseBody = Json.createObjectBuilder()
                .add("courtSchedules", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("courtRoomId", UUID.randomUUID().toString())
                                .add("courtRoomName", "Court Room 1")
                                .add("sessions", Json.createArrayBuilder()
                                        .add(session1)
                                        .add(session2))))
                .build();

        return Response.ok(responseBody).build();
    }
}
