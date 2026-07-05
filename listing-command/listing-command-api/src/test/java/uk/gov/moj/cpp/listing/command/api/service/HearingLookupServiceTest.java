package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HearingLookupServiceTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private HearingQueryView hearingQueryView;

    @Mock
    private JsonEnvelope envelope;

    @InjectMocks
    private HearingLookupService hearingLookupService;

    @Test
    void shouldReturnHearingWhenFound() {
        final UUID hearingId = randomUUID();
        final JsonObject hearingPayload = Json.createObjectBuilder()
                .add("id", hearingId.toString())
                .add("jurisdictionType", "MAGISTRATES")
                .build();

        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());
        when(hearingQueryView.getHearingById(any(JsonEnvelope.class)))
                .thenReturn(JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName().build(), hearingPayload));

        final Optional<JsonObject> result = hearingLookupService.findHearing(hearingId, envelope);

        assertTrue(result.isPresent());
        assertThat(result.get().getString("jurisdictionType"), is("MAGISTRATES"));
    }

    @Test
    void shouldReturnEmptyWhenHearingNotFound() {
        final UUID hearingId = randomUUID();

        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());
        when(hearingQueryView.getHearingById(any(JsonEnvelope.class)))
                .thenThrow(new NotFoundException("no hearing"));

        final Optional<JsonObject> result = hearingLookupService.findHearing(hearingId, envelope);

        assertTrue(result.isEmpty());
    }
}
