package uk.gov.moj.cpp.listing.messaging.inbound;

import static uk.gov.moj.cpp.listing.messaging.outbox.PublicEventPublisher.CPPNAME;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the framework's {@code public.event} topic (MBD refactor P2.2, ADR 013), replacing the
 * generated {@code ListingEventProcessorPublicEventJmsListener} MDB:
 * <ul>
 *     <li>the same broker queue: durable, shared subscription
 *     {@code listing.event.processor.public.event} on address {@code jms.topic.public.event}, no
 *     client id (Artemis names the queue {@code listing\.event\.processor\.public\.event});</li>
 *     <li>the same message selector ({@link PublicEventSubscription#selector()});</li>
 *     <li>a transacted session ({@code publicEventListenerContainerFactory}): an exception rolls the
 *     message back and the broker redelivers it, then dead-letters it after its
 *     {@code max-delivery-attempts} (CPP brokers: Artemis default 10, DLQ {@code jms.queue.DLQ});</li>
 *     <li>only {@code TextMessage}s are accepted, as {@code DefaultSubscriptionJmsProcessor} did.</li>
 * </ul>
 * Each message is handled with {@code correlationId} and {@code requestId} in the MDC
 * ({@link InboundTracing}, P2.4), cleared afterwards.
 *
 * <p>Not ported: the framework's inbound JSON-schema validation interceptor (it only logged
 * failures at DEBUG for this subscription); its MDC logger interceptor is replaced by
 * {@link InboundTracing}.
 */
@Component
@ConditionalOnProperty(name = "listing.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class PublicEventListener {

    public static final String LISTENER_ID = "listing.publicEventListener";

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicEventListener.class);

    private final InboundEventProcessor processor;

    public PublicEventListener(final InboundEventProcessor processor) {
        this.processor = processor;
    }

    @JmsListener(id = LISTENER_ID,
            containerFactory = "publicEventListenerContainerFactory",
            destination = "${listing.messaging.public-event.topic:jms.topic.public.event}",
            subscription = "${listing.messaging.public-event.subscription-name:listing.event.processor.public.event}",
            selector = "#{@publicEventSubscription.selector()}",
            concurrency = "${listing.messaging.public-event.concurrency:1-15}")
    public void onMessage(final Message message) throws JMSException {
        if (!(message instanceof TextMessage textMessage)) {
            throw new IllegalArgumentException("Message is not an instance of TextMessage " + message.getJMSMessageID());
        }
        final String body = textMessage.getText();
        InboundTracing.begin(body, message.getJMSMessageID());
        try {
            final String cppName = message.getStringProperty(CPPNAME);
            LOGGER.debug("Received {} {}", cppName, message.getJMSMessageID());
            processor.process(body, cppName);
        } finally {
            InboundTracing.end();
        }
    }
}
