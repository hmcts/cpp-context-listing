package uk.gov.justice.services.test.utils.persistence;

import static java.util.Arrays.asList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy IT harness override (MBD refactor P2.3.0, §9: "DatabaseCleaner / AbstractIT truncate the
 * new tables instead of the event-store and stream tables"). Replaces the Maven module's copy
 * (itself a fork of the event-store test-utils class) for the run against the Spring stack, with
 * the same public API so no scenario changes:
 * <ul>
 *     <li>there is no event store: {@code cleanEventStoreTables}/{@code cleanEventLogTable}/
 *     {@code cleanSystemTables} do nothing;</li>
 *     <li>viewstore tables the Spring schema does not have (the framework's {@code stream_status},
 *     {@code processed_event}, {@code stream_buffer}, {@code stream_error*}) are skipped; the
 *     others ({@code hearing}, {@code listing_notes}, {@code event_outbox},
 *     {@code aggregate_snapshot}, ...) are truncated as before;</li>
 *     <li>truncating {@code hearing} also truncates {@code hearing_rules_state} (P2.3.2, ADR 015:
 *     the command-side state and tombstones of the hearings, which replaced the Hearing aggregate's
 *     {@code aggregate_snapshot} rows that the scenarios already truncate);</li>
 *     <li>the publish relay is the viewstore {@code event_outbox} only
 *     ({@link #awaitPublishQueuesEmpty}, {@link #publishQueueDepth}).</li>
 * </ul>
 */
public class DatabaseCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCleaner.class);

    private static final long PUBLISH_DRAIN_POLL_INTERVAL_MILLIS = 100;

    /** The Hearing command-side state (changeset 033), cleaned with {@code hearing}. */
    private static final String HEARING_RULES_STATE = "hearing_rules_state";

    private final TestJdbcConnectionProvider testJdbcConnectionProvider;

    public DatabaseCleaner() {
        this(new TestJdbcConnectionProvider());
    }

    DatabaseCleaner(final TestJdbcConnectionProvider testJdbcConnectionProvider) {
        this.testJdbcConnectionProvider = testJdbcConnectionProvider;
    }

    public void cleanStreamBufferTable(final String contextName) {
        cleanViewStoreTables(contextName, "stream_buffer");
    }

    public void cleanStreamStatusTable(final String contextName) {
        cleanViewStoreTables(contextName, "stream_status");
    }

    public void cleanProcessedEventTable(final String contextName) {
        cleanViewStoreTables(contextName, "processed_event");
    }

    public void cleanViewStoreErrorTables(final String contextName) {
        cleanViewStoreTables(contextName, "stream_error_hash", "stream_error");
    }

    /** No event store on the Spring stack: nothing to clean. */
    public void cleanEventStoreTables(final String contextName) {
        LOGGER.debug("No event store on the Spring stack; cleanEventStoreTables({}) does nothing", contextName);
    }

    /** No event store on the Spring stack: nothing to clean. */
    public void cleanEventStoreTables(final String contextName, final String tableName, final String... additionalTableNames) {
        LOGGER.debug("No event store on the Spring stack; cleanEventStoreTables({}, {}) does nothing", contextName, tableName);
    }

    @Deprecated
    public void cleanEventLogTable(final String contextName) {
        cleanEventStoreTables(contextName);
    }

    /** No system database (stored_command) on the Spring stack: nothing to clean. */
    public void cleanSystemTables(final String contextName) {
        LOGGER.debug("No system database on the Spring stack; cleanSystemTables({}) does nothing", contextName);
    }

    /**
     * Bounded wait for the outbox relay to drain ({@code event_outbox} empty). Best-effort, as
     * before: on timeout it logs and returns {@code false}.
     */
    public boolean awaitPublishQueuesEmpty(final String contextName, final long timeoutMillis) {
        final long deadline = System.currentTimeMillis() + timeoutMillis;
        long remaining = -1;
        try (Connection connection = testJdbcConnectionProvider.getViewStoreConnection(contextName)) {
            while (System.currentTimeMillis() < deadline) {
                remaining = countRows(connection, "event_outbox");
                if (remaining == 0) {
                    return true;
                }
                Thread.sleep(PUBLISH_DRAIN_POLL_INTERVAL_MILLIS);
            }
        } catch (final SQLException e) {
            throw new IllegalStateException("Failed to open or close the viewstore connection", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        LOGGER.error("Outbox relay did not drain within {}ms ({} entries left in event_outbox); proceeding with cleanup",
                timeoutMillis, remaining);
        return false;
    }

    /** Rows still waiting in the outbox relay ({@code event_outbox}). */
    public long publishQueueDepth(final String contextName) {
        try (Connection connection = testJdbcConnectionProvider.getViewStoreConnection(contextName)) {
            return countRows(connection, "event_outbox");
        } catch (final SQLException e) {
            throw new IllegalStateException("Failed to open or close the viewstore connection", e);
        }
    }

    public void cleanViewStoreTables(final String contextName, final String tableName, final String... additionalTableNames) {
        final List<String> names = new ArrayList<>();
        names.add(tableName);
        names.addAll(asList(additionalTableNames));
        if (names.contains("hearing") && !names.contains(HEARING_RULES_STATE)) {
            names.add(HEARING_RULES_STATE);
        }
        //noinspection deprecation
        cleanViewStoreTables(contextName, names);
    }

    @Deprecated
    public void cleanViewStoreTables(final String contextName, final List<String> tableNames) {
        try (Connection connection = testJdbcConnectionProvider.getViewStoreConnection(contextName)) {
            for (final String tableName : tableNames) {
                if (exists(connection, tableName)) {
                    truncate(connection, tableName);
                } else {
                    LOGGER.debug("Viewstore table {} does not exist on the Spring stack; skipped", tableName);
                }
            }
        } catch (final SQLException e) {
            throw new IllegalStateException("Failed to commit or close database connection", e);
        }
    }

    private static boolean exists(final Connection connection, final String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    private static void truncate(final Connection connection, final String tableName) {
        try (PreparedStatement statement = connection.prepareStatement("TRUNCATE TABLE " + tableName + " CASCADE")) {
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new IllegalStateException("Failed to delete content from table " + tableName, e);
        }
    }

    private static long countRows(final Connection connection, final String tableName) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (final SQLException e) {
            throw new IllegalStateException("Failed to count rows in table " + tableName, e);
        }
    }
}
