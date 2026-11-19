package uk.gov.moj.cpp.listing.messaging.outbox;

import uk.gov.moj.cpp.listing.messaging.config.MessagingProperties;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeCodec;

import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code public.*} events to the framework's {@code public.event} topic in the
 * framework's JMS format (MBD refactor P2.2, ADR 013), as {@code JmsSender} +
 * {@code DefaultEnvelopeConverter.toMessage} did (framework 17.104, read with {@code javap}):
 * <ul>
 *     <li>a {@code TextMessage} whose text is the JSON envelope ({@link EnvelopeCodec#toJson});</li>
 *     <li>one string property, {@value #CPPNAME} = {@code _metadata.name} (consumers' message
 *     selectors and schema validation use it);</li>
 *     <li>producer defaults: persistent delivery, priority 4, no expiry; no JMSType, correlation id
 *     or reply-to; a non-transacted, auto-acknowledge session.</li>
 * </ul>
 * Matches the message captured from the JEE app on 2026-11-19 (ADR 013).
 */
@Component
@Order(1)
public class PublicEventPublisher implements OutboxRoute {

    public static final String CPPNAME = "CPPNAME";
    static final String PUBLIC_PREFIX = "public.";

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicEventPublisher.class);

    private final JmsTemplate jmsTemplate;
    private final EnvelopeCodec codec;
    private final String topic;

    public PublicEventPublisher(@Qualifier("listingJmsTemplate") final JmsTemplate jmsTemplate, final EnvelopeCodec codec,
                                final MessagingProperties properties) {
        this.jmsTemplate = jmsTemplate;
        this.codec = codec;
        this.topic = properties.getPublicEvent().getTopic();
    }

    @Override
    public boolean handles(final String eventName) {
        return eventName.startsWith(PUBLIC_PREFIX);
    }

    @Override
    public void deliver(final Envelope envelope) {
        final String body = codec.toJson(envelope);
        jmsTemplate.send(topic, session -> {
            final TextMessage message = session.createTextMessage(body);
            message.setStringProperty(CPPNAME, envelope.name());
            return message;
        });
        LOGGER.debug("Published {} {} to {}", envelope.name(), envelope.metadata().id(), topic);
    }
}
