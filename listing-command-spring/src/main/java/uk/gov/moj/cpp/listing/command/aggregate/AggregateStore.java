package uk.gov.moj.cpp.listing.command.aggregate;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.listing.command.recorder.EventRecorder;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.persistence.repository.snapshot.AggregateSnapshotJdbcRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads and saves aggregates from {@code aggregate_snapshot} (MBD refactor ADR 011, Spring port in
 * P2.3.0, ADR 014). The Spring counterpart of the Phase 1 JEE {@code AggregateStore}: the same
 * table, the same key ({@code aggregate_id}, aggregate class name) and the same format (plain Java
 * serialisation of the aggregate object), so snapshots written by the Phase 1 JEE app or the P1h
 * backfill load unchanged ({@code AggregateSnapshotCompatibilityTest}).
 *
 * <p>Must be called inside the command's transaction ({@code MANDATORY}): {@link #load} locks the
 * snapshot row ({@code SELECT ... FOR UPDATE}) until commit, which serialises concurrent commands on
 * the same aggregate, and the events raised are recorded ({@link EventRecorder}) in that same
 * transaction.
 */
@Component
@Transactional(propagation = MANDATORY)
public class AggregateStore {

    private final AggregateSnapshotJdbcRepository repository;
    private final EventRecorder eventRecorder;

    public AggregateStore(final AggregateSnapshotJdbcRepository repository, final EventRecorder eventRecorder) {
        this.repository = repository;
        this.eventRecorder = eventRecorder;
    }

    /**
     * The equivalent of {@code aggregateService.get(eventSource.getStreamById(id), type)} followed by
     * {@code eventStream.append(...)}: loads (and locks) the aggregate, runs {@code command} on it,
     * saves the new state and records the events it raised.
     *
     * @param cause the command's metadata (the events' metadata is derived from it)
     * @return the events raised (payload POJOs), already recorded
     */
    public <A extends Aggregate> List<Object> update(final Metadata cause, final UUID aggregateId, final Class<A> type,
                                                     final Function<A, Stream<Object>> command) {
        return update(cause, aggregateId, load(aggregateId, type), command);
    }

    /**
     * As {@link #update(Metadata, UUID, Class, Function)} for an aggregate already obtained with
     * {@link #load} in this transaction (the service read it before deciding what to do).
     */
    public <A extends Aggregate> List<Object> update(final Metadata cause, final UUID aggregateId, final A aggregate,
                                                     final Function<A, Stream<Object>> command) {
        // Aggregate.apply(Stream) is lazy: state changes only as the stream is consumed.
        final List<Object> events = command.apply(aggregate).toList();
        save(aggregateId, aggregate);
        eventRecorder.recordEvents(cause, aggregateId, events);
        return events;
    }

    /**
     * Loads (and locks) the aggregate, or a new instance if it has no snapshot yet. Use it to read
     * state; to change state, go through one of the {@code update} methods so the snapshot is saved.
     */
    public <A extends Aggregate> A load(final UUID aggregateId, final Class<A> type) {
        return repository.findForUpdate(aggregateId, type.getName())
                .map(bytes -> deserialise(bytes, type))
                .orElseGet(() -> newInstance(type));
    }

    public <A extends Aggregate> void save(final UUID aggregateId, final A aggregate) {
        repository.save(aggregateId, aggregate.getClass().getName(), serialise(aggregate));
    }

    public static byte[] serialise(final Aggregate aggregate) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(aggregate);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to serialise " + aggregate.getClass().getName(), e);
        }
        return bytes.toByteArray();
    }

    public static <A extends Aggregate> A deserialise(final byte[] bytes, final Class<A> type) {
        try (ObjectInputStream in = new ClassLoaderAwareObjectInputStream(new ByteArrayInputStream(bytes), type.getClassLoader())) {
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

    /**
     * Resolves classes with the aggregate's class loader, and accepts a generated event/core-domain
     * POJO whose {@code serialVersionUID} differs from the local class when both have exactly the
     * same serialised fields.
     *
     * <p>Why: the framework's pojo-generation-plugin generated the same {@code uk.gov.justice.*}
     * classes (same schemas) into several JEE modules, each with its own serialVersionUIDs
     * (listing-domain-event, listing-event-processor, listing-command-api, listing-command-handler).
     * The Phase 1 WAR contains all four copies and WildFly loads whichever jar comes first in the
     * expanded deployment's directory listing, which is file-system order (observed locally:
     * command-api, event-processor, domain-event, command-handler). So a Phase 1 snapshot can hold
     * any copy's UID. The field layout is what Java serialisation actually reads; when it is
     * identical, reading with the local descriptor gives the same object. Anything else (different
     * fields, or a class outside the generated packages) still fails as plain Java serialisation
     * would (ADR 014).
     */
    static final class ClassLoaderAwareObjectInputStream extends ObjectInputStream {

        private static final List<String> GENERATED_PACKAGES = List.of("uk.gov.justice.listing.", "uk.gov.justice.core.courts.");

        private final ClassLoader classLoader;

        ClassLoaderAwareObjectInputStream(final InputStream in, final ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        @Override
        protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(desc.getName(), false, classLoader);
            } catch (final ClassNotFoundException e) {
                return super.resolveClass(desc);
            }
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            final ObjectStreamClass streamDescriptor = super.readClassDescriptor();
            if (GENERATED_PACKAGES.stream().noneMatch(streamDescriptor.getName()::startsWith)) {
                return streamDescriptor;
            }
            final Class<?> localClass;
            try {
                localClass = Class.forName(streamDescriptor.getName(), false, classLoader);
            } catch (final ClassNotFoundException e) {
                return streamDescriptor;
            }
            final ObjectStreamClass localDescriptor = ObjectStreamClass.lookup(localClass);
            if (localDescriptor != null
                    && localDescriptor.getSerialVersionUID() != streamDescriptor.getSerialVersionUID()
                    && sameFields(streamDescriptor, localDescriptor)) {
                return localDescriptor;
            }
            return streamDescriptor;
        }

        private static boolean sameFields(final ObjectStreamClass stream, final ObjectStreamClass local) {
            final ObjectStreamField[] streamFields = stream.getFields();
            final ObjectStreamField[] localFields = local.getFields();
            if (streamFields.length != localFields.length) {
                return false;
            }
            for (int i = 0; i < streamFields.length; i++) {
                if (!streamFields[i].getName().equals(localFields[i].getName())
                        || streamFields[i].getTypeCode() != localFields[i].getTypeCode()
                        || !Objects.equals(streamFields[i].getTypeString(), localFields[i].getTypeString())) {
                    return false;
                }
            }
            return true;
        }
    }
}
