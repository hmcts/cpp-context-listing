package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListingReferenceDataServiceTest {

    private static final String REFERENCE_DATA_GET_COURTROOM = "referencedata.query.courtrooms";

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
    private ArgumentCaptor<Envelope> requestCaptor;

    @InjectMocks
    private ListingReferenceDataService listingReferenceDataService;

    @Test
    public void shouldReturnCourtRoomReferenceDataDetails(){
        final JsonEnvelope event = hearingAllocatedEvent();
        final String courtCentreId = randomUUID().toString();

        final JsonObject courtRooms = getPayloadForCourtRooms(courtCentreId);
        when(requester.request(any()))
                .thenReturn(envelopeFrom(metadataWithRandomUUID(REFERENCE_DATA_GET_COURTROOM), courtRooms));

        final JsonEnvelope jsonEnvelope = listingReferenceDataService.getPayLoadForCourtRoom(event, courtCentreId);

        verify(requester).request(requestCaptor.capture());
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("oucode"), is(courtRooms.getString("oucode")));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("courtrooms").size(), is(1));
    }

    @Test
    public void shouldRetrieveCourtRoomId() {
        final String courtCentreId = randomUUID().toString();

        final JsonObject payloadForCourtRooms = getPayloadForCourtRooms(courtCentreId);

        final int courtRoomId = listingReferenceDataService.retrieveCourtRoomId(payloadForCourtRooms, COURT_ROOM_ID, COURT_CENTRE_ID);

        assertThat(courtRoomId, is(payloadForCourtRooms.getJsonArray("courtrooms").getJsonObject(0).getInt("courtroomId")));
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
                .add("hearingDate", hearingDate.build());

        return envelopeFrom(metadataWithDefaults().withName(HEARING_ALLOCATED_FOR_LISTING), hearingAllocated);
    }

    private JsonObject getPayloadForCourtRooms(String id) {
        return Json.createObjectBuilder()
                .add("id", id)
                .add("oucode", "B01LY00")
                .add("oucodeL3Name", "South Western (Lavender Hill)")
                .add("oucodeL3WelshName", "welshName_Test")
                .add("courtrooms", getCourtRooms())
                .build();
    }

    private JsonArray getCourtRooms() {
        return createArrayBuilder().add(createObjectBuilder()
                .add("id", COURT_ROOM_ID.toString())
                .add("venueName", "BEXLEY MAGISTRATES' COURT")
                .add("courtroomId", 12)
                .add("courtroomName", "Courtroom 01"))
                .build();
    }
}