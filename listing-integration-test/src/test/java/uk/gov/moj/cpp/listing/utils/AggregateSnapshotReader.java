package uk.gov.moj.cpp.listing.utils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static uk.gov.moj.cpp.listing.it.util.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.awaitility.Awaitility;

/**
 * MBD P1h: reads migrated aggregates from the viewstore {@code aggregate_snapshot} table (written by
 * {@code AggregateStore}: Java-serialised state keyed by aggregate id + aggregate class name). Replaces
 * private-event waits whose only observable effect is a change of aggregate state (no event store any more).
 */
public final class AggregateSnapshotReader {

    private static final String SELECT_STATE =
            "SELECT state FROM aggregate_snapshot WHERE aggregate_id = ? AND aggregate_type = ?";

    private static final TestJdbcConnectionProvider CONNECTION_PROVIDER = new TestJdbcConnectionProvider();

    private AggregateSnapshotReader() {
    }

    /**
     * Polls until the {@link Case} snapshot for {@code caseId} exists and references {@code hearingId}
     * (mirrors {@code Case.onHearingUpdatedToCase}: {@code hearingIds.add(existingHearingId)}).
     */
    public static void awaitCaseReferencesHearing(final UUID caseId, final UUID hearingId) {
        Awaitility.await()
                .alias("aggregate_snapshot Case " + caseId + " references hearing " + hearingId)
                .atMost(30, SECONDS)
                .pollInterval(200, MILLISECONDS)
                .until(() -> findCase(caseId).map(c -> c.getHearingIds().contains(hearingId)).orElse(false));
    }

    public static Optional<Case> findCase(final UUID caseId) {
        return findSnapshot(caseId, Case.class);
    }

    public static <T> Optional<T> findSnapshot(final UUID aggregateId, final Class<T> type) {
        try (final Connection connection = CONNECTION_PROVIDER.getViewStoreConnection(CONTEXT_NAME);
             final PreparedStatement statement = connection.prepareStatement(SELECT_STATE)) {
            statement.setObject(1, aggregateId);
            statement.setString(2, type.getName());
            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(type.cast(deserialise(resultSet.getBytes(1))));
            }
        } catch (final SQLException e) {
            throw new IllegalStateException("Failed to read aggregate_snapshot for " + type.getName() + " " + aggregateId, e);
        }
    }

    private static Object deserialise(final byte[] bytes) {
        try (final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return in.readObject();
        } catch (final IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialise aggregate_snapshot state", e);
        }
    }
}
