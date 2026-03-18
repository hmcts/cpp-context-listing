package uk.gov.moj.cpp.listing.common.service;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.common.utils.FileUtil.givenPayload;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.domain.JudicialRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtSchedulerServiceAdapterTest {

    @InjectMocks
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @Mock
    private Response response;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Test
    void shouldGetJudicialRoles() {
        final String startDate = LocalDate.now().toString();
        final String ouCode = "B01LY00";
        final Optional<String> courtSessionOptional = Optional.of("AM");
        final String courtRoomId = UUID.randomUUID().toString();

        final JsonObject hearingSlotsResponse = givenPayload("/mock-data/azure.rotasl.getHearingSlots.stub-data.json");

        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(hearingSlotsResponse);
        when(hearingSlotsService.search(anyMap())).thenReturn(response);

        final List<JudicialRole> judicialRoleList = courtSchedulerServiceAdapter.getJudicialRoles(startDate, ouCode, courtSessionOptional, courtRoomId);

        assertThat(judicialRoleList.size(), is(3));

        IntStream.range(0, judicialRoleList.size()).forEach(index -> {
            final JudicialRole judicialRole = judicialRoleList.get(index);

            final JsonObject judiciaryJsonObject = (JsonObject) ((JsonObject) hearingSlotsResponse.getJsonArray("hearingSlots").get(0)).getJsonArray("judiciaries").get(index);

            assertThat(judicialRole.getJudicialId().toString(), is(judiciaryJsonObject.getString("judiciaryId")));
            assertThat(judicialRole.getIsBenchChairman(), is(Optional.of(judiciaryJsonObject.getBoolean("benchChairman"))));
            assertThat(judicialRole.getIsDeputy(), is(Optional.of(judiciaryJsonObject.getBoolean("deputy"))));
            assertThat(judicialRole.getJudicialRoleType().getJudiciaryType(), is(judiciaryJsonObject.getString("judiciaryType")));
        });

    }

    @Test
    void shouldGetEmptyListIfThereIsNoMatchingJudicialRolesInRotaSL() {
        final String startDate = LocalDate.now().toString();
        final String ouCode = "B01LY00";
        final Optional<String> courtSessionOptional = Optional.of("AM");
        final String courtRoomId = UUID.randomUUID().toString();

        when(response.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn("entity response");
        when(hearingSlotsService.search(anyMap())).thenReturn(response);

        final List<JudicialRole> judicialRoleList = courtSchedulerServiceAdapter.getJudicialRoles(startDate, ouCode, courtSessionOptional, courtRoomId);

        assertTrue(CollectionUtils.isEmpty(judicialRoleList));
    }

    @Test
    void shouldGetHearingSlotResponse() {
        final String startDate = LocalDate.now().toString();
        final String ouCode = "B01LY00";
        final String courtRoomId = "a91a93e6-d704-3cf1-9f20-e267b5a7eeeb";

        final JsonObject hearingSlotsResponse = givenPayload("/mock-data/azure.rotasl.getHearingSlots.stub-data.json");

        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(hearingSlotsResponse);
        when(hearingSlotsService.search(anyMap())).thenReturn(response);

        final Response slotResponse = courtSchedulerServiceAdapter.getHearingSlotResponse(startDate, startDate, ouCode, courtRoomId);

        final JsonObject responseJson = objectToJsonObjectConverter.convert(slotResponse.getEntity());
        final JsonObject object = responseJson.getJsonArray("hearingSlots").getValuesAs(JsonObject.class).get(0);

        assertThat(object.getString("panel"), is("YOUTH"));
        assertThat(object.getString("courtRoomId"), is(courtRoomId));
    }

    @Test
    void shouldGetPanelInfoIfNotPresentInPayload() {
        final Optional<String> panelInfoFromPayload = empty();

        final LocalDate startDate = LocalDate.of(2021, 6, 21);
        final LocalDate endDate = LocalDate.of(2021, 6, 22);
        final UUID courtRoomId = UUID.fromString("a91a93e6-d704-3cf1-9f20-e267b5a7eeeb");
        final String ouCode = "B06AN00";

        final JsonObject hearingSlotsResponseJsonObject = givenPayload("/mock-data/azure.rotasl.getHearingSlots.stub-data.json");

        when(hearingSlotsService.search(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(hearingSlotsResponseJsonObject);

        final Optional<String> actualPanelInfo = courtSchedulerServiceAdapter.getPanelInfo(panelInfoFromPayload, startDate, endDate, courtRoomId, ouCode);


        assertTrue(actualPanelInfo.isPresent());
        assertThat(actualPanelInfo.get(), CoreMatchers.is(hearingSlotsResponseJsonObject.getJsonArray("hearingSlots").getJsonObject(0).getString("panel")));
    }

    @Test
    void shouldGetPanelInfoIfPresentInPayload() {
        final Optional<String> panelInfoFromPayload = of("ADULT");

        final LocalDate startDate = LocalDate.of(2021, 6, 21);
        final LocalDate endDate = LocalDate.of(2021, 6, 22);
        final UUID courtRoomId = UUID.fromString("a91a93e6-d704-3cf1-9f20-e267b5a7eeeb");
        final String ouCode = "B06AN00";

        final Optional<String> actualPanelInfo = courtSchedulerServiceAdapter.getPanelInfo(panelInfoFromPayload, startDate, endDate, courtRoomId, ouCode);


        assertTrue(actualPanelInfo.isPresent());
        assertThat(actualPanelInfo.get(), CoreMatchers.is(panelInfoFromPayload.get()));
    }

    @Test
    void shouldReturnEmptyPanelInfoIfNotPresentInPayloadAndGettingErrorFromRotaApi() {
        final Optional<String> panelInfoFromPayload = empty();

        final LocalDate startDate = LocalDate.of(2021, 6, 21);
        final LocalDate endDate = LocalDate.of(2021, 6, 22);
        final UUID courtRoomId = UUID.fromString("a91a93e6-d704-3cf1-9f20-e267b5a7eeeb");
        final String ouCode = "B06AN00";

        when(hearingSlotsService.search(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        final Optional<String> actualPanelInfo = courtSchedulerServiceAdapter.getPanelInfo(panelInfoFromPayload, startDate, endDate, courtRoomId, ouCode);


        assertFalse(actualPanelInfo.isPresent());
    }

    @Test
    void shouldReturnEmptyPanelInfoIfNotPresentInPayloadAndGettingEmptyPayloadFromRotaApi() {
        final Optional<String> panelInfoFromPayload = empty();

        final LocalDate startDate = LocalDate.of(2021, 6, 21);
        final LocalDate endDate = LocalDate.of(2021, 6, 22);
        final UUID courtRoomId = UUID.fromString("a91a93e6-d704-3cf1-9f20-e267b5a7eeeb");
        final String ouCode = "B06AN00";

        final JsonObject hearingSlotsResponseJsonObject = givenPayload("/mock-data/azure.rotasl.getHearingSlots.empty-response.json");

        when(hearingSlotsService.search(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(hearingSlotsResponseJsonObject);

        final Optional<String> actualPanelInfo = courtSchedulerServiceAdapter.getPanelInfo(panelInfoFromPayload, startDate, endDate, courtRoomId, ouCode);


        assertFalse(actualPanelInfo.isPresent());
    }

    @Test
    void shouldGetHearingIds() {
        final String courtCentreId = UUID.randomUUID().toString();
        final Optional<String> courtSessionOptional = Optional.of("AD");
        final String courtRoomId = UUID.randomUUID().toString();
        final String startDate = LocalDate.now().toString();
        final String endDate = LocalDate.now().plusDays(7).toString();
        final Optional<String> businessTypeOptional = Optional.of("BA123");
        final Integer pageSize = 50;
        final Integer pageNumber = 1;

        final JsonObject hearingIdsResponse = givenPayload("/mock-data/azure.rotasl.getHearingIds.stub-data.json");

        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(hearingIdsResponse);
        when(hearingSlotsService.getCourtSchedulerHearingIds(anyMap())).thenReturn(response);

        final HearingIdsResponse finalResp = courtSchedulerServiceAdapter.getCourtSchedulerHearings(courtCentreId, courtSessionOptional, courtRoomId, startDate, endDate, Optional.of(Instant.now()), businessTypeOptional, "ADULT,YOUTH", pageSize, pageNumber);

        assertThat(finalResp.getUuids().size(), is(4));
        assertThat(finalResp.getPageCount(), is(1L));
        assertThat(finalResp.getResults(), is(4L));
    }

    @Test
    void shouldValidateSessionAvailability() {
        final JsonObject validateResponse = givenPayload("/mock-data/azure.rotasl.getHearingSlots.stub-data.json");
        final java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("panel", "ADULT");
        params.put("sessionStartDate", "2017-10-11");
        params.put("sessionEndDate", "2020-10-11");

        when(hearingSlotsService.validateSessionAvailability(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(validateResponse);

        final Response result = courtSchedulerServiceAdapter.validateSessionAvailability(params);

        assertThat(result.getStatus(), is(HttpStatus.SC_OK));
        assertThat(result.getEntity(), is(validateResponse));
    }

    @Test
    void shouldReturnErrorResponseWhenValidateSessionAvailabilityFails() {
        final java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("panel", "ADULT");

        when(hearingSlotsService.validateSessionAvailability(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn("Validation failed");

        final Response result = courtSchedulerServiceAdapter.validateSessionAvailability(params);

        assertThat(result.getStatus(), is(HttpStatus.SC_BAD_REQUEST));
    }
}
