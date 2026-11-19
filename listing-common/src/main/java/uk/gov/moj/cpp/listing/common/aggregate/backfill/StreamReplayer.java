package uk.gov.moj.cpp.listing.common.aggregate.backfill;

import static java.util.Comparator.comparingLong;

import uk.gov.justice.domain.aggregate.Aggregate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Rebuilds an aggregate from its event-store stream (MBD refactor P1h, ADR 011 §5): works out which
 * aggregate type owns the stream, converts each event payload to its POJO and passes it to the
 * aggregate's {@code apply()}, exactly as the framework did when it rehydrated an aggregate.
 *
 * <p>The event → (POJO class, aggregate types) map is {@code mbd/aggregate-events.json}, generated
 * from the {@code when(X.class)} cases of each aggregate's {@code apply()}. Events no aggregate
 * applies are skipped (the aggregate would ignore them). A stream with no applied events (e.g. the
 * raw court-list streams) has no aggregate.
 *
 * <p>Known gap: state that commands set outside {@code apply()} cannot be recovered by a replay
 * (listed in the §15.2 inventory).
 */
public class StreamReplayer {

    static final String EVENTS_RESOURCE = "/mbd/aggregate-events.json";

    private final ObjectMapper objectMapper;
    private final Map<String, EventType> eventTypes;

    public StreamReplayer(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.eventTypes = loadEventTypes(objectMapper);
    }

    /**
     * @param events the stream's events in {@code position_in_stream} order
     * @return the rebuilt aggregate, or empty if no aggregate applies any of the stream's events
     */
    public Optional<Aggregate> replay(final List<StoredEvent> events) {
        final Optional<Class<? extends Aggregate>> owner = ownerOf(events);
        if (owner.isEmpty()) {
            return Optional.empty();
        }
        final Aggregate aggregate = newInstance(owner.get());
        events.stream()
                .sorted(comparingLong(StoredEvent::positionInStream))
                .filter(event -> appliesTo(event.name(), owner.get()))
                .forEach(event -> aggregate.apply(toPojo(event)));
        return Optional.of(aggregate);
    }

    /** The aggregate type that owns the stream: decided by the events only one aggregate applies. */
    Optional<Class<? extends Aggregate>> ownerOf(final List<StoredEvent> events) {
        final Map<Class<? extends Aggregate>, Long> votes = events.stream()
                .map(event -> eventTypes.get(event.name()))
                .filter(type -> type != null && type.aggregates().size() == 1)
                .collect(Collectors.groupingBy(type -> type.aggregates().iterator().next(), LinkedHashMap::new, Collectors.counting()));
        if (votes.size() > 1) {
            throw new IllegalStateException("Stream has events of several aggregates: " + votes.keySet());
        }
        if (!votes.isEmpty()) {
            return Optional.of(votes.keySet().iterator().next());
        }
        // only events applied by several aggregates (e.g. case-ejected): ambiguous, skip
        return Optional.empty();
    }

    private boolean appliesTo(final String eventName, final Class<? extends Aggregate> aggregate) {
        final EventType type = eventTypes.get(eventName);
        return type != null && type.aggregates().contains(aggregate);
    }

    private Object toPojo(final StoredEvent event) {
        try {
            return objectMapper.readValue(event.payload(), eventTypes.get(event.name()).pojo());
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot read payload of " + event.name() + " at position " + event.positionInStream(), e);
        }
    }

    private static Aggregate newInstance(final Class<? extends Aggregate> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create " + type.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, EventType> loadEventTypes(final ObjectMapper objectMapper) {
        try (final InputStream in = StreamReplayer.class.getResourceAsStream(EVENTS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(EVENTS_RESOURCE + " not found");
            }
            final Map<String, EventType> types = new HashMap<>();
            final JsonNode root = objectMapper.readTree(in);
            final ClassLoader loader = StreamReplayer.class.getClassLoader();
            root.fields().forEachRemaining(entry -> {
                try {
                    final Class<?> pojo = Class.forName(entry.getValue().get("class").asText(), false, loader);
                    final Set<Class<? extends Aggregate>> aggregates = new java.util.LinkedHashSet<>();
                    for (final JsonNode aggregate : entry.getValue().get("aggregates")) {
                        aggregates.add((Class<? extends Aggregate>) Class.forName(aggregate.asText(), false, loader));
                    }
                    types.put(entry.getKey(), new EventType(pojo, aggregates));
                } catch (final ClassNotFoundException e) {
                    throw new IllegalStateException("Unknown class in " + EVENTS_RESOURCE, e);
                }
            });
            return types;
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot read " + EVENTS_RESOURCE, e);
        }
    }

    /** One row of {@code event_log}. */
    public record StoredEvent(long positionInStream, String name, String payload) {
    }

    private record EventType(Class<?> pojo, Set<Class<? extends Aggregate>> aggregates) {
    }
}
