package uk.gov.moj.cpp.listing.persistence.repository.outbox;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC access to the {@code event_outbox} table (MBD refactor Phase 1, ADR 2), on Spring's
 * {@link JdbcTemplate} (P2.1 port of the JEE repository; the relay that uses it is P2.2).
 *
 * <p>All methods must run inside the caller's transaction ({@code MANDATORY}): {@link #append}
 * commits atomically with the caller's viewstore changes, and the advisory lock taken by
 * {@link #tryLockForRelay()} is held until the relay's transaction ends.
 */
@Repository
@Transactional(propagation = MANDATORY)
public class EventOutboxJdbcRepository {

    /** Arbitrary constant key for the relay's transaction-scoped advisory lock. */
    static final long RELAY_LOCK_KEY = 7_340_001L;

    private static final String INSERT =
            "INSERT INTO event_outbox (event_id, event_name, stream_id, envelope, created_at) VALUES (?, ?, ?, ?, ?)";
    private static final String TRY_LOCK = "SELECT pg_try_advisory_xact_lock(?)";
    private static final String SELECT_BATCH = "SELECT id, envelope, attempts FROM event_outbox ORDER BY id LIMIT ?";
    private static final String INCREMENT_ATTEMPTS = "UPDATE event_outbox SET attempts = attempts + 1 WHERE id = ?";
    private static final String DELETE_BY_IDS = "DELETE FROM event_outbox WHERE id = ANY (?)";

    private final JdbcTemplate jdbcTemplate;

    public EventOutboxJdbcRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void append(final UUID eventId, final String eventName, final UUID streamId,
                       final String envelopeJson, final ZonedDateTime createdAt) {
        try {
            jdbcTemplate.update(INSERT, ps -> {
                ps.setObject(1, eventId);
                ps.setString(2, eventName);
                ps.setObject(3, streamId);
                ps.setString(4, envelopeJson);
                ps.setTimestamp(5, Timestamp.from(createdAt.toInstant()));
            });
        } catch (final DataAccessException e) {
            throw new EventOutboxException("Failed to append event " + eventName + " to event_outbox", e);
        }
    }

    /**
     * Takes the relay's advisory lock for the current transaction, so only one relay (across all
     * nodes) publishes at a time and events go out in {@code id} order.
     *
     * @return {@code false} if another transaction holds the lock
     */
    public boolean tryLockForRelay() {
        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(TRY_LOCK, Boolean.class, RELAY_LOCK_KEY));
        } catch (final DataAccessException e) {
            throw new EventOutboxException("Failed to take the event_outbox relay lock", e);
        }
    }

    /** The oldest {@code maxRows} entries, in {@code id} order. */
    public List<OutboxEntry> nextBatch(final int maxRows) {
        try {
            return jdbcTemplate.query(SELECT_BATCH,
                    (rs, rowNum) -> new OutboxEntry(rs.getLong("id"), rs.getString("envelope"), rs.getInt("attempts")),
                    maxRows);
        } catch (final DataAccessException e) {
            throw new EventOutboxException("Failed to read from event_outbox", e);
        }
    }

    /**
     * Removes exactly the given entries. Deleting by id (not "up to id") matters: a row with a
     * lower id can commit after the batch was read and must stay for the next batch.
     */
    public void delete(final List<Long> ids) {
        try {
            jdbcTemplate.update(connection -> {
                final var ps = connection.prepareStatement(DELETE_BY_IDS);
                ps.setArray(1, connection.createArrayOf("bigint", ids.toArray()));
                return ps;
            });
        } catch (final DataAccessException e) {
            throw new EventOutboxException("Failed to delete from event_outbox", e);
        }
    }

    /** Records one failed processing attempt for the entry. */
    public void incrementAttempts(final long id) {
        try {
            jdbcTemplate.update(INCREMENT_ATTEMPTS, id);
        } catch (final DataAccessException e) {
            throw new EventOutboxException("Failed to update event_outbox attempts", e);
        }
    }

    public record OutboxEntry(long id, String envelopeJson, int attempts) {
    }
}
