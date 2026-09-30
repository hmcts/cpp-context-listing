package uk.gov.moj.cpp.listing.query.api.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {
    private static final String REFERENCE_DATA_GET_COURTROOM = "referencedata.query.courtroom";

    private static final String HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final String TYPE = "Sentence";
    private static final Integer ESTIMATED_MINUTES = RandomGenerator.INTEGER.next();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID JUDICIAL_ID = randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();


    @Mock
    private Requester requester;



    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope;

    @InjectMocks
    private ReferenceDataService referenceDataService;


    @Test
    public void shouldGetCourtCentreByIdSuccessfully() {

        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();
        final JsonEnvelope returnedResponseEnvelope = generateEmptyEnvelope();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(returnedResponseEnvelope);

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

    @Test
    public void shouldGetJudiciariesByIdList() {
        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();

        final UUID judiciaryId = randomUUID();

        referenceDataService.getJudiciariesByIdList(asList(judiciaryId) , eventEnvelope);

        verify(requester).requestAsAdmin(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is("referencedata.query.judiciaries"));
    }

    @Test
    public void shouldGetProsecutorById() {
        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();
        final JsonEnvelope returnedResponseEnvelope = generateEmptyEnvelope();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(returnedResponseEnvelope);

        final String prosecutorId = randomUUID().toString();
        final JsonEnvelope responseEnvelope = referenceDataService.getProsecutorById(prosecutorId, eventEnvelope);

        verify(requester).requestAsAdmin(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is("referencedata.query.prosecutor"));
        final JsonObject expectedPayload = createObjectBuilder()
                .add("id", prosecutorId)
                .build();
        final JsonObject payloadOfRequestEnvelope = requestEnvelope.payloadAsJsonObject();
        assertThat(payloadOfRequestEnvelope, is(expectedPayload));

        assertThat(responseEnvelope, is(returnedResponseEnvelope));
    }

    @Test
    public void shouldGetIsHearingLanguageWelsh() {

        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();
        final JsonEnvelope returnedResponseEnvelope = generateEmptyEnvelope();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(returnedResponseEnvelope);

        final UUID courtCentreId = randomUUID();
        final Optional<Boolean> hearingLanguageWelsh = referenceDataService.isHearingLanguageWelsh(eventEnvelope, courtCentreId.toString());

        verify(requester).requestAsAdmin(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is("referencedata.query.courtroom"));
        final JsonObject expectedPayload = createObjectBuilder()
                .add("id", courtCentreId.toString())
                .build();
        final JsonObject payloadOfRequestEnvelope = requestEnvelope.payloadAsJsonObject();
        assertThat(payloadOfRequestEnvelope, is(expectedPayload));

        assertThat(hearingLanguageWelsh.isPresent(), is(true));
        assertThat(hearingLanguageWelsh.get(), is(false));

    }

    @Test
    public void getHearingTypesIdWelshDescriptionMap() {
        final String hearingTypeId = randomUUID().toString();
        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();
        final JsonEnvelope returnedResponseEnvelope = generateHearingTypesIdWelshDescriptionMap(hearingTypeId);
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(returnedResponseEnvelope);

        final Map<String, String> hearingTypesIdWelshDescriptionMap = referenceDataService.getHearingTypesIdWelshDescriptionMap(eventEnvelope);

        verify(requester).requestAsAdmin(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is("referencedata.query.hearing-types"));

        assertThat(hearingTypesIdWelshDescriptionMap.entrySet(), hasSize(1));
        assertThat(hearingTypesIdWelshDescriptionMap.get(hearingTypeId), is("welshtest"));
    }

    private JsonEnvelope generateEmptyEnvelope() {
        return createEnvelope(".", createObjectBuilder().build());
    }

    private JsonEnvelope generateHearingTypesIdWelshDescriptionMap(final String hearingTypeId) {
        return createEnvelope(".", createObjectBuilder()
                .add("hearingTypes", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", hearingTypeId)
                                .add("welshHearingDescription", "welshtest")
                                .build()).build())
                .build());
    }
}