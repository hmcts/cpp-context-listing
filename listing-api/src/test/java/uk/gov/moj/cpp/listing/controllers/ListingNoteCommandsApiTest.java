package uk.gov.moj.cpp.listing.controllers;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import uk.gov.moj.cpp.listing.command.note.ListingNoteCommands;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class ListingNoteCommandsApiTest {

    private final ListingNoteCommands commands = mock(ListingNoteCommands.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final ListingNoteCommandsApi api = new ListingNoteCommandsApi(commands, request);

    private final ArgumentCaptor<Metadata> metadata = ArgumentCaptor.forClass(Metadata.class);
    private final ArgumentCaptor<JsonObject> payload = ArgumentCaptor.forClass(JsonObject.class);

    @Test
    void createRunsTheCommandWithTheFrameworkMetadataAndReturns202() {
        final UUID causation = randomUUID();
        request.addHeader("CJSCPPUID", "user-1");
        request.addHeader("CPPSID", "session-1");
        request.addHeader("CPPCLIENTCORRELATIONID", "client-1");
        request.addHeader("CPPCAUSATION", causation.toString());
        request.setContentType("application/vnd.listing.command.create-listing-note+json");

        final var response = api.postCreateListingNote(Map.of("courtRoomId", "a299cf49-d14a-496f-895f-59c46859b4a9",
                "hearingDate", "2026-11-20", "noteDescription", "a note"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
        verify(commands).createNote(metadata.capture(), payload.capture());
        assertThat(metadata.getValue().name()).isEqualTo("listing.command.create-listing-note");
        assertThat(metadata.getValue().id()).isNotNull();
        assertThat(metadata.getValue().userId()).contains("user-1");
        assertThat(metadata.getValue().sessionId()).contains("session-1");
        assertThat(metadata.getValue().clientCorrelationId()).contains("client-1");
        assertThat(metadata.getValue().causation()).isEqualTo(List.of(causation));
        assertThat(metadata.getValue().createdAt()).isEmpty();
        assertThat(payload.getValue().getString("noteDescription")).isEqualTo("a note");
    }

    @Test
    void editIsChosenByContentTypeAndTheNoteIdComesFromThePath() {
        request.setContentType("application/vnd.listing.command.edit-listing-note+json");

        assertThat(api.postListingNote("note-1", Map.of("noteDescription", "edited", "noteId", "ignored")).getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);

        verify(commands).editNote(metadata.capture(), payload.capture());
        assertThat(metadata.getValue().name()).isEqualTo("listing.command.edit-listing-note");
        assertThat(metadata.getValue().userId()).isEmpty();
        assertThat(payload.getValue().getString("noteId")).isEqualTo("note-1");
        assertThat(payload.getValue().getString("noteDescription")).isEqualTo("edited");
    }

    @Test
    void deleteIsChosenByContentType() {
        request.setContentType("application/vnd.listing.command.delete-listing-note+json; charset=UTF-8");

        api.postListingNote("note-1", Map.of());

        verify(commands).deleteNote(metadata.capture(), payload.capture());
        assertThat(metadata.getValue().name()).isEqualTo("listing.command.delete-listing-note");
        assertThat(payload.getValue().getString("noteId")).isEqualTo("note-1");
    }

    @Test
    void anotherMediaTypeOnTheNotePathIsRejected() {
        request.setContentType("application/vnd.listing.command.create-listing-note+json");

        assertThatThrownBy(() -> api.postListingNote("note-1", Map.of())).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(commands);
    }

    @Test
    void anEmptyBodyIsAnEmptyPayload() {
        assertThat(CommandRequests.payload(null, Map.of("a", "b")).getString("a")).isEqualTo("b");
        assertThat(CommandRequests.payload(Map.of("n", Map.of("x", List.of(1, true))), Map.of()).getJsonObject("n")
                .getJsonArray("x").size()).isEqualTo(2);
    }

    @Test
    void actionNameNeedsAVendorJsonMediaType() {
        request.setContentType("application/json");

        assertThatThrownBy(() -> CommandRequests.actionName(request)).isInstanceOf(IllegalArgumentException.class);
        verify(commands, org.mockito.Mockito.never()).createNote(any(), any());
    }
}
