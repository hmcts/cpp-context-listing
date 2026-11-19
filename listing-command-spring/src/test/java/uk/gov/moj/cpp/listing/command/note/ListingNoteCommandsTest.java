package uk.gov.moj.cpp.listing.command.note;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.command.recorder.EventRecorder;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.common.domain.note.ListingNoteService;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.util.List;
import java.util.UUID;

import jakarta.json.Json;
import org.junit.jupiter.api.Test;

class ListingNoteCommandsTest {

    private final ListingNoteService service = mock(ListingNoteService.class);
    private final EventRecorder recorder = mock(EventRecorder.class);
    private final ListingNoteCommands commands = new ListingNoteCommands(service, recorder);
    private final Metadata command = Metadata.builder().withId(randomUUID()).withName("listing.command.x").build();
    private final List<DomainEvent> events = List.of(new DomainEvent("listing.events.x", null, Json.createObjectBuilder().build()));

    @Test
    void createRecordsTheServicesEvents() {
        final UUID courtRoomId = randomUUID();
        when(service.createNote(courtRoomId, "2026-11-20", "a note")).thenReturn(events);

        commands.createNote(command, Json.createObjectBuilder()
                .add("courtRoomId", courtRoomId.toString()).add("hearingDate", "2026-11-20").add("noteDescription", "a note").build());

        verify(recorder).record(command, events);
    }

    @Test
    void editRecordsTheServicesEvents() {
        final UUID noteId = randomUUID();
        when(service.editNote(noteId, "edited")).thenReturn(events);

        commands.editNote(command, Json.createObjectBuilder().add("noteId", noteId.toString()).add("noteDescription", "edited").build());

        verify(recorder).record(command, events);
    }

    @Test
    void deleteRecordsTheServicesEvents() {
        final UUID noteId = randomUUID();
        when(service.deleteNote(noteId)).thenReturn(events);

        commands.deleteNote(command, Json.createObjectBuilder().add("noteId", noteId.toString()).build());

        verify(recorder).record(command, events);
    }
}
