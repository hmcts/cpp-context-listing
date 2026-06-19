package uk.gov.moj.cpp.listing.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.CreatedListingNote;
import uk.gov.justice.listing.events.DeletedListingNote;
import uk.gov.justice.listing.events.ListingNoteEdited;

import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListingNote implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingNote.class);
    private static final long serialVersionUID = 2011122244432L;

    private boolean exists = false;

    @Override
    public Object apply(Object event) {
        return match(event)
                .with(
                        when(CreatedListingNote.class).apply(e -> exists = true),
                        when(ListingNoteEdited.class).apply(this::doNothing),
                        when(DeletedListingNote.class).apply(e -> exists = false),
                        when(Object.class).apply(this::doNothing)
                );
    }

    public Stream<Object> editNote(final UUID id, final String noteDescription) {
        if (exists) {
            return apply(Stream.of(new ListingNoteEdited(noteDescription, id)));
        }
        LOGGER.error("Invalid note id supplied to edit listing note  {} ", id);
        return Stream.empty();
    }

    public Stream<Object> createNote(final UUID id, final UUID courtRoomId, final String date, final String noteDescription) {
        if (!exists) {
            return apply(Stream.of(new CreatedListingNote(courtRoomId, date, id, noteDescription)));
        }
        LOGGER.error("Note already exists with noteId {} ", id);
        return Stream.empty();
    }

    public Stream<Object> deleteNote(final UUID id) {
        if (exists) {
            return apply(Stream.of(new DeletedListingNote(id)));
        }
        LOGGER.error("Invalid note id supplied to delete listing note  {} ", id);
        return Stream.empty();
    }

    @SuppressWarnings("squid:S1172")
    private void doNothing(Object e) {
        // do nothing
    }

}
