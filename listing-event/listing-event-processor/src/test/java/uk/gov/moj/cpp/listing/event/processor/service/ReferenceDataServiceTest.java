package uk.gov.moj.cpp.listing.event.processor.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.events.OrganisationUnit.organisationUnit;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.listing.events.OrganisationUnit;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.function.Function;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.hamcrest.Matchers;
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
    private Function<Object, JsonEnvelope> function;

    @Mock
    private Requester requester;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Envelope<OrganisationUnit> organisationUnitEnvelope;

    @Captor
    private ArgumentCaptor<Envelope> requestCaptor;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    public void shouldGetOrganizationUnitById() {
        final JsonEnvelope event = hearingAllocatedEvent();
        final UUID courtCentreId = randomUUID();
        final UUID orgId = randomUUID();

        final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                .add("id", orgId.toString())
                .add("address1", orgId.toString())
                .build();

        when(enveloper.withMetadataFrom(eq(event), eq("referencedata.query.organisation-unit")))
                .thenReturn(function);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID(REFERENCE_DATA_GET_COURTROOM), jsonObject);
        when(requester.request(any())).thenReturn(jsonEnvelope);
        when(jsonObjectConverter.convert(any(), eq(OrganisationUnit.class)))
                .thenReturn(organisationUnit().withId(orgId.toString()).withAddress1("address1").build());

        final OrganisationUnit result = referenceDataService.getOrganizationUnitById(courtCentreId, event);

        assertThat(result.getId(), is(orgId.toString()));
        assertThat(result.getAddress1(), is("address1"));
    }

    @Test
    public void getOrganizationUnitByIdWithAdmin() {
        final JsonEnvelope event = hearingAllocatedEvent();
        final UUID courtCentreId = randomUUID();
        final UUID orgId = randomUUID();

        when(enveloper.withMetadataFrom(eq(event), eq("referencedata.query.organisation-unit")))
                .thenReturn(function);
        when(requester.requestAsAdmin(any(), eq(OrganisationUnit.class))).thenReturn(organisationUnitEnvelope);
        when(organisationUnitEnvelope.payload()).thenReturn(organisationUnit().withId(orgId.toString()).withAddress1("address1").build());

        final OrganisationUnit result = referenceDataService.getOrganizationUnitByIdWithAdmin(courtCentreId, event);

        assertThat(result.getId(), is(orgId.toString()));
        assertThat(result.getAddress1(), is("address1"));
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
        assertThat(requestEnvelope.metadata().name(), Matchers.is("referencedata.query.courtroom"));
        final JsonObject expectedPayload = createObjectBuilder()
                .add("id", courtCentreId.toString())
                .build();
        final JsonObject payloadOfRequestEnvelope = requestEnvelope.payloadAsJsonObject();
        assertThat(payloadOfRequestEnvelope, Matchers.is(expectedPayload));

        assertThat(responseEnvelope, Matchers.is(returnedResponseEnvelope));

    }

    @Test
    public void getOucodeFromEnvelope() {
        final JsonEnvelope event = hearingAllocatedEvent();
        final UUID courtCentreId = randomUUID();

        final JsonEnvelope returnedResponseEnvelope = hearingAllocatedEvent();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(returnedResponseEnvelope);
        ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

        final String oucodeFromEnvelope = referenceDataService.getOucodeFromEnvelope(courtCentreId, event);

        assertThat(oucodeFromEnvelope, is("testOuCode"));
    }

    private JsonEnvelope hearingAllocatedEvent() {

        final JsonObjectBuilder hearingDate = createObjectBuilder()
                .add("startDate", START_DATE.toString())
                .add("startTime", START_TIME.toString());

        final JsonObjectBuilder hearingAllocated = createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", TYPE)
                .add("estimatedMinutes", ESTIMATED_MINUTES)
                .add("judgeId", JUDICIAL_ID.toString())
                .add("courtCentre", COURT_CENTRE_ID.toString())
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("oucode","testOuCode")
                .add("hearingDate", hearingDate.build());

        return envelopeFrom(metadataWithDefaults().withName(HEARING_ALLOCATED_FOR_LISTING), hearingAllocated);
    }

    private JsonEnvelope generateEmptyEnvelope() {
        return createEnvelope(".", createObjectBuilder().build());
    }
}