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
 * JDBC access to the Case, Application and SeedHearing rules state (changeset 034; MBD refactor
 * P2.3.3, ADR 015): one JSON document per former aggregate id. Written and read only by the
 * command services, inside their transaction.
 */
@Repository
@Transactional(propagation = MANDATORY)
public class RulesStateJdbcRepository {

    /** The three state tables. */
    public enum Kind {
        CASE("case_rules_state", "case_id", RulesStateLocks.CASE),
        APPLICATION("application_rules_state", "application_id", RulesStateLocks.APPLICATION),
        SEED_HEARING("seed_hearing_rules_state", "seeding_hearing_id", RulesStateLocks.SEED_HEARING);

        private final String table;
        private final String key;
        private final int lockNamespace;

        Kind(final String table, final String key, final int lockNamespace) {
            this.table = table;
            this.key = key;
            this.lockNamespace = lockNamespace;
        }
    }

    private final JdbcTemplate jdbcTemplate;

    public RulesStateJdbcRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Serialises the commands on {@code id} until the transaction ends (P2.4, {@link RulesStateLocks}):
     * called before the state is read, whether or not a row exists yet.
     */
    public void lock(final Kind kind, final UUID id) {
        RulesStateLocks.lock(jdbcTemplate, kind.lockNamespace, id);
    }

    /** The state document of {@code id}, if any. */
    public Optional<String> find(final Kind kind, final UUID id) {
        final List<String> rows = jdbcTemplate.query("SELECT state::text FROM " + kind.table + " WHERE " + kind.key + " = ?",
                (rs, i) -> rs.getString(1), id);
        return rows.stream().findFirst();
    }

    /** Inserts or replaces the state document of {@code id}. */
    public void save(final Kind kind, final UUID id, final String stateJson, final ZonedDateTime updatedAt) {
        jdbcTemplate.update("INSERT INTO " + kind.table + " (" + kind.key + ", state, updated_at) VALUES (?, cast(? as jsonb), ?) "
                + "ON CONFLICT (" + kind.key + ") DO UPDATE SET state = EXCLUDED.state, updated_at = EXCLUDED.updated_at", ps -> {
            ps.setObject(1, id);
            ps.setString(2, stateJson);
            ps.setTimestamp(3, Timestamp.from(updatedAt.toInstant()));
        });
    }
}
