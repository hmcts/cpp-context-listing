package uk.gov.moj.cpp.listing.command.hearing.flow;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.messaging.outbox.EventOutbox;

/**
 * The framework {@code Sender} of the ported event processor (MBD refactor P2.3.2, ADR 015), in the
 * command's transaction:
 * <ul>
 *     <li>{@code public.*} → the outbox, published after commit. The envelope is what the JEE
 *     processor built ({@code metadataFrom(event.metadata()).withName(…)}: the raised event's
 *     metadata — user, session, client correlation, causation, stream id, createdAt — under the
 *     public name) with a new id: the JEE public message reused the private event's id, which no
 *     longer exists, and one event can cause several public messages;</li>
 *     <li>{@code listing.command.*} → the command service, directly ({@link InternalCommands}).</li>
 * </ul>
 */
public class ProcessorSender {

    private final EventOutbox eventOutbox;
    private final InternalCommands internalCommands;

    public ProcessorSender(final EventOutbox eventOutbox, final InternalCommands internalCommands) {
        this.eventOutbox = eventOutbox;
        this.internalCommands = internalCommands;
    }

    public void send(final Envelope envelope) {
        final String name = envelope.name();
        if (name.startsWith("public.")) {
            final Metadata metadata = Metadata.from(envelope.metadata()).withId(randomUUID()).build();
            eventOutbox.append(new Envelope(metadata, envelope.payload()));
        } else if (name.startsWith("listing.command.")) {
            internalCommands.send(envelope);
        } else {
            throw new IllegalArgumentException("The processor sends public messages and listing commands only, not " + name);
        }
    }
}
