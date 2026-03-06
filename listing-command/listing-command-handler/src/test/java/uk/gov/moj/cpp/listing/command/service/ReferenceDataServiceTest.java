package uk.gov.moj.cpp.listing.command.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    private Enveloper enveloper = createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @BeforeEach
    public void setup() {
        referenceDataService.setEnveloper(enveloper);
    }

    @Test
    public void getAllHearingTypes() {

        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();
        final JsonEnvelope returnedResponseEnvelope = generateEmptyEnvelope();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(returnedResponseEnvelope);
        ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

        final JsonEnvelope responseEnvelope = referenceDataService.getHearingTypes(eventEnvelope);

        verify(requester).requestAsAdmin(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is("referencedata.query.all-hearing-types"));
        final JsonObject payloadOfRequestEnvelope = requestEnvelope.payloadAsJsonObject();
        assertThat(payloadOfRequestEnvelope, notNullValue());

        assertThat(responseEnvelope, is(returnedResponseEnvelope));

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

    @Test
    public void getAllCourtRoomsSuccessfully() {

        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();
        final JsonEnvelope returnedResponseEnvelope = generateEmptyEnvelope();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(returnedResponseEnvelope);
        ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

        final JsonEnvelope responseEnvelope = referenceDataService.getAllCourtRooms(eventEnvelope);

        verify(requester).requestAsAdmin(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is("referencedata.query.courtrooms"));
        final JsonObject expectedPayload = createObjectBuilder()
                .build();
        final JsonObject payloadOfRequestEnvelope = requestEnvelope.payloadAsJsonObject();
        assertThat(payloadOfRequestEnvelope, is(expectedPayload));

        assertThat(responseEnvelope, is(returnedResponseEnvelope));

    }

    @Test
    public void getCourtCentreByIdSuccessfully() {

        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();
        final JsonEnvelope returnedResponseEnvelope = generateEmptyEnvelope();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(returnedResponseEnvelope);
        ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

        final UUID courtCentreId = randomUUID();
        final JsonEnvelope responseEnvelope = referenceDataService.getCourtCentreById(courtCentreId, eventEnvelope);

        verify(requester).requestAsAdmin(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is("referencedata.query.courtroom"));
        final JsonObject expectedPayload = createObjectBuilder()
                .add("id", courtCentreId.toString())
                .build();
        final JsonObject payloadOfRequestEnvelope = requestEnvelope.payloadAsJsonObject();
        assertThat(payloadOfRequestEnvelope, is(expectedPayload));

        assertThat(responseEnvelope, is(returnedResponseEnvelope));

    }

    private JsonEnvelope generateEmptyEnvelope() {
        return createEnvelope(".", createObjectBuilder().build());
    }
}