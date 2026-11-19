package uk.gov.moj.cpp.listing.persistence.repository.hearing;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

/**
 * Per-aggregate serialisation of the command services (MBD refactor P2.4; §7 ADR 3, ADR 013 open
 * point): a transaction-scoped Postgres advisory lock, {@code pg_advisory_xact_lock(namespace, key)},
 * taken before the state of a hearing / case / application / seeding hearing is read and held until
 * the transaction ends. Commands (HTTP) and inbound public events that change the same instance
 * therefore run one after the other, and each sees what the previous one committed — the
 * guarantee the JEE event stream's optimistic version check gave.
 *
 * <p>An advisory lock rather than {@code SELECT … FOR UPDATE}: the first command on an instance
 * finds no state row to lock (e.g. two hearings of one case listed at once both creating
 * {@code case_rules_state}), and the lock must not create rows for read-only or rejected commands.
 * The two-key form keeps these locks apart from the single-key ones of the outbox relay and the
 * dedup purge. The key is a 32-bit hash of the id: two ids that collide only serialise more.
 * Locks are re-entrant within a transaction; two transactions that lock the same ids in opposite
 * orders deadlock, and Postgres aborts one of them (an inbound message is then redelivered).
 */
final class RulesStateLocks {

    static final int HEARING = 7_340_101;
    static final int CASE = 7_340_102;
    static final int APPLICATION = 7_340_103;
    static final int SEED_HEARING = 7_340_104;

    static final String LOCK = "SELECT pg_advisory_xact_lock(?, ?)";

    private static final ResultSetExtractor<Void> IGNORE = rs -> null;

    private RulesStateLocks() {
    }

    static void lock(final JdbcTemplate jdbcTemplate, final int namespace, final UUID id) {
        jdbcTemplate.query(LOCK, IGNORE, namespace, key(id));
    }

    static int key(final UUID id) {
        final long bits = id.getMostSignificantBits() ^ id.getLeastSignificantBits();
        return (int) (bits ^ (bits >>> 32));
    }
}
