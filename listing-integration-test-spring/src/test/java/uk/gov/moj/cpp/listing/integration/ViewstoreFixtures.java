package uk.gov.moj.cpp.listing.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Seeds the compose stack's viewstore (Postgres on the mapped port) from
 * {@code fixtures/viewstore.sql}, once per test JVM. The query ITs only read, so one seed serves
 * every test class. The app has already applied the Liquibase changelog (it is healthy before the
 * tests start).
 */
final class ViewstoreFixtures {

    static final String DB_URL = System.getProperty("db.url", "jdbc:postgresql://localhost:55436/listing");
    private static final String TABLES = "hearing_days, defendant, linked_case, listed_cases, court_applications, hearing, "
            + "listing_notes, cache_refdata_courtroom, published_court_list, court_list_publish_status";

    private static boolean seeded;

    private ViewstoreFixtures() {
    }

    static synchronized void seedOnce() {
        if (seeded) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(DB_URL, "listing", "listing");
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE " + TABLES + " CASCADE");
            statement.execute(read("fixtures/viewstore.sql"));
        } catch (final SQLException e) {
            throw new IllegalStateException("Could not seed the viewstore at " + DB_URL, e);
        }
        seeded = true;
    }

    private static String read(final String resource) {
        try (InputStream in = ViewstoreFixtures.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " not found");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
