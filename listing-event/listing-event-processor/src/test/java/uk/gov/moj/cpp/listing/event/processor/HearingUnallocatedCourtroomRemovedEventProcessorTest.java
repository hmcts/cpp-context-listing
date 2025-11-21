package uk.gov.moj.cpp.listing.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

@ExtendWith(MockitoExtension.class)
public class HearingUnallocatedCourtroomRemovedEventProcessorTest {

    private static final String PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED =
            "public.listing.hearing-unallocated-courtroom-removed";

    @InjectMocks
    private HearingUnallocatedCourtroomRemovedEventProcessor courtRoomForHearingEventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Test
    public void shouldHandleHearingUnallocatedCourtroomRemoved() {
        final UUID hearingId = randomUUID();
        final int estimatedMinutes = 60;

        final JsonObject payLoad = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("estimatedMinutes", estimatedMinutes)
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), payLoad);

        final HearingUnallocatedCourtroomRemoved hearingUnallocatedCourtroomRemoved =
                HearingUnallocatedCourtroomRemoved.hearingUnallocatedCourtroomRemoved()
                        .withHearingId(hearingId)
                        .withEstimatedMinutes(estimatedMinutes)
                        .build();

        when(jsonObjectConverter.convert(payLoad, HearingUnallocatedCourtroomRemoved.class))
                .thenReturn(hearingUnallocatedCourtroomRemoved);

        courtRoomForHearingEventProcessor.handleHearingUnallocatedCourtroomRemoved(envelope);

        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope publicEvent = senderJsonEnvelopeCaptor.getValue();
        assertThat(publicEvent.metadata().name(), is(PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED));
        assertThat(publicEvent.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(publicEvent.payloadAsJsonObject().getInt("estimatedMinutes"), is(60));
    }

}
