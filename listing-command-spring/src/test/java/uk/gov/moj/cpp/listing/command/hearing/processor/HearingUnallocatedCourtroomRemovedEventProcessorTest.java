package uk.gov.moj.cpp.listing.command.hearing.processor;

import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;

import static java.util.UUID.randomUUID;
import static jakarta.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.hearing.processor.ProcessorTestSupport.metadataWithRandomUUIDAndName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.UUID;

import jakarta.json.JsonObject;

@ExtendWith(MockitoExtension.class)
public class HearingUnallocatedCourtroomRemovedEventProcessorTest {

    private static final String PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED =
            "public.listing.hearing-unallocated-courtroom-removed";

    @InjectMocks
    private HearingUnallocatedCourtroomRemovedEventProcessor courtRoomForHearingEventProcessor;

    @Mock
    private ProcessorSender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Captor
    private ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor;

    @Test
    public void shouldHandleHearingUnallocatedCourtroomRemoved() {
        final UUID hearingId = randomUUID();
        final int estimatedMinutes = 60;

        final JsonObject payLoad = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("estimatedMinutes", estimatedMinutes)
                .build();

        final Envelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), payLoad);

        final HearingUnallocatedCourtroomRemoved hearingUnallocatedCourtroomRemoved =
                HearingUnallocatedCourtroomRemoved.hearingUnallocatedCourtroomRemoved()
                        .withHearingId(hearingId)
                        .withEstimatedMinutes(estimatedMinutes)
                        .build();

        when(jsonObjectConverter.convert(payLoad, HearingUnallocatedCourtroomRemoved.class))
                .thenReturn(hearingUnallocatedCourtroomRemoved);

        courtRoomForHearingEventProcessor.handleHearingUnallocatedCourtroomRemoved(envelope);

        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());

        final Envelope publicEvent = senderJsonEnvelopeCaptor.getValue();
        assertThat(publicEvent.metadata().name(), is(PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED));
        assertThat(publicEvent.payload().getString("hearingId"), is(hearingId.toString()));
        assertThat(publicEvent.payload().getInt("estimatedMinutes"), is(60));
    }

}
