package uk.gov.moj.cpp.listing.utils;

import static java.lang.String.format;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.getPrivateTopicInstance;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.getPublicTopicInstance;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utilities {

    public static final int RETRY_TIMEOUT_IN_MILLIS = 5000;
    public static final int DEFAULT_POLL_TIMEOUT_IN_SEC = 120;
    public static final int DEFAULT_POLL_TIMEOUT_IN_MILLIS = DEFAULT_POLL_TIMEOUT_IN_SEC * 1000;
    public static final int DEFAULT_NOT_HAPPENED_TIMEOUT_IN_MILLIS = DEFAULT_POLL_TIMEOUT_IN_MILLIS / 6;


    public static EventListener listenForPrivateEvent(String mediaType) {
        return new EventListener(mediaType, DEFAULT_POLL_TIMEOUT_IN_MILLIS, "listing.event");
    }

    public static EventListener listenForPublicEvent(String mediaType) {
        return new EventListener(mediaType);
    }

    public static EventListener listenForPublicEvent(String mediaType, long timeout) {
        return new EventListener(mediaType, timeout);
    }

    public static class EventListener implements AutoCloseable {

        private static final Logger LOGGER = LoggerFactory.getLogger(EventListener.class);
        private final MessageConsumer messageConsumer;
        private final String eventType;
        private Matcher<?> matcher;
        private final long timeout;
        private final QueueUtil queueUtil;

        public EventListener(final String eventType) {
            this(eventType, DEFAULT_POLL_TIMEOUT_IN_MILLIS);
        }

        public EventListener(final String eventType, long timeout) {
            this.eventType = eventType;
            this.queueUtil = getPublicTopicInstance();
            this.messageConsumer = queueUtil.createConsumer(eventType);
            this.timeout = timeout;
        }

        public EventListener(final String eventType, long timeout, String topicName) {
            this.eventType = eventType;
            this.queueUtil = getPrivateTopicInstance(topicName);
            this.messageConsumer = queueUtil.createConsumer(eventType);
            this.timeout = timeout;
        }

        public void expectNone() {
            expectNoneWithin(timeout);
        }

        public void expectNoneWithin(long timeout) {
            JsonPath message = retrieveMessage(messageConsumer, timeout);

            while (message != null && !this.matcher.matches(message.prettify())) {
                message = retrieveMessage(messageConsumer);
            }
            if (message != null) {
                fail(format("expected no messages but got %s", message.prettify()));
            }
        }

        public JsonPath waitFor() {
            int numberOfRetries = 1;
            final long startTime = System.currentTimeMillis();
            JsonPath message;
            StringDescription description = new StringDescription();
            do {
                message = retrieveMessage(messageConsumer, RETRY_TIMEOUT_IN_MILLIS);

                if (message != null) {
                    if (this.matcher.matches(message.prettify())) {
                        LOGGER.info("message:" + message.prettify());
                        return message;
                    } else {
                        description = new StringDescription();
                        description.appendText("Expected ");
                        this.matcher.describeTo(description);
                        description.appendText(" but ");
                        this.matcher.describeMismatch(message.prettify(), description);
                    }
                }
                numberOfRetries++;

            } while (timeout > (System.currentTimeMillis() - startTime));

            fail("Expected '" + eventType + "' Retries " + numberOfRetries + "  message to emit on the public.event topic: " + description.toString());
            return null;
        }

        public EventListener withFilter(Matcher<?> matcher) {
            this.matcher = matcher;
            return this;
        }

        @Override
        public void close() {
            try {
                messageConsumer.close();
            } catch (JMSException ignored) {
            }
        }
    }
}
