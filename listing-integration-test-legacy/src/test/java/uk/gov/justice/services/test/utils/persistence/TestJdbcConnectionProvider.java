package uk.gov.justice.services.test.utils.persistence;

import uk.gov.moj.cpp.listing.legacyharness.LegacyStack;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Legacy IT harness override (MBD refactor P2.3.0, §9: DB connection). Shadows the event-store
 * test-utils class of the same name. Only the viewstore exists on the Spring stack (the compose
 * {@code db}); there is no event store and no system database any more, so asking for them is a
 * harness error (the only caller left, {@code ListingEventStoreUtility}, is SPI-scenario only and not
 * part of the {@code *IT} run).
 */
public class TestJdbcConnectionProvider {

    public Connection getViewStoreConnection(final String contextName) {
        try {
            return DriverManager.getConnection(LegacyStack.dbUrl(), LegacyStack.dbUser(), LegacyStack.dbPassword());
        } catch (final SQLException e) {
            throw new IllegalStateException("Cannot connect to the viewstore " + LegacyStack.dbUrl(), e);
        }
    }

    public Connection getEventStoreConnection(final String contextName) {
        throw new UnsupportedOperationException("There is no event store on the Spring stack (MBD refactor, ADR 011/014)");
    }

    public Connection getSystemConnection(final String contextName) {
        throw new UnsupportedOperationException("There is no system database on the Spring stack (MBD refactor, ADR 014)");
    }
}
