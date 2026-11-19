package uk.gov.moj.cpp.listing.command.hearing.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;
import uk.gov.moj.cpp.listing.query.view.exception.QueryNotFoundException;

import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Port of the JEE {@code HearingLookupServiceTest} (MBD refactor P2.3.2). */
class HearingLookupServiceTest {

    private final HearingQueryView hearingQueryView = mock(HearingQueryView.class);
    private final HearingLookupService hearingLookupService = new HearingLookupService(hearingQueryView);
    private final Envelope envelope = mock(Envelope.class);

    @Test
    void shouldReturnHearingWhenFound() {
        final UUID hearingId = randomUUID();
        final JsonObject hearingPayload = Json.createObjectBuilder()
                .add("id", hearingId.toString())
                .add("jurisdictionType", "MAGISTRATES")
                .build();
        when(hearingQueryView.getHearingById(any(JsonObject.class))).thenReturn(hearingPayload);

        final Optional<JsonObject> result = hearingLookupService.findHearing(hearingId, envelope);

        assertTrue(result.isPresent());
        assertThat(result.get().getString("jurisdictionType"), is("MAGISTRATES"));
        final ArgumentCaptor<JsonObject> query = ArgumentCaptor.forClass(JsonObject.class);
        verify(hearingQueryView).getHearingById(query.capture());
        assertThat(query.getValue().getString("id"), is(hearingId.toString()));
    }

    @Test
    void shouldReturnEmptyWhenHearingNotFound() {
        when(hearingQueryView.getHearingById(any(JsonObject.class))).thenThrow(new QueryNotFoundException("no hearing"));

        assertTrue(hearingLookupService.findHearing(randomUUID(), envelope).isEmpty());
    }
}
