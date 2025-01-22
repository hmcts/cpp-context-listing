package uk.gov.moj.cpp.listing.utils;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.getPrivateTopicInstance;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.util.Optional;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utilities {

    public static final int DEFAULT_POLL_TIMEOUT_IN_SEC = 120;
    public static final int DEFAULT_POLL_TIMEOUT_IN_MILLIS = DEFAULT_POLL_TIMEOUT_IN_SEC * 1000;
    public static final int DEFAULT_NOT_HAPPENED_TIMEOUT_IN_MILLIS = DEFAULT_POLL_TIMEOUT_IN_MILLIS / 6;


    public static EventListener listenForPrivateEvent(String mediaType) {
        return new EventListener(mediaType, DEFAULT_POLL_TIMEOUT_IN_MILLIS, "listing.event");
    }

    public static class EventListener {

        private static final Logger LOGGER = LoggerFactory.getLogger(EventListener.class);
        private final JmsMessageConsumerClient messageConsumer;
        private final String eventType;
        private Matcher<?> matcher;
        private final long timeout;
        private final QueueUtil queueUtil;

        public EventListener(final String eventType, long timeout, String topicName) {
            this.eventType = eventType;
            this.queueUtil = getPrivateTopicInstance();
            this.messageConsumer = queueUtil.createPrivateConsumer(eventType);
            this.timeout = timeout;
        }

        public void expectNoneWithin(long timeout) {
            Optional<JsonPath> message = messageConsumer.retrieveMessageAsJsonPath(timeout);

            while (message.isPresent() && !this.matcher.matches(message.get().prettify())) {
                message = messageConsumer.retrieveMessageAsJsonPath(timeout);
            }
            message.ifPresent(jsonPath -> fail(format("expected no messages but got %s", jsonPath.prettify())));
        }

        public EventListener withFilter(Matcher<?> matcher) {
            this.matcher = matcher;
            return this;
        }
    }
}
