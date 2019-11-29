package uk.gov.moj.cpp.listing.persistence.repository;

import static java.lang.String.format;

import uk.gov.justice.services.jdbc.persistence.DataAccessException;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

public class CourtListPublishStatusDBCleaner {
    final DataSource dataSource;
    private static final String SQL_PATTERN = "DELETE FROM %s";

    public CourtListPublishStatusDBCleaner(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void cleanTable(final String tableName) {

        final String sql = format(SQL_PATTERN, tableName);
        try (final PreparedStatement ps = dataSource.getConnection().prepareStatement(sql)) {
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete content from table " + tableName, e);
        }

    }
}
