package uk.gov.moj.cpp.listing.utils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static uk.gov.moj.cpp.listing.it.util.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.awaitility.Awaitility;

/**
 * Legacy IT harness override (MBD refactor P2.3.1, ADR 015, §9). The Maven copy polls the
 * {@code aggregate_snapshot} table for the {@code Case} aggregate's hearing ids; the Spring
 * application keeps no aggregates (ADR 015), so the same wait becomes a viewstore poll: the
 * hearing row {@code hearingId} lists the case {@code caseId} ({@code hearing.properties ->
 * listedCases[].id}). Same method signature, so {@code ListCourtHearingSteps} is unchanged.
 *
 * <p>The Case slice may move the Case → hearing links to an additive viewstore table (ADR 015
 * §3); this poll then switches to that table.
 */
public final class AggregateSnapshotReader {

    private static final String CASE_LISTED_IN_HEARING =
            "SELECT 1 FROM hearing h, jsonb_array_elements(coalesce(h.properties -> 'listedCases', '[]'::jsonb)) c "
                    + "WHERE h.id = ? AND c ->> 'id' = ?";

    private static final TestJdbcConnectionProvider CONNECTION_PROVIDER = new TestJdbcConnectionProvider();

    private AggregateSnapshotReader() {
    }

    /** Polls until the viewstore hearing {@code hearingId} lists the case {@code caseId}. */
    public static void awaitCaseReferencesHearing(final UUID caseId, final UUID hearingId) {
        Awaitility.await()
                .alias("viewstore hearing " + hearingId + " lists case " + caseId)
                .atMost(30, SECONDS)
                .pollInterval(200, MILLISECONDS)
                .until(() -> caseListedInHearing(caseId, hearingId));
    }

    private static boolean caseListedInHearing(final UUID caseId, final UUID hearingId) {
        try (final Connection connection = CONNECTION_PROVIDER.getViewStoreConnection(CONTEXT_NAME);
             final PreparedStatement statement = connection.prepareStatement(CASE_LISTED_IN_HEARING)) {
            statement.setObject(1, hearingId);
            statement.setString(2, caseId.toString());
            try (final ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (final SQLException e) {
            throw new IllegalStateException("Failed to read hearing " + hearingId, e);
        }
    }
}
