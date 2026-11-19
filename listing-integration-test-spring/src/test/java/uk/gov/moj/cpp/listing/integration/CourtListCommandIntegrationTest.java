package uk.gov.moj.cpp.listing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * P2.3.1 court-list slice end to end on the Spring stack (MBD refactor, ADR 015): publish-court-list
 * answers 202 and, in the command transaction, writes the request (changeset 032) and the
 * COURT_LIST_REQUESTED status row the JEE listener wrote; after commit the court-list steps run
 * asynchronously (build and store the list, then export it), which ends with an export outcome
 * row. The full XHIBIT flow with its reference data and the WebDAV PUT is covered by the legacy
 * PublishCourtListIT on this stack.
 */
class CourtListCommandIntegrationTest extends AbstractIntegrationTest {

    private static final String PUBLISH_COURT_LIST = "application/vnd.listing.command.publish-court-list+json";

    /** Liverpool Crown Court: in the static organisation-unit and XHIBIT court-mapping stubs. */
    private static final UUID COURT_CENTRE_ID = UUID.fromString("9b583616-049b-30f9-a14f-028a53b7cfe8");

    @Test
    void publishCourtListRecordsTheRequestAndRunsTheExportAfterCommit() throws Exception {
        final UUID courtCentreId = COURT_CENTRE_ID;
        final java.time.LocalDate start = java.time.LocalDate.of(2031, 1, 6).plusWeeks((long) (Math.random() * 400));
        final String startDate = start.toString();

        final ResponseEntity<String> response = post(COMMAND_API + "/publishCourtList/" + courtCentreId, LISTING_OFFICER_USER_ID, PUBLISH_COURT_LIST,
                "{\"courtCentreId\":\"" + courtCentreId + "\",\"startDate\":\"" + startDate + "\",\"endDate\":\"" + start.plusDays(4)
                        + "\",\"publishCourtListType\":\"WARN\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(query("SELECT week_commencing FROM publish_court_list_request WHERE court_centre_id = ? AND start_date = '" + startDate + "'",
                courtCentreId)).containsExactly("t");
        assertThat(statuses(courtCentreId, startDate)).contains("COURT_LIST_REQUESTED|t|" + startDate);

        final long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline && statuses(courtCentreId, startDate).size() < 2) {
            Thread.sleep(250);
        }
        assertThat(statuses(courtCentreId, startDate)).hasSize(2)
                .anySatisfy(row -> assertThat(row).matches("EXPORT_(SUCCESSFUL|FAILED)\\|t\\|" + startDate));
        assertThat(query("SELECT court_list_id FROM published_court_list WHERE court_centre_id = ? AND start_date = '" + startDate + "'", courtCentreId))
                .containsExactly(UUID.nameUUIDFromBytes((courtCentreId + "/WARN/" + startDate).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
    }

    private static List<String> statuses(final UUID courtCentreId, final String publishDate) throws SQLException {
        return query("SELECT publish_status || '|' || CASE WHEN week_commencing THEN 't' ELSE 'f' END || '|' || publish_date "
                + "FROM court_list_publish_status WHERE court_centre_id = ? AND publish_date = '" + publishDate + "' ORDER BY last_updated", courtCentreId);
    }

    private static List<String> query(final String sql, final UUID courtCentreId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(ViewstoreFixtures.DB_URL, "listing", "listing");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, courtCentreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                final List<String> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(resultSet.getString(1));
                }
                return rows;
            }
        }
    }
}
