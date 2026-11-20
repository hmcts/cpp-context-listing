package uk.gov.moj.cpp.listing.scenario;

import com.google.common.annotations.VisibleForTesting;
import uk.gov.justice.services.jdbc.persistence.DataAccessException;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ListingEventStoreUtility {

    private final TestJdbcConnectionProvider testJdbcConnectionProvider;

    private static final String COUNT_SQL =
            "SELECT COUNT(*) AS cnt " +
                    "FROM event_log " +
                    "WHERE stream_id = ?";

    public ListingEventStoreUtility() {
        this(new TestJdbcConnectionProvider());
    }

    @VisibleForTesting
    ListingEventStoreUtility(final TestJdbcConnectionProvider testJdbcConnectionProvider) {
        this.testJdbcConnectionProvider = testJdbcConnectionProvider;
    }

    public boolean checkEventExists(final UUID streamId) {
        try {
            try (Connection connection = this.testJdbcConnectionProvider.getEventStoreConnection("listing")) {

                return queryEventLog(streamId,connection);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to commit or close database connection", e);
        }
    }

    //method to query the event log table
    private boolean queryEventLog(final UUID streamId, final Connection connection) {

        try {
            try (PreparedStatement ps = connection.prepareStatement(COUNT_SQL)) {
                ps.setObject(1, streamId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("cnt") >= 1;
                    }
                    return false;
                }

            }

        } catch (SQLException e) {
            throw new RuntimeException("Error checking existence of stream_id " + streamId, e);
        }
    }

}