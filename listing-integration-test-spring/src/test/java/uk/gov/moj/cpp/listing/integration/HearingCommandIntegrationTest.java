package uk.gov.moj.cpp.listing.integration;

import static java.util.UUID.randomUUID;
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
 * P2.3.2 Hearing core end to end on the Spring stack (MBD refactor, ADR 015): list an unscheduled
 * hearing (command API → enrichment → ported rules → listener port → processor port, one
 * transaction), then delete it. The hearing row and its command-side state (changeset 033) are
 * written; delete-hearing removes the row as the JEE listener did and keeps a tombstone, so a
 * second delete is rejected by the rules and changes nothing. The full scenarios run in the legacy
 * suite on this stack.
 */
class HearingCommandIntegrationTest extends AbstractIntegrationTest {

    private static final String LIST_UNSCHEDULED = "application/vnd.listing.command.list-unscheduled-court-hearing+json";
    private static final String DELETE_HEARING = "application/vnd.listing.delete-hearing+json";
    /**
     * delete-hearing carries the hearing in the path only: its RAML schema
     * (listing.command.delete-hearing.json) is an empty object with additionalProperties false, so a
     * body naming the hearingId is a 400 on JEE and on Spring alike. Only system users may send it
     * (listing-command-api.drl: isSystemUser on JEE, SYSTEM_USERS here); a listing officer gets 403.
     */
    private static final String EMPTY_BODY = "{}";
    /** In the referencedata-hearing-types stub. */
    private static final String HEARING_TYPE_ID = "4a0e892d-c0c5-3c51-95b8-704d8c781776";

    @Test
    void listUnscheduledHearingThenDeleteItLeavesATombstone() throws Exception {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();

        final ResponseEntity<String> listed = post(COMMAND_API + "/cases", LISTING_OFFICER_USER_ID, LIST_UNSCHEDULED,
                "{\"hearings\":[{\"id\":\"" + hearingId + "\",\"type\":{\"id\":\"" + HEARING_TYPE_ID + "\",\"description\":\"Plea & Trial Preparation\"},"
                        + "\"jurisdictionType\":\"MAGISTRATES\",\"courtCentre\":{\"id\":\"" + courtCentreId + "\",\"name\":\"Port Talbot\"},"
                        + "\"typeOfList\":{\"id\":\"" + randomUUID() + "\",\"description\":\"Unscheduled\"}}]}");

        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(query("SELECT coalesce(unscheduled::text, 'null') || '|' || court_centre_id FROM hearing WHERE id = ?", hearingId))
                .containsExactly("true|" + courtCentreId);
        assertThat(query("SELECT duplicate || '|' || deleted FROM hearing_rules_state WHERE hearing_id = ?", hearingId))
                .containsExactly("false|false");

        final ResponseEntity<String> deleted = post(COMMAND_API + "/delete-hearing/" + hearingId, SYSTEM_USER_ID, DELETE_HEARING, EMPTY_BODY);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(query("SELECT id FROM hearing WHERE id = ?", hearingId)).isEmpty();
        assertThat(query("SELECT duplicate || '|' || deleted FROM hearing_rules_state WHERE hearing_id = ?", hearingId))
                .containsExactly("false|true");

        final ResponseEntity<String> deletedAgain = post(COMMAND_API + "/delete-hearing/" + hearingId, SYSTEM_USER_ID, DELETE_HEARING, EMPTY_BODY);

        assertThat(deletedAgain.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(query("SELECT duplicate || '|' || deleted FROM hearing_rules_state WHERE hearing_id = ?", hearingId))
                .containsExactly("false|true");
    }

    private static List<String> query(final String sql, final UUID id) throws SQLException {
        try (Connection connection = DriverManager.getConnection(ViewstoreFixtures.DB_URL, "listing", "listing");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
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
