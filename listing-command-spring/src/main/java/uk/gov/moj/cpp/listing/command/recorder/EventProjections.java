package uk.gov.moj.cpp.listing.command.recorder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * The registry of ported listener projections, by event name (MBD refactor P2.3, ADR 014). Built
 * once from every {@link EventProjection} bean; two projections for one name fail start-up.
 */
@Component
public class EventProjections {

    private final Map<String, EventProjection> byName = new HashMap<>();

    public EventProjections(final List<EventProjection> projections) {
        for (final EventProjection projection : projections) {
            for (final String name : projection.handledEvents()) {
                final EventProjection previous = byName.putIfAbsent(name, projection);
                if (previous != null) {
                    throw new IllegalStateException("Two listener projections for " + name + ": "
                            + previous.getClass().getName() + " and " + projection.getClass().getName());
                }
            }
        }
    }

    public Optional<EventProjection> forEvent(final String eventName) {
        return Optional.ofNullable(byName.get(eventName));
    }

    public Set<String> eventNames() {
        return Set.copyOf(byName.keySet());
    }
}
