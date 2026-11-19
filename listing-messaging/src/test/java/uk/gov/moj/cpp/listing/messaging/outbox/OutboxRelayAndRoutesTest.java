package uk.gov.moj.cpp.listing.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.messaging.config.MessagingProperties;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeCodec;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.messaging.outbox.OutboxDeliverer.Result;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.json.Json;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class OutboxRelayAndRoutesTest {

    private final EnvelopeCodec codec = new EnvelopeCodec();

    @Test
    void relayDrainsUntilTheOutboxIsIdleOrAnEntryFails() {
        final OutboxDeliverer deliverer = mock(OutboxDeliverer.class);
        when(deliverer.deliverNext()).thenReturn(Result.DELIVERED, Result.DELIVERED, Result.FAILED, Result.DELIVERED);

        new OutboxRelay(deliverer).drain();

        verify(deliverer, times(3)).deliverNext();
    }

    @Test
    void relayNeverLetsAnExceptionEscapeTheScheduledRun() {
        final OutboxDeliverer deliverer = mock(OutboxDeliverer.class);
        when(deliverer.deliverNext()).thenThrow(new IllegalStateException("db down"));

        new OutboxRelay(deliverer).drain();

        verify(deliverer).deliverNext();
    }

    @Test
    void publicEventsArePublishedAsATextMessageWithCppname() throws Exception {
        final JmsTemplate jmsTemplate = mock(JmsTemplate.class);
        final MessagingProperties properties = new MessagingProperties();
        final PublicEventPublisher publisher = new PublicEventPublisher(jmsTemplate, codec, properties);
        final Envelope envelope = new Envelope(Metadata.builder().withId(UUID.randomUUID()).withName("public.listing.x").build(),
                Json.createObjectBuilder().add("k", "v").build());

        assertThat(publisher.handles("public.listing.x")).isTrue();
        assertThat(publisher.handles("public.events.listing.x")).isTrue();
        assertThat(publisher.handles("listing.events.x")).isFalse();

        publisher.deliver(envelope);

        final ArgumentCaptor<MessageCreator> creator = ArgumentCaptor.forClass(MessageCreator.class);
        verify(jmsTemplate).send(eq("jms.topic.public.event"), creator.capture());
        final Session session = mock(Session.class);
        final TextMessage message = mock(TextMessage.class);
        final AtomicReference<String> text = new AtomicReference<>();
        when(session.createTextMessage(anyString())).thenAnswer(invocation -> {
            text.set(invocation.getArgument(0));
            return message;
        });

        assertThat(creator.getValue().createMessage(session)).isSameAs(message);
        verify(message).setStringProperty("CPPNAME", "public.listing.x");
        assertThat(text.get()).isEqualTo(codec.toJson(envelope));
    }

    @Test
    void inProcessRouteRunsTheHandlerInItsOwnTransaction() {
        final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        final RecordingHandler handler = new RecordingHandler(Set.of("listing.events.a"));
        final InProcessEventRoute route = new InProcessEventRoute(List.of(handler), transactionManager);
        final Envelope envelope = new Envelope(Metadata.builder().withId(UUID.randomUUID()).withName("listing.events.a").build(),
                Json.createObjectBuilder().build());

        assertThat(route.handles("listing.events.a")).isTrue();
        assertThat(route.handles("listing.events.b")).isFalse();
        route.deliver(envelope);

        assertThat(handler.received.get()).isSameAs(envelope);
        final ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definition.capture());
        assertThat(definition.getValue().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void twoPrivateHandlersForOneEventFailAtStartUp() {
        assertThatThrownBy(() -> new InProcessEventRoute(List.of(new RecordingHandler(Set.of("x")), new RecordingHandler(Set.of("x"))),
                mock(PlatformTransactionManager.class)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Two private event handlers for x");
    }

    private static final class RecordingHandler implements PrivateEventHandler {
        private final Set<String> names;
        private final AtomicReference<Envelope> received = new AtomicReference<>();

        private RecordingHandler(final Set<String> names) {
            this.names = names;
        }

        @Override
        public Set<String> handledEvents() {
            return names;
        }

        @Override
        public void handle(final Envelope event) {
            received.set(event);
        }
    }
}
