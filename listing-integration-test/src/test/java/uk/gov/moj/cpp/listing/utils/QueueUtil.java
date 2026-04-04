package uk.gov.moj.cpp.listing.utils;

import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.listing.it.util.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;

import java.util.Optional;

public class QueueUtil {

    private static final long RETRIEVE_TIMEOUT = 5000;
    private static final long MESSAGE_RETRIEVE_TRIAL_TIMEOUT = 60000;

    public static final QueueUtil publicEvents = new QueueUtil();

    public static final QueueUtil privateEvents = new QueueUtil();


    private QueueUtil() {
    }

    public static QueueUtil getPublicTopicInstance() {
        return new QueueUtil();
    }

    public static QueueUtil getPrivateTopicInstance() {
        return new QueueUtil();
    }

    public JmsMessageConsumerClient createPublicConsumer(final String eventSelector) {
        return newPublicJmsMessageConsumerClientProvider()
                .withEventNames( eventSelector).getMessageConsumerClient();
    }

    public JmsMessageConsumerClient createPrivateConsumer(final String eventSelector) {
        return newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                .withEventNames(eventSelector).getMessageConsumerClient();
    }

    public static JsonPath retrieveMessage(final JmsMessageConsumerClient consumer) {
        final long startTime = System.currentTimeMillis();
        do {
            final Optional<JsonPath> message = consumer.retrieveMessageAsJsonPath(RETRIEVE_TIMEOUT);
            if (message.isPresent()) {
                return message.get();
            }
        } while (MESSAGE_RETRIEVE_TRIAL_TIMEOUT > (System.currentTimeMillis() - startTime));
        throw new java.util.NoSuchElementException("No JMS message received within " + MESSAGE_RETRIEVE_TRIAL_TIMEOUT + "ms");
    }

    public static String retrieveMessageString(final JmsMessageConsumerClient consumer) {
        final long startTime = System.currentTimeMillis();
        do {
            final Optional<String> message = consumer.retrieveMessage(RETRIEVE_TIMEOUT);
            if (message.isPresent()) {
                return message.get();
            }
        } while (MESSAGE_RETRIEVE_TRIAL_TIMEOUT > (System.currentTimeMillis() - startTime));
        throw new java.util.NoSuchElementException("No JMS message received within " + MESSAGE_RETRIEVE_TRIAL_TIMEOUT + "ms");
    }

    public JmsMessageProducerClient createPublicProducer() {
        return newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    }

    public static void sendMessage(final JmsMessageProducerClient jmsMessageProducerClient, final String commandName, final JsonObject payload, final Metadata metadata) {
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        jmsMessageProducerClient.sendMessage(commandName, jsonEnvelope);
    }

    public static JsonPath retrieveMessage(final JmsMessageConsumerClient consumer, final Matcher matchers) {
        final long startTime = System.currentTimeMillis();
        do {
            final Optional<JsonPath> optMessage = consumer.retrieveMessageAsJsonPath(RETRIEVE_TIMEOUT);
            if (optMessage.isPresent()) {
                final JsonPath message = optMessage.get();
                if(matchers.matches(message.prettify())){
                    return message;
                }
            }
        } while (MESSAGE_RETRIEVE_TRIAL_TIMEOUT > (System.currentTimeMillis() - startTime));
        return null;
    }

    public static void clearAllMessages(JmsMessageConsumerClient consumer) {
        Optional<String> message;
        do {
            message = consumer.retrieveMessage(200).map(Object::toString);
        } while (message.isPresent());
    }
}
