package uk.gov.moj.cpp.listing.messaging.config;

import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeCodec;

import java.time.Clock;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Domain messaging wiring (MBD refactor P2.2, ADR 013).
 *
 * <ul>
 *     <li>{@code listingConnectionFactory}: an Artemis core-client factory built from
 *     {@code listing.messaging.artemis.*} ({@link ArtemisBrokerUrl}, the {@code cp.audit.*} key
 *     model). Declaring it makes Spring Boot's Artemis auto-configuration back off, so
 *     {@code spring.artemis.*} is not used. Connections are opened lazily: with messaging disabled
 *     nothing connects.</li>
 *     <li>{@code listingJmsTemplate}: publishes to topics through a session-caching wrapper.</li>
 *     <li>{@code publicEventListenerContainerFactory}: durable, shared topic subscriptions on a
 *     transacted session, on the raw (non-caching) factory as Spring recommends for listener
 *     containers; auto-start follows {@code listing.messaging.enabled} and
 *     {@code listing.messaging.public-event.auto-startup}.</li>
 * </ul>
 */
@Configuration
@EnableJms
@EnableScheduling
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingConfiguration.class);

    @Bean
    public ActiveMQConnectionFactory listingConnectionFactory(final MessagingProperties properties) {
        final MessagingProperties.Artemis artemis = properties.getArtemis();
        final String brokerUrl = ArtemisBrokerUrl.of(artemis);
        LOGGER.info("Listing messaging broker: hosts={}, port={}, ssl={}, ha={}, enabled={}", artemis.getHosts(),
                artemis.getPort(), artemis.isSslEnabled(), artemis.isHighAvailability(), properties.isEnabled());
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        if (artemis.getUser() != null && !artemis.getUser().isBlank()) {
            factory.setUser(artemis.getUser());
            factory.setPassword(artemis.getPassword());
        }
        return factory;
    }

    @Bean
    public JmsTemplate listingJmsTemplate(@Qualifier("listingConnectionFactory") final ConnectionFactory connectionFactory,
                                          final MessagingProperties properties) {
        final CachingConnectionFactory caching = new CachingConnectionFactory(connectionFactory);
        caching.setSessionCacheSize(properties.getArtemis().getJms().getSessionCacheSize());
        caching.setCacheProducers(true);
        caching.setReconnectOnException(true);
        final JmsTemplate template = new JmsTemplate(caching);
        template.setPubSubDomain(true);
        // Producer defaults, as the framework's JmsSender: persistent, priority 4, no expiry.
        template.setExplicitQosEnabled(false);
        return template;
    }

    @Bean
    public DefaultJmsListenerContainerFactory publicEventListenerContainerFactory(
            @Qualifier("listingConnectionFactory") final ConnectionFactory connectionFactory, final MessagingProperties properties) {
        final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSubscriptionDurable(true);
        factory.setSubscriptionShared(true);
        factory.setSessionTransacted(true);
        factory.setAutoStartup(properties.isEnabled() && properties.getPublicEvent().isAutoStartup());
        factory.setErrorHandler(error -> LOGGER.warn("public.event listener: message rolled back for redelivery: {}",
                rootMessage(error), error));
        return factory;
    }

    @Bean
    public EnvelopeCodec envelopeCodec() {
        return new EnvelopeCodec();
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock listingMessagingClock() {
        return Clock.systemUTC();
    }

    private static String rootMessage(final Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
