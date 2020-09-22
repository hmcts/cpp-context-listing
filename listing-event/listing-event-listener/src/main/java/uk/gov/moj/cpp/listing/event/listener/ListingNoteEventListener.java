package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.CreatedListingNote;
import uk.gov.justice.listing.events.DeletedListingNote;
import uk.gov.justice.listing.events.ListingNoteEdited;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.NotesRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class ListingNoteEventListener {
    @Inject
    private NotesRepository notesRepository;

    @Handles("listing.events.created-listing-note")
    public void handleCreateListingNote(final Envelope<CreatedListingNote> event) {
        final CreatedListingNote createdListingNotes = event.payload();
        final LocalDate date = LocalDate.parse(createdListingNotes.getHearingDate());
        final List<Notes> noteByCourtRoomAndDate = notesRepository.findByCourtRoomIdAndDate(
                createdListingNotes.getCourtRoomId(),
                date);
        if (noteByCourtRoomAndDate.isEmpty()) {
            final Notes notes = new Notes(createdListingNotes.getId(), createdListingNotes.getCourtRoomId(), date, createdListingNotes.getNoteDescription());
            notesRepository.save(notes);
        }
    }

    @Handles("listing.events.listing-note-edited")
    public void handleListingNoteEditedEvent(final Envelope<ListingNoteEdited> event) {
        final ListingNoteEdited noteEdited = event.payload();

        final Optional<Notes> optionalById = Optional.ofNullable(notesRepository.findOptionalById(noteEdited.getNoteId()));

        optionalById.ifPresent(note -> {
                    note.setNote(noteEdited.getNoteDescription());
                    notesRepository.save(note);
                }
        );
    }

    @Handles("listing.events.deleted-listing-note")
    public void handleDeleteListingNote(final Envelope<DeletedListingNote> event) {
        final DeletedListingNote deletedListingNote = event.payload();
        final Notes byNoteId = notesRepository.findBy(deletedListingNote.getNoteId());
        if (Objects.nonNull(byNoteId)) {
            notesRepository.remove(byNoteId);
        }
    }
}