package uk.gov.moj.cpp.listing.common.aggregate;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.common.outbox.EventRecorder;
import uk.gov.moj.cpp.listing.persistence.repository.snapshot.AggregateSnapshotJdbcRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Loads and saves migrated aggregates from {@code aggregate_snapshot} instead of replaying their
 * event-store stream (MBD refactor Phase 1, ADR 011). The aggregate classes and their rules are
 * unchanged; only where their state lives changes.
 *
 * <p>Must be called inside a JTA transaction (the command handler's MDB transaction): the snapshot
 * row is locked from {@link #update} until commit.
 */
@ApplicationScoped
public class AggregateStore {

    @Inject
    private AggregateSnapshotJdbcRepository aggregateSnapshotJdbcRepository;

    @Inject
    private EventRecorder eventRecorder;

    /**
     * The equivalent of {@code aggregateService.get(eventSource.getStreamById(id), type)} followed by
     * {@code eventStream.append(...)}: loads the aggregate, runs {@code command} on it, saves the new
     * state and records the events it raised.
     *
     * @return the events raised, already recorded
     */
    public <A extends Aggregate> List<Object> update(final Envelope<?> cause, final UUID aggregateId, final Class<A> type,
                                                     final Function<A, Stream<Object>> command) {
        final A aggregate = load(aggregateId, type);
        // Aggregate.apply(Stream) is lazy: state changes only as the stream is consumed.
        final List<Object> events = command.apply(aggregate).collect(toList());
        save(aggregateId, aggregate);
        eventRecorder.record(cause, aggregateId, events);
        return events;
    }

    /**
     * As {@link #update(Envelope, UUID, Class, Function)} for an aggregate already obtained with
     * {@link #load} in this transaction (the handler read it before deciding what to do).
     */
    public <A extends Aggregate> List<Object> update(final Envelope<?> cause, final UUID aggregateId, final A aggregate,
                                                     final Function<A, Stream<Object>> command) {
        final List<Object> events = command.apply(aggregate).collect(toList());
        save(aggregateId, aggregate);
        eventRecorder.record(cause, aggregateId, events);
        return events;
    }

    /**
     * Loads (and locks) the aggregate, or a new instance if it has no snapshot yet. Use it to read
     * state; to change state, go through one of the {@code update} methods so the snapshot is saved.
     */
    public <A extends Aggregate> A load(final UUID aggregateId, final Class<A> type) {
        return aggregateSnapshotJdbcRepository.findForUpdate(aggregateId, type.getName())
                .map(bytes -> deserialise(bytes, type))
                .orElseGet(() -> newInstance(type));
    }

    public <A extends Aggregate> void save(final UUID aggregateId, final A aggregate) {
        aggregateSnapshotJdbcRepository.save(aggregateId, aggregate.getClass().getName(), serialise(aggregate));
    }

    public static byte[] serialise(final Aggregate aggregate) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (final ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(aggregate);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to serialise " + aggregate.getClass().getName(), e);
        }
        return bytes.toByteArray();
    }

    public static <A extends Aggregate> A deserialise(final byte[] bytes, final Class<A> type) {
        try (final ObjectInputStream in = new ClassLoaderAwareObjectInputStream(new ByteArrayInputStream(bytes), type.getClassLoader())) {
            return type.cast(in.readObject());
        } catch (final IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialise " + type.getName(), e);
        }
    }

    private static <A extends Aggregate> A newInstance(final Class<A> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Cannot create " + type.getName(), e);
        }
    }

    /** Resolves classes with the aggregate's class loader (WildFly module class loaders). */
    private static final class ClassLoaderAwareObjectInputStream extends ObjectInputStream {
        private final ClassLoader classLoader;

        ClassLoaderAwareObjectInputStream(final ByteArrayInputStream in, final ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        @Override
        protected Class<?> resolveClass(final java.io.ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(desc.getName(), false, classLoader);
            } catch (final ClassNotFoundException e) {
                return super.resolveClass(desc);
            }
        }
    }
}
