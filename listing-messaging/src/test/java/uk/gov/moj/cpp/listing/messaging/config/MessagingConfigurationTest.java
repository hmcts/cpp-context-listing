package uk.gov.moj.cpp.listing.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import uk.gov.moj.cpp.listing.messaging.inbound.InboundEventProcessor;
import uk.gov.moj.cpp.listing.messaging.inbound.PublicEventListener;
import uk.gov.moj.cpp.listing.messaging.inbound.PublicEventSubscription;
import uk.gov.moj.cpp.listing.messaging.inbound.PublicEventSubscriptionTest;

import java.util.List;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * The public.event listener container is configured like the legacy MDB (durable shared
 * subscription, selector, transacted session) and the broker URL follows the cp.audit.* model.
 * {@code auto-startup=false}: nothing connects to a broker in this test.
 */
class MessagingConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MessagingConfiguration.class, PublicEventSubscription.class, PublicEventListener.class)
            .withBean(InboundEventProcessor.class, () -> mock(InboundEventProcessor.class))
            .withPropertyValues("listing.messaging.public-event.auto-startup=false");

    @Test
    void publicEventListenerUsesTheLegacyDurableSharedSubscriptionAndSelectorOnATransactedSession() {
        runner.run(context -> {
            final DefaultMessageListenerContainer container = (DefaultMessageListenerContainer) context
                    .getBean(JmsListenerEndpointRegistry.class).getListenerContainer(PublicEventListener.LISTENER_ID);

            assertThat(container.getDestinationName()).isEqualTo("jms.topic.public.event");
            assertThat(container.isPubSubDomain()).isTrue();
            assertThat(container.isSubscriptionDurable()).isTrue();
            assertThat(container.isSubscriptionShared()).isTrue();
            assertThat(container.getSubscriptionName()).isEqualTo("listing.event.processor.public.event");
            assertThat(container.getClientId()).isNull();
            assertThat(container.getMessageSelector()).isEqualTo(PublicEventSubscriptionTest.GENERATED_MDB_SELECTOR);
            assertThat(container.isSessionTransacted()).isTrue();
            assertThat(container.getMaxConcurrentConsumers()).isEqualTo(15);
            assertThat(container.getConnectionFactory()).isSameAs(context.getBean("listingConnectionFactory"));
            assertThat(container.isAutoStartup()).isFalse();
            assertThat(container.isRunning()).isFalse();
        });
    }

    @Test
    void publisherTemplatePublishesToTopicsWithProducerDefaults() {
        runner.run(context -> {
            final JmsTemplate template = context.getBean("listingJmsTemplate", JmsTemplate.class);
            assertThat(template.isPubSubDomain()).isTrue();
            assertThat(template.isExplicitQosEnabled()).isFalse();
        });
    }

    @Test
    void listenerIsAbsentWhenMessagingIsDisabled() {
        runner.withPropertyValues("listing.messaging.enabled=false").run(context ->
                assertThat(context).doesNotHaveBean(PublicEventListener.class));
    }

    @Test
    void connectionFactoryIsBuiltFromTheCpAuditStyleKeys() {
        runner.withPropertyValues("listing.messaging.artemis.hosts[0]=artemis", "listing.messaging.artemis.port=61617",
                        "listing.messaging.artemis.user=u", "listing.messaging.artemis.password=p")
                .run(context -> {
                    final ActiveMQConnectionFactory factory = context.getBean("listingConnectionFactory", ActiveMQConnectionFactory.class);
                    assertThat(factory.toURI().toString()).contains("artemis").contains("61617");
                    assertThat(factory.getUser()).isEqualTo("u");
                    assertThat(factory.getReconnectAttempts()).isEqualTo(-1);
                    assertThat(factory.getCallTimeout()).isEqualTo(15000);
                });
    }

    @Test
    void brokerUrlCoversHaAndSsl() {
        final MessagingProperties.Artemis artemis = new MessagingProperties.Artemis();
        artemis.setHosts(List.of("a", " ", "b"));
        artemis.setHighAvailability(true);
        artemis.setSslEnabled(true);
        artemis.setVerifyHost(true);
        artemis.setTruststore("/t.jks");
        artemis.setTruststorePassword("tp");

        assertThat(ArtemisBrokerUrl.of(artemis)).isEqualTo("(tcp://a:61616,tcp://b:61616)?ha=true&reconnectAttempts=-1"
                + "&initialConnectAttempts=10&retryInterval=2000&retryIntervalMultiplier=1.5&maxRetryInterval=30000"
                + "&connectionTTL=60000&callTimeout=15000&sslEnabled=true&verifyHost=true&trustStorePath=/t.jks&trustStorePassword=tp");

        artemis.setHosts(List.of(""));
        assertThatThrownBy(() -> ArtemisBrokerUrl.of(artemis)).isInstanceOf(IllegalStateException.class);
    }
}
