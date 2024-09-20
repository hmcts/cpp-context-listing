package uk.gov.moj.cpp.listing.event.processor;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.listing.event.processor.ListingNoteEventProcessor.PUBLIC_LISTING_NOTE_EDITED;

import uk.gov.justice.listing.events.CreatedListingNote;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListingNoteEventProcessorTest {

    private static final String PUBLIC_LISTING_CREATED_LISTING_NOTE = "public.listing.created-listing-note";
    private static final String PUBLIC_LISTING_DELETED_LISTING_NOTE = "public.listing.deleted-listing-note";
    public static final String COURT_ROOM_ID = "courtRoomId";
    public static final String HEARING_DATE = "hearingDate";
    public static final String NOTE_DESCRIPTION = "noteDescription";
    public static final String NOTE_DESCRIPTION_TEXT = "note description";
    public static final String NOTE_ID = "id";
    public static final String DATE = "date";
    public static final String NOTE = "note";


    @InjectMocks
    private ListingNoteEventProcessor listingNoteEventProcessor;

    @Mock
    private Sender sender;

    private JsonEnvelope envelope;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Test
    public void handlesNoteCreatedForListing() {
        String noteId = randomUUID().toString();
        String courtRoomId = randomUUID().toString();
        final JsonObject payLoad = Json.createObjectBuilder()
                .add(NOTE_ID, noteId)
                .add(COURT_ROOM_ID, courtRoomId)
                .add(HEARING_DATE, now().toString())
                .add(NOTE_DESCRIPTION, NOTE_DESCRIPTION_TEXT)
                .build();
        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), payLoad);

        when(jsonObjectConverter.convert(payLoad, CreatedListingNote.class)).thenReturn(formCreatedListingNoteObject(payLoad));

        listingNoteEventProcessor.handlesNoteCreatedForListing(envelope);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_LISTING_CREATED_LISTING_NOTE));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString(NOTE_ID), is(noteId));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString(DATE), is(now().toString()));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString(COURT_ROOM_ID), is(courtRoomId));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString(NOTE), is(NOTE_DESCRIPTION_TEXT));
    }

    private CreatedListingNote formCreatedListingNoteObject(JsonObject payLoad) {
        return CreatedListingNote.createdListingNote()
                .withId(UUID.fromString(payLoad.getString(NOTE_ID)))
                .withCourtRoomId(UUID.fromString(payLoad.getString(COURT_ROOM_ID)))
                .withHearingDate(payLoad.getString(HEARING_DATE))
                .withNoteDescription(payLoad.getString(NOTE_DESCRIPTION))
                .build();
    }

    @Test
    public void shouldHandleListingNoteEditedEvent() {

        //given
        UUID notedId = UUID.randomUUID();
        final JsonObject payLoad = Json.createObjectBuilder()
                .add("noteId", notedId.toString())
                .add("noteDescription", "edited note")
                .build();
        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), payLoad);

        //when
        listingNoteEventProcessor.handleListingNoteEditedEvent(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(),
                is(PUBLIC_LISTING_NOTE_EDITED));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("noteId"),
                is(notedId.toString()));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("noteDescription"),
                is("edited note"));

    }


    @Test
    public void handlesNoteDeletedForListing() {
        final JsonObject payLoad = Json.createObjectBuilder()
                .add("id", randomUUID().toString())
                .build();
        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), payLoad);

        listingNoteEventProcessor.handlesNoteDeletedForListing(envelope);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_LISTING_DELETED_LISTING_NOTE));
    }
}