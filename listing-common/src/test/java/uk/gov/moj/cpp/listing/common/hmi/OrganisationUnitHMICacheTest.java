package uk.gov.moj.cpp.listing.common.hmi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.common.utils.FileUtil.givenPayload;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.staginghmi.OrganisationUnitHmiStatus;
import uk.gov.moj.cpp.staginghmi.common.azure.OrganisationUnitHMIStatusService;

import java.util.Optional;
import java.util.Set;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OrganisationUnitHMICacheTest {

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Response response;

    @Mock
    private OrganisationUnitHMIStatusService organisationUnitHMIStatusService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @InjectMocks
    private OrganisationUnitHMICache organisationUnitHMICache;

    @Test
    public void shouldReturnNotHmiEnabledCourtCentreIds(){

        OrganisationUnitHmiStatus hmiEnabled = new OrganisationUnitHmiStatus("oucode1", true, true, true,
                "2022-01-01", "courtCenter1");

        OrganisationUnitHmiStatus notHmiEnabled = new OrganisationUnitHmiStatus("oucode2", false, false, false,
                "2022-01-01", "courtCenter2");

        OrganisationUnitHmiStatus hmiEnabled2 = new OrganisationUnitHmiStatus("oucode2", true, true, true,
                "2022-01-01", "courtCenter3");

        final JsonObject hmiList = givenPayload("/mock-data/staginghmi.query.organisation-unit-hmi-status-rota.json");

        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(hmiList);
        when(organisationUnitHMIStatusService.getAllOrganisationUnitHMIStatus()).thenReturn(response);

        when(objectToJsonObjectConverter.convert(response.getEntity())).thenReturn(hmiList);

        when(jsonObjectConverter.convert(hmiList.getJsonArray("organisationUnitHMIStatus").getJsonObject(0), OrganisationUnitHmiStatus.class))
                .thenReturn(hmiEnabled);

        when(jsonObjectConverter.convert(hmiList.getJsonArray("organisationUnitHMIStatus").getJsonObject(1), OrganisationUnitHmiStatus.class))
                .thenReturn(notHmiEnabled);

        when(jsonObjectConverter.convert(hmiList.getJsonArray("organisationUnitHMIStatus").getJsonObject(2), OrganisationUnitHmiStatus.class))
                .thenReturn(hmiEnabled2);

        Set<String>  ids = organisationUnitHMICache.getNotHmiEnabledCourtCentreIdSet();
        assertThat(ids.size(), is(1));
        assertThat(ids.stream().map(Optional::ofNullable).findFirst().get().get(), is("courtCenter2"));
    }

}
