package uk.gov.moj.cpp.listing.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.messaging.config.MessagingProperties;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeCodec;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.messaging.outbox.OutboxDeliverer.Result;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository.OutboxEntry;

import java.util.List;
import java.util.UUID;

import jakarta.json.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class OutboxDelivererTest {

    private final EventOutboxJdbcRepository repository = mock(EventOutboxJdbcRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final OutboxRoute publicRoute = mock(OutboxRoute.class);
    private final OutboxRoute inProcessRoute = mock(OutboxRoute.class);
    private final EnvelopeCodec codec = new EnvelopeCodec();
    private final MessagingProperties properties = new MessagingProperties();
    private OutboxDeliverer deliverer;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(publicRoute.handles(anyString())).thenAnswer(invocation -> invocation.<String>getArgument(0).startsWith("public."));
        when(inProcessRoute.handles(anyString())).thenAnswer(invocation -> invocation.getArgument(0).equals("listing.events.handled"));
        properties.getOutbox().getRelay().setMaxAttempts(3);
        deliverer = new OutboxDeliverer(repository, codec, List.of(publicRoute, inProcessRoute), transactionManager, properties);
        when(repository.tryLockForRelay()).thenReturn(true);
    }

    @Test
    void runsEachDeliveryInANewTransaction() {
        when(repository.nextBatch(1)).thenReturn(List.of());

        deliverer.deliverNext();

        final ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definition.capture());
        assertThat(definition.getValue().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void isIdleWhenAnotherNodeHoldsTheLock() {
        when(repository.tryLockForRelay()).thenReturn(false);

        assertThat(deliverer.deliverNext()).isEqualTo(Result.IDLE);
        verify(repository, never()).nextBatch(1);
    }

    @Test
    void isIdleWhenTheOutboxIsEmpty() {
        when(repository.nextBatch(1)).thenReturn(List.of());

        assertThat(deliverer.deliverNext()).isEqualTo(Result.IDLE);
    }

    @Test
    void deliversThePublicEventAndDeletesExactlyThatEntry() {
        final Envelope envelope = envelope("public.listing.x");
        when(repository.nextBatch(1)).thenReturn(List.of(new OutboxEntry(41, "public.listing.x", codec.toJson(envelope), 0)));

        assertThat(deliverer.deliverNext()).isEqualTo(Result.DELIVERED);

        verify(publicRoute).deliver(envelope);
        verify(inProcessRoute, never()).deliver(any());
        verify(repository).delete(List.of(41L));
    }

    @Test
    void routesAPrivateEventToItsInProcessHandler() {
        final Envelope envelope = envelope("listing.events.handled");
        when(repository.nextBatch(1)).thenReturn(List.of(new OutboxEntry(5, "listing.events.handled", codec.toJson(envelope), 0)));

        assertThat(deliverer.deliverNext()).isEqualTo(Result.DELIVERED);

        verify(inProcessRoute).deliver(envelope);
        verify(repository).delete(List.of(5L));
    }

    @Test
    void aFailedDeliveryCountsTheAttemptAndKeepsTheEntry() {
        final Envelope envelope = envelope("public.listing.x");
        when(repository.nextBatch(1)).thenReturn(List.of(new OutboxEntry(7, "public.listing.x", codec.toJson(envelope), 1)));
        doThrow(new IllegalStateException("broker down")).when(publicRoute).deliver(any());

        assertThat(deliverer.deliverNext()).isEqualTo(Result.FAILED);

        verify(repository).incrementAttempts(7);
        verify(repository, never()).delete(any());
    }

    @Test
    void theLastFailedAttemptDropsTheEntry() {
        final Envelope envelope = envelope("public.listing.x");
        when(repository.nextBatch(1)).thenReturn(List.of(new OutboxEntry(7, "public.listing.x", codec.toJson(envelope), 2)));
        doThrow(new IllegalStateException("broker down")).when(publicRoute).deliver(any());

        assertThat(deliverer.deliverNext()).isEqualTo(Result.FAILED);

        verify(repository).delete(List.of(7L));
        verify(repository, never()).incrementAttempts(anyLong());
    }

    @Test
    void anEventWithNoRouteIsAFailedAttempt() {
        final Envelope envelope = envelope("listing.events.unhandled");
        when(repository.nextBatch(1)).thenReturn(List.of(new OutboxEntry(9, "listing.events.unhandled", codec.toJson(envelope), 0)));

        assertThat(deliverer.deliverNext()).isEqualTo(Result.FAILED);

        verify(repository).incrementAttempts(9);
    }

    @Test
    void anUnparseableEntryIsAFailedAttempt() {
        when(repository.nextBatch(1)).thenReturn(List.of(new OutboxEntry(3, "public.listing.x", "{}", 0)));

        assertThat(deliverer.deliverNext()).isEqualTo(Result.FAILED);

        verify(repository).incrementAttempts(3);
    }

    private static Envelope envelope(final String name) {
        return new Envelope(Metadata.builder().withId(UUID.randomUUID()).withName(name).build(),
                Json.createObjectBuilder().add("k", "v").build());
    }
}
