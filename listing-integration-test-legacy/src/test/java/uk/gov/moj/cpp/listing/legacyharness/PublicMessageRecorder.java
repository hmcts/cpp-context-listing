package uk.gov.moj.cpp.listing.legacyharness;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records every message on {@code public.event} while the legacy suite runs (MBD refactor P2.5,
 * harness only: no scenario is touched), so that {@code PublicMessageContractTest} can validate the
 * payload of each {@code public.*} message the Spring app emitted against the legacy public schemas.
 *
 * <p>The topic is created as {@code new ActiveMQTopic("jms.topic.public.event")}, as the framework's
 * test consumers do: {@code Session.createTopic} would add the 1.x {@code jms.topic.} prefix a second time.
 *
 * <p>Active only when {@code legacy.publicMessages.dir} is set (the {@code legacyIntegrationTest}
 * task sets it). Uses its own non-durable subscription (broker-named, so {@code ArtemisQueuePurger}
 * treats it as a test consumer and never counts it as pipeline backlog). Writes
 * {@code <dir>/<CPPNAME>/<n>.json}, at most {@value #MAX_SAMPLES_PER_NAME} messages per name.
 * Registered in {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener}.
 */
public class PublicMessageRecorder implements TestExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicMessageRecorder.class);
    static final String DIR_PROPERTY = "legacy.publicMessages.dir";
    private static final String TOPIC = "jms.topic.public.event";
    private static final int MAX_SAMPLES_PER_NAME = 500;

    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();
    private Path dir;
    private Connection connection;

    @Override
    public void testPlanExecutionStarted(final TestPlan testPlan) {
        final String configured = System.getProperty(DIR_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return;
        }
        dir = Path.of(configured);
        try {
            deleteRecursively(dir);
            Files.createDirectories(dir);
            connection = new ActiveMQConnectionFactory(LegacyStack.artemisUrl()).createConnection();
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final MessageConsumer consumer = session.createConsumer(new ActiveMQTopic(TOPIC));
            consumer.setMessageListener(message -> {
                try {
                    final String name = message.getStringProperty("CPPNAME");
                    if (name != null && message instanceof TextMessage text) {
                        record(name, text.getText());
                    } else {
                        counts.computeIfAbsent("_unrecorded:" + name + ":" + message.getClass().getSimpleName(), k -> new AtomicInteger()).incrementAndGet();
                    }
                } catch (final JMSException | RuntimeException e) {
                    LOGGER.warn("Could not record a public.event message", e);
                    status("could not record a message: " + e);
                }
            });
            connection.start();
            LOGGER.info("Recording public.event messages into {}", dir);
            status("recording " + TOPIC + " from " + LegacyStack.artemisUrl());
        } catch (final JMSException | IOException | RuntimeException e) {
            LOGGER.error("Public message recorder not started: PublicMessageContractTest will find no samples", e);
            status("NOT STARTED: " + e);
            close();
        }
    }

    @Override
    public void testPlanExecutionFinished(final TestPlan testPlan) {
        if (connection == null) {
            return;
        }
        try {
            // The relay publishes after commit; give the last test's messages a moment to arrive.
            Thread.sleep(2_000);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        close();
        LOGGER.info("Recorded public.event messages: {}", counts);
        status("recorded " + counts);
    }

    /** {@code <dir>/_recorder-status.txt}: what the recorder did (the test JVM's log is not kept). */
    private void status(final String line) {
        try {
            Files.writeString(dir.resolve("_recorder-status.txt"), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (final IOException e) {
            LOGGER.warn("Could not write the recorder status", e);
        }
    }

    private void record(final String name, final String body) {
        final int n = counts.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
        if (n > MAX_SAMPLES_PER_NAME) {
            return;
        }
        try {
            final Path nameDir = Files.createDirectories(dir.resolve(name));
            Files.writeString(nameDir.resolve(n + ".json"), body, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (final JMSException e) {
            LOGGER.warn("Could not close the recorder connection", e);
        } finally {
            connection = null;
        }
    }

    private static void deleteRecursively(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            for (final Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
