package uk.gov.moj.cpp.listing.common.azure.adapter;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;
import uk.gov.moj.cpp.listing.common.utils.FileUtil;
import uk.gov.moj.cpp.listing.domain.JudicialRole;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;



@RunWith(MockitoJUnitRunner.class)
public class RotaSLServiceAdapterTest {

    @InjectMocks
    private RotaSLServiceAdapter rotaSLServiceAdapter;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @Mock
    private Response response;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Test
    public void shouldGetJudicialRoles() {
        final String startDate = LocalDate.now().toString();
        final String ouCode = "B01LY00";
        final Optional<String> courtSessionOptional = Optional.of("AM");
        final String courtRoomId = UUID.randomUUID().toString();

        final JsonObject hearingSlotsResponse = FileUtil.givenPayload("/mock-data/azure.rotasl.getHearingSlots.stub-data.json");

        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(hearingSlotsResponse);
        when(hearingSlotsService.search(anyMap())).thenReturn(response);

        final List<JudicialRole> judicialRoleList = rotaSLServiceAdapter.getJudicialRoles(startDate, ouCode, courtSessionOptional, courtRoomId);

        assertThat(judicialRoleList.size(), is(3));

        IntStream.range(0, judicialRoleList.size()).forEach(index -> {
            final JudicialRole judicialRole = judicialRoleList.get(index);

            final JsonObject judiciaryJsonObject = (JsonObject) ((JsonObject)hearingSlotsResponse.getJsonArray("hearingSlots").get(0))
                    .getJsonArray("judiciaries")
                    .get(index);

            assertThat(judicialRole.getJudicialId().toString(), is(judiciaryJsonObject.getString("judiciaryId")));
            assertThat(judicialRole.getIsBenchChairman(), is(Optional.of(judiciaryJsonObject.getBoolean("benchChairman"))));
            assertThat(judicialRole.getIsDeputy(), is(Optional.of(judiciaryJsonObject.getBoolean("deputy"))));
            assertThat(judicialRole.getJudicialRoleType().getJudiciaryType(), is(judiciaryJsonObject.getString("judiciaryType")));
        });

    }

    @Test
    public void shouldGetEmptyListIfThereIsNoMatchingJudicialRolesInRotaSL() {
        final String startDate = LocalDate.now().toString();
        final String ouCode = "B01LY00";
        final Optional<String> courtSessionOptional = Optional.of("AM");
        final String courtRoomId = UUID.randomUUID().toString();

        when(response.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn("entity response");
        when(hearingSlotsService.search(anyMap())).thenReturn(response);

        final List<JudicialRole> judicialRoleList = rotaSLServiceAdapter.getJudicialRoles(startDate, ouCode, courtSessionOptional, courtRoomId);

        assertTrue(CollectionUtils.isEmpty(judicialRoleList));
    }

    @Test
    public void shouldGetHearingSlotResponse() {
        final String startDate = LocalDate.now().toString();
        final String ouCode = "B01LY00";
        final String courtRoomId = "a91a93e6-d704-3cf1-9f20-e267b5a7eeeb";

        final JsonObject hearingSlotsResponse = FileUtil.givenPayload("/mock-data/azure.rotasl.getHearingSlots.stub-data.json");

        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(hearingSlotsResponse);
        when(hearingSlotsService.search(anyMap())).thenReturn(response);

        final Response response = rotaSLServiceAdapter.getHearingSlotResponse(startDate, startDate, ouCode, courtRoomId);

        final JsonObject responseJson = objectToJsonObjectConverter.convert(response.getEntity());
        final JsonObject object = responseJson.getJsonArray("hearingSlots").getValuesAs(JsonObject.class).get(0);

        assertThat(object.getString("panel"), is("YOUTH"));
        assertThat(object.getString("courtRoomId"), is(courtRoomId));
    }
}
