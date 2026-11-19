package uk.gov.moj.cpp.listing.messaging.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** P2.4 (D24): correlationId / requestId in the MDC for each consumed public.event message. */
class InboundTracingTest {

    private static final String WITH_CLIENT_CORRELATION = "{\"_metadata\":{\"id\":\"5d2ad2c4-5bd6-4b9e-a0f1-3c6f0e0c1111\","
            + "\"name\":\"public.progression.case-linked\",\"correlation\":{\"client\":\"client-corr-1\"}},\"caseId\":\"c\"}";
    private static final String WITHOUT_CORRELATION = "{\"_metadata\":{\"id\":\"5d2ad2c4-5bd6-4b9e-a0f1-3c6f0e0c2222\","
            + "\"name\":\"public.progression.case-linked\"},\"caseId\":\"c\"}";

    @Test
    void theClientCorrelationIdIsUsedWhenPresent() {
        assertThat(InboundTracing.correlationIdOf(WITH_CLIENT_CORRELATION, "ID:jms-1")).isEqualTo("client-corr-1");
    }

    @Test
    void otherwiseTheEnvelopeIdThenTheJmsMessageIdThenANewUuid() {
        assertThat(InboundTracing.correlationIdOf(WITHOUT_CORRELATION, "ID:jms-1")).isEqualTo("5d2ad2c4-5bd6-4b9e-a0f1-3c6f0e0c2222");
        assertThat(InboundTracing.correlationIdOf("not json", "ID:jms-1")).isEqualTo("ID:jms-1");
        assertThat(InboundTracing.correlationIdOf("{\"no\":\"metadata\"}", "ID:jms-1")).isEqualTo("ID:jms-1");
        assertThat(UUID.fromString(InboundTracing.correlationIdOf(null, null))).isNotNull();
    }

    @Test
    void anUnsafeValueFromTheMessageIsNotLogged() {
        final String unsafe = "{\"_metadata\":{\"id\":\"x y\\n{\",\"correlation\":{\"client\":\"bad value\\n\"}}}";

        assertThat(InboundTracing.correlationIdOf(unsafe, "ID:jms-1")).isEqualTo("ID:jms-1");
    }

    @Test
    void theListenerSetsBothIdsWhileProcessingAndClearsThemAfterwards() throws Exception {
        final InboundEventProcessor processor = mock(InboundEventProcessor.class);
        final Map<String, String> seen = new HashMap<>();
        doAnswer(i -> {
            seen.putAll(MDC.getCopyOfContextMap());
            return null;
        }).when(processor).process(anyString(), any());

        new PublicEventListener(processor).onMessage(text(WITH_CLIENT_CORRELATION));

        assertThat(seen).containsEntry("correlationId", "client-corr-1");
        assertThat(UUID.fromString(seen.get("requestId"))).isNotNull();
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void theIdsAreClearedWhenProcessingFails() throws Exception {
        final InboundEventProcessor processor = mock(InboundEventProcessor.class);
        doThrow(new IllegalStateException("boom")).when(processor).process(anyString(), any());

        assertThatThrownBy(() -> new PublicEventListener(processor).onMessage(text(WITHOUT_CORRELATION)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("requestId")).isNull();
    }

    private static TextMessage text(final String body) throws Exception {
        final TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn(body);
        when(message.getJMSMessageID()).thenReturn("ID:jms-1");
        when(message.getStringProperty("CPPNAME")).thenReturn("public.progression.case-linked");
        return message;
    }
}
