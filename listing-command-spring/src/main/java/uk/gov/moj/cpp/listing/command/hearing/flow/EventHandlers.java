package uk.gov.moj.cpp.listing.command.hearing.flow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

/**
 * The handlers of one port (listener or processor) by event class, in registration order: the JEE
 * {@code @Handles} methods of that component (MBD refactor P2.3.2, ADR 015). An event with no
 * handler is ignored, as an event the component did not subscribe to.
 */
public final class EventHandlers {

    private final Map<Class<?>, List<Consumer<RaisedEvent<?>>>> handlers = new LinkedHashMap<>();

    /** A handler of the typed event ({@code Envelope<X>} in JEE). */
    @SuppressWarnings("unchecked")
    public <T> EventHandlers on(final Class<T> eventClass, final Consumer<RaisedEvent<T>> handler) {
        handlers.computeIfAbsent(eventClass, c -> new ArrayList<>()).add(event -> handler.accept((RaisedEvent<T>) event));
        return this;
    }

    /** A handler of the event as JSON ({@code JsonEnvelope} in JEE). */
    public EventHandlers onJson(final Class<?> eventClass, final Consumer<Envelope> handler) {
        handlers.computeIfAbsent(eventClass, c -> new ArrayList<>()).add(event -> handler.accept(event.asJsonEnvelope()));
        return this;
    }

    public boolean handles(final Class<?> eventClass) {
        return handlers.containsKey(eventClass);
    }

    public void dispatch(final RaisedEvent<?> event) {
        handlers.getOrDefault(event.payload().getClass(), List.of()).forEach(handler -> handler.accept(event));
    }
}
