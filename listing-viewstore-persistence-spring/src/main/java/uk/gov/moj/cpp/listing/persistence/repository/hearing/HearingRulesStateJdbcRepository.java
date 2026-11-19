package uk.gov.moj.cpp.listing.persistence.repository.hearing;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC access to {@code hearing_rules_state} (changeset 033; MBD refactor P2.3.2, ADR 015): the
 * command-side state of a hearing that the viewstore does not hold, and the duplicate/deleted
 * tombstone. Written and read only by the Hearing command services, inside their transaction.
 */
@Repository
@Transactional(propagation = MANDATORY)
public class HearingRulesStateJdbcRepository {

    private static final String SELECT = "SELECT duplicate, deleted, state::text FROM hearing_rules_state WHERE hearing_id = ?";
    private static final String UPSERT = "INSERT INTO hearing_rules_state (hearing_id, duplicate, deleted, state, updated_at) "
            + "VALUES (?, ?, ?, cast(? as jsonb), ?) "
            + "ON CONFLICT (hearing_id) DO UPDATE SET duplicate = EXCLUDED.duplicate, deleted = EXCLUDED.deleted, "
            + "state = EXCLUDED.state, updated_at = EXCLUDED.updated_at";

    private final JdbcTemplate jdbcTemplate;

    public HearingRulesStateJdbcRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Serialises the commands and inbound events on {@code hearingId} until the transaction ends
     * (P2.4, {@link RulesStateLocks}): called before the hearing's state is read, whether or not it
     * was ever listed.
     */
    public void lock(final UUID hearingId) {
        RulesStateLocks.lock(jdbcTemplate, RulesStateLocks.HEARING, hearingId);
    }

    public Optional<StoredHearingRulesState> find(final UUID hearingId) {
        final List<StoredHearingRulesState> rows = jdbcTemplate.query(SELECT,
                (rs, i) -> new StoredHearingRulesState(hearingId, rs.getBoolean(1), rs.getBoolean(2), rs.getString(3)),
                hearingId);
        return rows.stream().findFirst();
    }

    public void save(final StoredHearingRulesState state, final ZonedDateTime updatedAt) {
        jdbcTemplate.update(UPSERT, ps -> {
            ps.setObject(1, state.hearingId());
            ps.setBoolean(2, state.duplicate());
            ps.setBoolean(3, state.deleted());
            ps.setString(4, state.stateJson());
            ps.setTimestamp(5, Timestamp.from(updatedAt.toInstant()));
        });
    }

    /** A {@code hearing_rules_state} row. */
    public record StoredHearingRulesState(UUID hearingId, boolean duplicate, boolean deleted, String stateJson) {
    }
}
