package uk.gov.moj.cpp.listing.persistence.repository.snapshot;

import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;

/**
 * JDBC access to {@code aggregate_snapshot} (MBD refactor Phase 1, ADR 011). Must run inside a
 * JTA transaction: {@link #findForUpdate} locks the row until the transaction ends, which
 * serialises concurrent commands on the same aggregate (the event store did this with optimistic
 * stream versions).
 */
@ApplicationScoped
public class AggregateSnapshotJdbcRepository {

    private static final String SELECT_FOR_UPDATE =
            "SELECT state FROM aggregate_snapshot WHERE aggregate_id = ? AND aggregate_type = ? FOR UPDATE";
    private static final String UPSERT =
            "INSERT INTO aggregate_snapshot (aggregate_id, aggregate_type, state, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT (aggregate_id, aggregate_type) DO UPDATE SET state = EXCLUDED.state, updated_at = EXCLUDED.updated_at";

    @Inject
    private ViewStoreJdbcDataSourceProvider viewStoreJdbcDataSourceProvider;

    private DataSource dataSource;

    @PostConstruct
    protected void initialiseDataSource() {
        dataSource = viewStoreJdbcDataSourceProvider.getDataSource();
    }

    public Optional<byte[]> findForUpdate(final UUID aggregateId, final String aggregateType) {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(SELECT_FOR_UPDATE)) {
            ps.setObject(1, aggregateId);
            ps.setString(2, aggregateType);
            try (final ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getBytes(1)) : Optional.empty();
            }
        } catch (final SQLException e) {
            throw new AggregateSnapshotException("Failed to read snapshot of " + aggregateType + " " + aggregateId, e);
        }
    }

    public void save(final UUID aggregateId, final String aggregateType, final byte[] state) {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(UPSERT)) {
            ps.setObject(1, aggregateId);
            ps.setString(2, aggregateType);
            ps.setBytes(3, state);
            ps.setTimestamp(4, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new AggregateSnapshotException("Failed to save snapshot of " + aggregateType + " " + aggregateId, e);
        }
    }
}
