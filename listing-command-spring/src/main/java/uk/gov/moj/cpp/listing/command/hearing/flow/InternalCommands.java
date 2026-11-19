package uk.gov.moj.cpp.listing.command.hearing.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import org.springframework.beans.factory.ObjectProvider;

/**
 * The listing commands the processor port can send, by name (MBD refactor P2.3.2, ADR 015). Built
 * lazily from the {@link InternalCommandPort} beans (the command services depend on the flow, which
 * depends on the processor port, which sends to them).
 */
public class InternalCommands {

    private final ObjectProvider<InternalCommandPort> ports;
    private volatile Map<String, Consumer<Envelope>> handlers;

    public InternalCommands(final ObjectProvider<InternalCommandPort> ports) {
        this.ports = ports;
    }

    public void send(final Envelope command) {
        final Consumer<Envelope> handler = handlers().get(command.name());
        if (handler == null) {
            throw new IllegalStateException("No command service for " + command.name());
        }
        handler.accept(command);
    }

    public boolean handles(final String commandName) {
        return handlers().containsKey(commandName);
    }

    private Map<String, Consumer<Envelope>> handlers() {
        Map<String, Consumer<Envelope>> result = handlers;
        if (result == null) {
            final Map<String, Consumer<Envelope>> built = new HashMap<>();
            final InternalCommandPort.Registry registry = new InternalCommandPort.Registry() {
                @Override
                public InternalCommandPort.Registry on(final String commandName, final Consumer<Envelope> handler) {
                    if (built.putIfAbsent(commandName, handler) != null) {
                        throw new IllegalStateException("Two command services for " + commandName);
                    }
                    return this;
                }
            };
            final List<InternalCommandPort> all = ports.orderedStream().toList();
            all.forEach(port -> port.register(registry));
            result = Map.copyOf(built);
            handlers = result;
        }
        return result;
    }
}
