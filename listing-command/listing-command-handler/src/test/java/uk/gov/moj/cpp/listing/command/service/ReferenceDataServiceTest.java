package uk.gov.moj.cpp.listing.command.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    private Enveloper enveloper = createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Before
    public void setup() {
        referenceDataService.setEnveloper(enveloper);
    }


    @Test
    public void getAllCrownCourtCentresSuccessfully() {

        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();
        final JsonEnvelope returnedResponseEnvelope = generateEmptyEnvelope();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(returnedResponseEnvelope);
        ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

        final JsonEnvelope responseEnvelope = referenceDataService.getAllCrownCourtCentres(eventEnvelope);

        verify(requester).requestAsAdmin(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is("referencedata.query.courtrooms"));
        final JsonObject expectedPayload = createObjectBuilder()
                .add("oucodeL1Code", "C")
                .build();
        final JsonObject payloadOfRequestEnvelope = requestEnvelope.payloadAsJsonObject();
        assertThat(payloadOfRequestEnvelope, is(expectedPayload));

        assertThat(responseEnvelope, is(returnedResponseEnvelope));

    }

    private JsonEnvelope generateEmptyEnvelope() {
        return createEnvelope(".", createObjectBuilder().build());
    }
}