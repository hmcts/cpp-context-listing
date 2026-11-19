package uk.gov.moj.cpp.listing.command.api.outbox;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.listing.command.api.outbox.EventOutboxPublisher.MAX_ATTEMPTS;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository.OutboxEntry;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventOutboxPublisherTest {

    private static final JsonEnvelope EVENT = envelopeFrom(
            metadataBuilder().withId(UUID.randomUUID()).withName("listing.events.created-listing-note"),
            JsonObjects.createObjectBuilder());

    @Mock
    private EventOutboxJdbcRepository eventOutboxJdbcRepository;

    @Mock
    private EventProcessorInvoker eventProcessorInvoker;

    @Mock
    private JsonObjectEnvelopeConverter jsonObjectEnvelopeConverter;

    @InjectMocks
    private EventOutboxPublisher publisher;

    @Test
    void deliversTheOldestEntryToTheProcessorThenDeletesIt() {
        givenNextEntry(new OutboxEntry(4L, "json", 0));

        assertThat(publisher.deliverNext(), is(true));

        final InOrder inOrder = inOrder(eventProcessorInvoker, eventOutboxJdbcRepository);
        inOrder.verify(eventProcessorInvoker).process(EVENT);
        inOrder.verify(eventOutboxJdbcRepository).delete(List.of(4L));
        verify(eventOutboxJdbcRepository, never()).incrementAttempts(anyLong());
    }

    @Test
    void countsAFailedAttemptAndKeepsTheEntryForRetry() {
        givenNextEntry(new OutboxEntry(4L, "json", 0));
        doThrow(new IllegalStateException("processor failed")).when(eventProcessorInvoker).process(EVENT);

        assertThat(publisher.deliverNext(), is(true));

        verify(eventOutboxJdbcRepository).incrementAttempts(4L);
        verify(eventOutboxJdbcRepository, never()).delete(anyList());
    }

    @Test
    void dropsTheEntryWhenTheLastAttemptFails() {
        givenNextEntry(new OutboxEntry(4L, "json", MAX_ATTEMPTS - 1));
        doThrow(new IllegalStateException("processor failed")).when(eventProcessorInvoker).process(EVENT);

        assertThat(publisher.deliverNext(), is(true));

        verify(eventOutboxJdbcRepository).delete(List.of(4L));
        verify(eventOutboxJdbcRepository, never()).incrementAttempts(anyLong());
    }

    @Test
    void doesNothingWhenAnotherRelayHoldsTheLock() {
        when(eventOutboxJdbcRepository.tryLockForRelay()).thenReturn(false);

        assertThat(publisher.deliverNext(), is(false));

        verify(eventOutboxJdbcRepository, never()).nextBatch(anyInt());
        verifyNoInteractions(eventProcessorInvoker);
    }

    @Test
    void doesNothingWhenTheOutboxIsEmpty() {
        when(eventOutboxJdbcRepository.tryLockForRelay()).thenReturn(true);
        when(eventOutboxJdbcRepository.nextBatch(1)).thenReturn(List.of());

        assertThat(publisher.deliverNext(), is(false));

        verifyNoInteractions(eventProcessorInvoker);
    }

    private void givenNextEntry(final OutboxEntry entry) {
        when(eventOutboxJdbcRepository.tryLockForRelay()).thenReturn(true);
        when(eventOutboxJdbcRepository.nextBatch(1)).thenReturn(List.of(entry));
        when(jsonObjectEnvelopeConverter.asEnvelope(entry.envelopeJson())).thenReturn(EVENT);
    }
}
