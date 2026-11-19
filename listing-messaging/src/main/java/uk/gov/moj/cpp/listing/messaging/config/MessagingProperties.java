package uk.gov.moj.cpp.listing.messaging.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code listing.messaging.*} (application-messaging.yml; MBD refactor P2.2, ADR 013). Every value
 * comes from a {@code LISTING_*} environment variable with a default (§6). The broker keys mirror
 * courtscheduler's {@code cp.audit.*} block (HA hosts, port, SSL, keystore/truststore and
 * reconnect/retry tuning), so one set of {@code LISTING_ARTEMIS_*} variables configures both the
 * domain messaging and the audit publisher.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "listing.messaging")
public class MessagingProperties {

    /**
     * Master switch. {@code false}: no public.event listener, no outbox relay and no JMS health
     * contributor (the health endpoint does not report DOWN because a broker is absent). The outbox
     * writer still works; rows wait for a relay.
     */
    private boolean enabled = true;

    private Artemis artemis = new Artemis();

    private PublicEvent publicEvent = new PublicEvent();

    private Outbox outbox = new Outbox();

    private Inbound inbound = new Inbound();

    @Getter
    @Setter
    public static class Artemis {
        private boolean highAvailability;
        private List<String> hosts = new ArrayList<>(List.of("localhost"));
        private int port = 61616;
        private String user;
        private String password;
        private boolean sslEnabled;
        private boolean clientAuthRequired;
        private String keystore;
        private String keystorePassword;
        private String truststore;
        private String truststorePassword;
        private boolean verifyHost;
        private Jms jms = new Jms();
    }

    @Getter
    @Setter
    public static class Jms {
        private int reconnectAttempts = -1;
        private int initialConnectAttempts = 10;
        private long retryIntervalMs = 2000;
        private double retryMultiplier = 1.5;
        private long maxRetryIntervalMs = 30000;
        private long connectionTtlMs = 60000;
        private long callTimeoutMs = 15000;
        private int sessionCacheSize = 10;
    }

    @Getter
    @Setter
    public static class PublicEvent {
        /**
         * Physical Artemis address of the framework's {@code public.event} topic. WildFly bound the
         * JNDI name {@code public.event} to the external topic {@code jms.topic.public.event}
         * (cpp-developers-docker broker.xml / standalone.xml); a Spring Artemis client addresses it
         * directly.
         */
        private String topic = "jms.topic.public.event";
        /** The legacy processor MDB's durable shared subscription name: the same broker queue. */
        private String subscriptionName = "listing.event.processor.public.event";
        /** Consumer concurrency; the legacy MDB allowed up to 15 sessions (maxSession). */
        private String concurrency = "1-15";
        /**
         * Start consuming at start-up. {@code false} keeps the listener stopped (e.g. to bring the
         * app up at cut-over before draining the subscription); it can be started through the
         * {@code JmsListenerEndpointRegistry}.
         */
        private boolean autoStartup = true;
    }

    @Getter
    @Setter
    public static class Outbox {
        private Relay relay = new Relay();
    }

    @Getter
    @Setter
    public static class Relay {
        private boolean enabled = true;
        /** Fixed delay between relay runs (Phase 1 default 25 ms). */
        private long delayMillis = 25;
        /** Attempts per entry before it is logged at ERROR and dropped (Phase 1 default 10). */
        private int maxAttempts = 10;
    }

    @Getter
    @Setter
    public static class Inbound {
        private Dedup dedup = new Dedup();
    }

    /** {@code inbound_event_dedup} retention (D26, §15.3). */
    @Getter
    @Setter
    public static class Dedup {
        /** Rows received longer ago than this are purged (ISO-8601, default 30 days). */
        private Duration retention = Duration.ofDays(30);
        private Purge purge = new Purge();
    }

    @Getter
    @Setter
    public static class Purge {
        private boolean enabled = true;
        /** When the purge runs (Spring cron; default daily at 02:30 in the JVM's zone, UTC in the container). */
        private String cron = "0 30 2 * * *";
        /** Rows deleted per transaction. */
        private int batchSize = 1000;
    }
}
