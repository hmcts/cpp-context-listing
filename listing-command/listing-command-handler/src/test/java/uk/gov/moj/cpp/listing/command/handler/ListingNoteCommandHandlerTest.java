package uk.gov.moj.cpp.listing.command.handler;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.command.utils.FileUtil;
import uk.gov.moj.cpp.listing.common.NoteUUIDService;
import uk.gov.moj.cpp.listing.domain.aggregate.ListingNote;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListingNoteCommandHandlerTest {

    private static final UUID NOTE_ID1 = randomUUID();
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private Stream<Object> events;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private ListingNote listingNote;
    @Mock
    private NoteUUIDService noteUUIDService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    @Spy
    private ListingNoteCommandHandler listingNoteCommandHandler;

    @Test
    public void shouldHandleCreateNote() throws Exception {
        final JsonEnvelope commandEnvelope = getJsonEnvelopeForCreateNote();
        when(noteUUIDService.getNoteId(any(UUID.class), any(LocalDate.class))).thenReturn(NOTE_ID1);

        givenEventStream(NOTE_ID1, eventStream, listingNote, ListingNote.class);
        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ListingNote.class)).thenReturn(listingNote);
        when(listingNote.createNote(any(UUID.class), any(UUID.class), anyString(), anyString())).thenReturn(events);
        listingNoteCommandHandler.handleCreateNote(commandEnvelope);
        verify(listingNote, times(1)).createNote(any(UUID.class), any(UUID.class), anyString(), anyString());

    }

    @Test
    public void shouldHandleEditNoteCommand() throws Exception {
        //given
        UUID noteId = NOTE_ID1;
        String noteDescription = "edited note description";
        final JsonEnvelope commandEnvelope = getEnvelopeForEditNoteForListing(noteId, noteDescription);

        givenEventStream(NOTE_ID1, eventStream, listingNote, ListingNote.class);
        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ListingNote.class)).thenReturn(listingNote);
        when(listingNote.editNote(noteId, noteDescription)).thenReturn(events);

        listingNoteCommandHandler.handleEditNote(commandEnvelope);

        verify(listingNote, times(1)).editNote(noteId, noteDescription);
    }

    @Test
    public void shouldHandleDeleteNote() throws Exception {
        final JsonEnvelope commandEnvelope = getJsonEnvelopeForDeleteNote();

        givenEventStream(NOTE_ID1, eventStream, listingNote, ListingNote.class);
        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ListingNote.class)).thenReturn(listingNote);
        when(listingNote.deleteNote(any(UUID.class))).thenReturn(events);
        listingNoteCommandHandler.handleDeleteNote(commandEnvelope);
        verify(listingNote, times(1)).deleteNote(any(UUID.class));

    }

    private JsonEnvelope getJsonEnvelopeForDeleteNote() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.delete-note.json").toString();
        try {
            final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.delete-listing-note", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope getJsonEnvelopeForCreateNote() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.create-note.json").toString();
        try {
            final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.create-listing-note", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends Aggregate> void givenEventStream(final UUID id, final EventStream eventStream, final T aggregate, final Class<T> clz) {
        when(aggregateService.get(eventStream, clz)).thenReturn(aggregate);
    }

    private JsonEnvelope getEnvelopeForEditNoteForListing(final UUID noteId, final String noteDescription) {
        final String requestBody = "{\"noteId\":\"" + noteId + "\",\"noteDescription\":\"" + noteDescription + "\"}";
        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(requestBody));
        return createEnvelope("listing.command.edit-listing-note", jsonReader.readObject());
    }

}
