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
 * {@link JdbcTemplate} (P2.1 port of the JEE repository; used by the P2.2 Spring relay and writer in
 * listing-messaging).
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
    private static final String INSERT_IF_ABSENT = INSERT + " ON CONFLICT (event_id) DO NOTHING";
    private static final String TRY_LOCK = "SELECT pg_try_advisory_xact_lock(?)";
    private static final String SELECT_BATCH = "SELECT id, event_name, envelope, attempts FROM event_outbox ORDER BY id LIMIT ?";
    private static final String INCREMENT_ATTEMPTS = "UPDATE event_outbox SET attempts = attempts + 1 WHERE id = ?";
    private static final String DELETE_BY_IDS = "DELETE FROM event_outbox WHERE id = ANY (?)";

    private final JdbcTemplate jdbcTemplate;

    public EventOutboxJdbcRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void append(final UUID eventId, final String eventName, final UUID streamId,
                       final String envelopeJson, final ZonedDateTime createdAt) {
        insert(INSERT, eventId, eventName, streamId, envelopeJson, createdAt);
    }

    /**
     * As {@link #append}, but a row with the same {@code event_id} already in the outbox is kept
     * and this call does nothing ({@code INSERT ... ON CONFLICT DO NOTHING}, §11 pitfall 2). For
     * events whose id is derived from the event that caused them (P2.3, ADR 014): a redelivered
     * cause then cannot append its consequences twice while they are still in the outbox.
     *
     * @return {@code true} if the row was inserted
     */
    public boolean appendIfAbsent(final UUID eventId, final String eventName, final UUID streamId,
                                  final String envelopeJson, final ZonedDateTime createdAt) {
        return insert(INSERT_IF_ABSENT, eventId, eventName, streamId, envelopeJson, createdAt) == 1;
    }

    private int insert(final String sql, final UUID eventId, final String eventName, final UUID streamId,
                       final String envelopeJson, final ZonedDateTime createdAt) {
        try {
            return jdbcTemplate.update(sql, ps -> {
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
                    (rs, rowNum) -> new OutboxEntry(rs.getLong("id"), rs.getString("event_name"), rs.getString("envelope"),
                            rs.getInt("attempts")),
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

    /** One outbox row; {@code eventName} lets the relay route without parsing the envelope first (P2.2). */
    public record OutboxEntry(long id, String eventName, String envelopeJson, int attempts) {
    }
}
