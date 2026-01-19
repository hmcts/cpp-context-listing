package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import uk.gov.justice.services.messaging.JsonObjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    @Mock
    private Enveloper enveloper;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Test
    public void shouldGetCourtCentreById(){
        final UUID courtCentreId = randomUUID();
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        final JsonEnvelope command = mock(JsonEnvelope.class);
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("referencedata.query.courtroom");
        when(command.metadata()).thenReturn(metadataBuilder.build());

        referenceDataService.getCourtCentreById(courtCentreId, command);

        verify(requester).requestAsAdmin(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("referencedata.query.courtroom"));
    }

    @Test
    public void shouldGetCourtCentreSById(){
        final UUID courtCentreId = randomUUID();
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        final JsonEnvelope command = mock(JsonEnvelope.class);
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("referencedata.query.courtrooms");
        when(command.metadata()).thenReturn(metadataBuilder.build());

        referenceDataService.getCourtCentresById(Set.of(courtCentreId), command);

        verify(requester).requestAsAdmin(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("referencedata.query.courtrooms"));
    }

    @Test
    public void shouldGetOrganizationUnitById(){
        final UUID courtCentreId = randomUUID();
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        final JsonEnvelope command = mock(JsonEnvelope.class);
        final JsonEnvelope response = mock(JsonEnvelope.class);
        Function<Object, JsonEnvelope> jsonEnvelopeFunction = new Function<Object, JsonEnvelope>() {
            @Override
            public JsonEnvelope apply(final Object o) {
                return command;
            }
        };

        final UUID id = randomUUID();
        final OrganisationUnit organisationUnit = new OrganisationUnit(id, "oucode");

        when(enveloper.withMetadataFrom(any(), any())).thenReturn(jsonEnvelopeFunction);
        when(requester.request(command)).thenReturn(response);
        when(response.payloadAsJsonObject()).thenReturn(JsonObjects.createObjectBuilder().build());
        when(jsonObjectConverter.convert(any(), any())).thenReturn(organisationUnit);

        final OrganisationUnit responseOrganisationUnit = referenceDataService.getOrganizationUnitById(courtCentreId, command);

        verify(requester).request(senderJsonEnvelopeCaptor.capture());
        assertThat(responseOrganisationUnit.getId(), is(id));
        assertThat(responseOrganisationUnit.getOucode(), is("oucode"));
    }
}
