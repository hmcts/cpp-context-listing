package uk.gov.moj.cpp.listing.persistence.repository.snapshot;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC access to {@code aggregate_snapshot} (MBD refactor Phase 1, ADR 011), on Spring's
 * {@link JdbcTemplate} (P2.1 port). Must run inside the caller's transaction ({@code MANDATORY}):
 * {@link #findForUpdate} locks the row until the transaction ends, which serialises concurrent
 * commands on the same aggregate. The upsert is a real {@code INSERT ... ON CONFLICT}
 * (§11 pitfall 2).
 */
@Repository
@Transactional(propagation = MANDATORY)
public class AggregateSnapshotJdbcRepository {

    private static final String SELECT_FOR_UPDATE =
            "SELECT state FROM aggregate_snapshot WHERE aggregate_id = ? AND aggregate_type = ? FOR UPDATE";
    private static final String UPSERT =
            "INSERT INTO aggregate_snapshot (aggregate_id, aggregate_type, state, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT (aggregate_id, aggregate_type) DO UPDATE SET state = EXCLUDED.state, updated_at = EXCLUDED.updated_at";

    private final JdbcTemplate jdbcTemplate;

    public AggregateSnapshotJdbcRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<byte[]> findForUpdate(final UUID aggregateId, final String aggregateType) {
        try {
            final List<byte[]> states = jdbcTemplate.query(SELECT_FOR_UPDATE,
                    (rs, rowNum) -> rs.getBytes(1), aggregateId, aggregateType);
            return states.stream().findFirst();
        } catch (final DataAccessException e) {
            throw new AggregateSnapshotException("Failed to read snapshot of " + aggregateType + " " + aggregateId, e);
        }
    }

    public void save(final UUID aggregateId, final String aggregateType, final byte[] state) {
        try {
            jdbcTemplate.update(UPSERT, ps -> {
                ps.setObject(1, aggregateId);
                ps.setString(2, aggregateType);
                ps.setBytes(3, state);
                ps.setTimestamp(4, Timestamp.from(Instant.now()));
            });
        } catch (final DataAccessException e) {
            throw new AggregateSnapshotException("Failed to save snapshot of " + aggregateType + " " + aggregateId, e);
        }
    }
}
