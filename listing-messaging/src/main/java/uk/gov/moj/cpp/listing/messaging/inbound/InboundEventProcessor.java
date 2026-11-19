package uk.gov.moj.cpp.listing.messaging.inbound;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeCodec;
import uk.gov.moj.cpp.listing.persistence.repository.inbound.InboundEventDedupJdbcRepository;

import java.time.Clock;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes one inbound {@code public.event} message body in one database transaction
 * (MBD refactor P2.2, ADR 013; §7 ADR 3):
 * <ol>
 *     <li>parse the framework envelope (no {@code _metadata}, or a bad id or name, throws);</li>
 *     <li>record {@code _metadata.id} in {@code inbound_event_dedup} with
 *     {@code INSERT … ON CONFLICT DO NOTHING}; if the id is already there the message is a
 *     duplicate (a broker redelivery after a lost acknowledgement, or a re-publish upstream) and
 *     is acknowledged without action;</li>
 *     <li>dispatch to the handler by {@code _metadata.name}.</li>
 * </ol>
 * The listener's JMS session is transacted and commits after this transaction: if the handler
 * throws, both roll back and the broker redelivers; if the JMS commit fails after the database
 * commit, the redelivery is caught by step 2.
 */
@Component
public class InboundEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundEventProcessor.class);

    private final EnvelopeCodec codec;
    private final InboundEventDedupJdbcRepository dedup;
    private final InboundEventDispatcher dispatcher;
    private final Clock clock;

    public InboundEventProcessor(final EnvelopeCodec codec, final InboundEventDedupJdbcRepository dedup,
                                 final InboundEventDispatcher dispatcher, final Clock clock) {
        this.codec = codec;
        this.dedup = dedup;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    /**
     * @param body    the {@code TextMessage} text
     * @param cppName the {@code CPPNAME} property (the selector matched on it; dispatch uses
     *                {@code _metadata.name}, as the framework did)
     */
    @Transactional
    public void process(final String body, final String cppName) {
        final Envelope envelope = codec.parse(body);
        if (cppName != null && !cppName.equals(envelope.name())) {
            LOGGER.warn("Inbound message CPPNAME '{}' differs from _metadata.name '{}' ({}); dispatching on _metadata.name",
                    cppName, envelope.name(), envelope.metadata().id());
        }
        if (!dedup.recordIfAbsent(envelope.metadata().id(), envelope.name(), ZonedDateTime.now(clock))) {
            LOGGER.info("Duplicate inbound event {} {} ignored", envelope.name(), envelope.metadata().id());
            return;
        }
        dispatcher.dispatch(envelope);
    }
}
