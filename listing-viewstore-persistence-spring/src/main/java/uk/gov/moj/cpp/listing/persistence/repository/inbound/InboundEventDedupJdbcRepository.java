package uk.gov.moj.cpp.listing.persistence.repository.inbound;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC access to {@code inbound_event_dedup} (changeset 031; MBD refactor P2.2, ADR 013).
 *
 * <p>Inbound public events are deduplicated on {@code _metadata.id}. The insert is a real
 * {@code INSERT … ON CONFLICT DO NOTHING}, never "insert and catch the constraint violation":
 * a failed insert inside the listener's transaction would mark it rollback-only (§11 pitfall 2).
 * A concurrent delivery of the same id blocks on the primary key until the first transaction ends,
 * then sees the conflict (first committed) or inserts (first rolled back).
 *
 * <p>Must run in the caller's transaction ({@code MANDATORY}), so the dedup row commits or rolls
 * back together with the handler's changes.
 *
 * <p>Retention (D26): rows older than the configured retention are purged in bounded batches by
 * listing-messaging's {@code InboundDedupPurge}, one node at a time under
 * {@link #tryLockForPurge()}.
 */
@Repository
@Transactional(propagation = MANDATORY)
public class InboundEventDedupJdbcRepository {

    private static final String INSERT_IF_ABSENT =
            "INSERT INTO inbound_event_dedup (event_id, event_name, received_at) VALUES (?, ?, ?) ON CONFLICT (event_id) DO NOTHING";
    private static final String EXISTS = "SELECT COUNT(*) FROM inbound_event_dedup WHERE event_id = ?";

    /** Arbitrary constant key for the purge's transaction-scoped advisory lock (the outbox relay uses 7_340_001). */
    static final long PURGE_LOCK_KEY = 7_340_002L;

    private static final String TRY_LOCK = "SELECT pg_try_advisory_xact_lock(?)";
    // Bounded: the oldest rows first, at most batchSize per statement (uses inbound_event_dedup_received_at_idx).
    private static final String DELETE_RECEIVED_BEFORE =
            "DELETE FROM inbound_event_dedup WHERE event_id IN "
                    + "(SELECT event_id FROM inbound_event_dedup WHERE received_at < ? ORDER BY received_at LIMIT ?)";

    private final JdbcTemplate jdbcTemplate;

    public InboundEventDedupJdbcRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Records the event id.
     *
     * @return {@code true} if this is the first time the id is seen (the caller should handle the
     *         event); {@code false} if it was already recorded (a duplicate delivery)
     */
    public boolean recordIfAbsent(final UUID eventId, final String eventName, final ZonedDateTime receivedAt) {
        final int inserted = jdbcTemplate.update(INSERT_IF_ABSENT, ps -> {
            ps.setObject(1, eventId);
            ps.setString(2, eventName);
            ps.setTimestamp(3, Timestamp.from(receivedAt.toInstant()));
        });
        return inserted == 1;
    }

    public boolean contains(final UUID eventId) {
        final Integer count = jdbcTemplate.queryForObject(EXISTS, Integer.class, eventId);
        return count != null && count > 0;
    }

    /**
     * Takes the purge's advisory lock for the current transaction, so only one node purges at a time.
     *
     * @return {@code false} if another transaction holds the lock
     */
    public boolean tryLockForPurge() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(TRY_LOCK, Boolean.class, PURGE_LOCK_KEY));
    }

    /**
     * Deletes at most {@code batchSize} rows received before {@code cutoff}, oldest first.
     *
     * @return the number of rows deleted
     */
    public int deleteReceivedBefore(final Instant cutoff, final int batchSize) {
        return jdbcTemplate.update(DELETE_RECEIVED_BEFORE, ps -> {
            ps.setTimestamp(1, Timestamp.from(cutoff));
            ps.setInt(2, batchSize);
        });
    }
}
