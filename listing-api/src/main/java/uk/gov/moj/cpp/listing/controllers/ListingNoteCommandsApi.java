package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.command.note.ListingNoteCommands.CREATE_LISTING_NOTE;
import static uk.gov.moj.cpp.listing.command.note.ListingNoteCommands.DELETE_LISTING_NOTE;
import static uk.gov.moj.cpp.listing.command.note.ListingNoteCommands.EDIT_LISTING_NOTE;
import static uk.gov.moj.cpp.listing.controllers.CommandRequests.accepted;
import static uk.gov.moj.cpp.listing.controllers.CommandRequests.actionName;
import static uk.gov.moj.cpp.listing.controllers.CommandRequests.metadata;
import static uk.gov.moj.cpp.listing.controllers.CommandRequests.payload;

import uk.gov.moj.cpp.listing.command.note.ListingNoteCommands;
import uk.gov.moj.cpp.listing.openapi.api.ListingNoteCommandsOpenApi;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /listing-note} (create) and {@code POST /listing-notes/{noteId}} (edit or delete,
 * chosen by the {@code Content-Type}), MBD refactor P2.3.0. Replaces the three
 * {@code ListingCommandApi} handlers of slice P1a. The command runs synchronously in the request
 * (D5) and the response is {@code 202 Accepted} with no body, as before.
 */
@RestController
public class ListingNoteCommandsApi implements ListingNoteCommandsOpenApi {

    private final ListingNoteCommands commands;
    private final HttpServletRequest request;

    public ListingNoteCommandsApi(final ListingNoteCommands commands, final HttpServletRequest request) {
        this.commands = commands;
        this.request = request;
    }

    @Override
    public ResponseEntity<Void> postCreateListingNote(final Map<String, Object> requestBody) {
        commands.createNote(metadata(request, CREATE_LISTING_NOTE), payload(requestBody, Map.of()));
        return accepted();
    }

    @Override
    public ResponseEntity<Void> postListingNote(final String noteId, final Map<String, Object> requestBody) {
        final String action = actionName(request);
        final var payload = payload(requestBody, Map.of("noteId", noteId));
        switch (action) {
            case EDIT_LISTING_NOTE -> commands.editNote(metadata(request, action), payload);
            case DELETE_LISTING_NOTE -> commands.deleteNote(metadata(request, action), payload);
            default -> throw new IllegalStateException("Unexpected media type for /listing-notes/{noteId}: " + action);
        }
        return accepted();
    }
}
