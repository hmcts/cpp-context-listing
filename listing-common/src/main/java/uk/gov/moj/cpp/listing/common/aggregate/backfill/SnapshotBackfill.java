package uk.gov.moj.cpp.listing.common.aggregate.backfill;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.common.aggregate.AggregateStore;
import uk.gov.moj.cpp.listing.common.aggregate.backfill.StreamReplayer.StoredEvent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One-off backfill of {@code aggregate_snapshot} from the event store (MBD refactor P1h,
 * ADR 011 §5). Run once at cut-over, after the viewstore Liquibase update and before the Phase 1
 * build takes traffic:
 *
 * <pre>
 * java -cp listing-common.jar:&lt;dependencies&gt;:postgresql.jar \
 *   uk.gov.moj.cpp.listing.common.aggregate.backfill.SnapshotBackfill \
 *   &lt;eventStoreJdbcUrl&gt; &lt;viewStoreJdbcUrl&gt; &lt;user&gt; &lt;password&gt; [--overwrite]
 * </pre>
 *
 * Reads {@code event_log} stream by stream, rebuilds each aggregate with {@link StreamReplayer} and
 * inserts its snapshot. Existing snapshots are kept unless {@code --overwrite} is given.
 */
public class SnapshotBackfill {

    private static final String SELECT_EVENTS =
            "SELECT stream_id, position_in_stream, name, payload FROM event_log ORDER BY stream_id, position_in_stream";
    private static final String INSERT =
            "INSERT INTO aggregate_snapshot (aggregate_id, aggregate_type, state, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT (aggregate_id, aggregate_type) DO NOTHING";
    private static final String UPSERT =
            "INSERT INTO aggregate_snapshot (aggregate_id, aggregate_type, state, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT (aggregate_id, aggregate_type) DO UPDATE SET state = EXCLUDED.state, updated_at = EXCLUDED.updated_at";

    private final StreamReplayer streamReplayer;

    public SnapshotBackfill(final StreamReplayer streamReplayer) {
        this.streamReplayer = streamReplayer;
    }

    public static void main(final String[] args) throws SQLException {
        if (args.length < 4) {
            System.err.println("Usage: SnapshotBackfill <eventStoreJdbcUrl> <viewStoreJdbcUrl> <user> <password> [--overwrite]");
            System.exit(2);
        }
        final boolean overwrite = args.length > 4 && "--overwrite".equals(args[4]);
        try (final Connection eventStore = DriverManager.getConnection(args[0], args[2], args[3]);
             final Connection viewStore = DriverManager.getConnection(args[1], args[2], args[3])) {
            final Result result = new SnapshotBackfill(new StreamReplayer(new ObjectMapperProducer().objectMapper()))
                    .run(eventStore, viewStore, overwrite);
            System.out.println(result);
        }
    }

    public Result run(final Connection eventStore, final Connection viewStore, final boolean overwrite) throws SQLException {
        eventStore.setAutoCommit(false); // lets the driver stream the result set
        viewStore.setAutoCommit(false);
        int streams = 0;
        int snapshots = 0;
        try (final PreparedStatement select = eventStore.prepareStatement(SELECT_EVENTS);
             final PreparedStatement insert = viewStore.prepareStatement(overwrite ? UPSERT : INSERT)) {
            select.setFetchSize(1000);
            try (final ResultSet rs = select.executeQuery()) {
                UUID currentStream = null;
                final List<StoredEvent> events = new ArrayList<>();
                while (rs.next()) {
                    final UUID streamId = rs.getObject("stream_id", UUID.class);
                    if (currentStream != null && !currentStream.equals(streamId)) {
                        snapshots += save(currentStream, events, insert);
                        streams++;
                        events.clear();
                    }
                    currentStream = streamId;
                    events.add(new StoredEvent(rs.getLong("position_in_stream"), rs.getString("name"), rs.getString("payload")));
                }
                if (currentStream != null) {
                    snapshots += save(currentStream, events, insert);
                    streams++;
                }
            }
            viewStore.commit();
        } catch (final SQLException | RuntimeException e) {
            viewStore.rollback();
            throw e;
        }
        return new Result(streams, snapshots);
    }

    private int save(final UUID streamId, final List<StoredEvent> events, final PreparedStatement insert) throws SQLException {
        final Optional<Aggregate> aggregate = streamReplayer.replay(events);
        if (aggregate.isEmpty()) {
            return 0;
        }
        insert.setObject(1, streamId);
        insert.setString(2, aggregate.get().getClass().getName());
        insert.setBytes(3, AggregateStore.serialise(aggregate.get()));
        insert.setTimestamp(4, Timestamp.from(Instant.now()));
        return insert.executeUpdate();
    }

    public record Result(int streams, int snapshotsWritten) {
    }
}
