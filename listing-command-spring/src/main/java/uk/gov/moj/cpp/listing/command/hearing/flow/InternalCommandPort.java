package uk.gov.moj.cpp.listing.command.hearing.flow;

import java.util.function.Consumer;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

/**
 * Command services that the processor port reaches by command name (MBD refactor P2.3.2, ADR 015):
 * the JEE processor sent {@code listing.command.*} envelopes to the command handler; here
 * {@link ProcessorSender} calls the registered method directly, in the same transaction.
 */
public interface InternalCommandPort {

    void register(Registry commands);

    interface Registry {
        Registry on(String commandName, Consumer<Envelope> handler);
    }
}
