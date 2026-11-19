package uk.gov.moj.cpp.listing.api.testsupport;

import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.inbound.InboundEventHandler;
import uk.gov.moj.cpp.listing.messaging.outbox.EventOutbox;

import java.util.List;
import java.util.Set;

import jakarta.json.Json;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * <b>P2.2 PROBE — TEST-ONLY. DELETE in P2.4</b>, when the real handler for
 * {@value #PROBED_EVENT} is ported (or earlier, in P2.3, if a real command can drive the outbox
 * ITs).
 *
 * <p>Lets the P2.2 messaging ITs observe the inbound path end to end without any ported processor
 * logic: for {@value #PROBED_EVENT} it writes {@value #ECHO_EVENT} to the outbox in the listener's
 * transaction (so the relay publishes it to {@code public.event}), carrying the inbound event's id.
 * A payload with {@code "p22AlwaysFail": true} makes it throw instead, to prove redelivery and
 * dead-lettering.
 *
 * <p>Only created when {@code listing.p22-messaging-probe.enabled=true}, which only the
 * {@code integration} profile sets; production never has it.
 */
@Component
@ConditionalOnProperty(name = "listing.p22-messaging-probe.enabled", havingValue = "true")
public class P22MessagingProbeHandler implements InboundEventHandler {

    public static final String PROBED_EVENT = "public.progression.case-linked";
    public static final String ECHO_EVENT = "public.listing.p22-messaging-probe-echo";
    public static final String ALWAYS_FAIL = "p22AlwaysFail";

    private final EventOutbox eventOutbox;

    public P22MessagingProbeHandler(final EventOutbox eventOutbox) {
        this.eventOutbox = eventOutbox;
    }

    @Override
    public Set<String> handledEvents() {
        return Set.of(PROBED_EVENT);
    }

    @Override
    public void handle(final Envelope event) {
        if (event.payload().getBoolean(ALWAYS_FAIL, false)) {
            throw new IllegalStateException("P2.2 probe: deliberate failure for " + event.metadata().id());
        }
        eventOutbox.write(event.metadata(), List.of(new DomainEvent(ECHO_EVENT, null, Json.createObjectBuilder()
                .add("receivedEventId", event.metadata().id().toString())
                .add("receivedEventName", event.name())
                .build())));
    }
}
