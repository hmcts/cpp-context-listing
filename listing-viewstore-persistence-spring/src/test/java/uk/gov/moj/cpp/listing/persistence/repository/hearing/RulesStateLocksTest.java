package uk.gov.moj.cpp.listing.persistence.repository.hearing;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.persistence.repository.AbstractRepositoryTest;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.RulesStateJdbcRepository.Kind;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * P2.4 (ADR 3): the per-instance lock of the command services is a transaction-scoped advisory lock
 * that another transaction cannot take until this one ends, also when no state row exists, and
 * that does not touch other instances or kinds. The test's own transaction holds the locks; a
 * second connection (another transaction) probes them with {@code pg_try_advisory_xact_lock}.
 */
class RulesStateLocksTest extends AbstractRepositoryTest {

    @Autowired
    private RulesStateJdbcRepository rulesStateRepository;

    @Autowired
    private HearingRulesStateJdbcRepository hearingRulesStateRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void aLockedHearingCannotBeLockedByAnotherTransactionEvenWithoutAStateRow() throws SQLException {
        final UUID hearingId = randomUUID();
        assertThat(hearingRulesStateRepository.find(hearingId)).isEmpty();

        hearingRulesStateRepository.lock(hearingId);
        hearingRulesStateRepository.lock(hearingId); // re-entrant within the transaction

        assertThat(lockableElsewhere(RulesStateLocks.HEARING, hearingId)).isFalse();
        assertThat(lockableElsewhere(RulesStateLocks.HEARING, randomUUID())).isTrue();
        // the same id in another kind is another lock
        assertThat(lockableElsewhere(RulesStateLocks.CASE, hearingId)).isTrue();
    }

    @Test
    void eachRulesStateKindLocksItsOwnInstances() throws SQLException {
        final UUID id = randomUUID();

        rulesStateRepository.lock(Kind.CASE, id);
        rulesStateRepository.lock(Kind.SEED_HEARING, id);

        assertThat(lockableElsewhere(RulesStateLocks.CASE, id)).isFalse();
        assertThat(lockableElsewhere(RulesStateLocks.SEED_HEARING, id)).isFalse();
        assertThat(lockableElsewhere(RulesStateLocks.APPLICATION, id)).isTrue();
        assertThat(lockableElsewhere(RulesStateLocks.HEARING, id)).isTrue();
    }

    @Test
    void theKeyIsStableAndUsesAllOfTheId() {
        final UUID id = UUID.fromString("5d2ad2c4-5bd6-4b9e-a0f1-3c6f0e0c1111");

        assertThat(RulesStateLocks.key(id)).isEqualTo(RulesStateLocks.key(UUID.fromString(id.toString())));
        assertThat(RulesStateLocks.key(id)).isNotEqualTo(RulesStateLocks.key(UUID.fromString("5d2ad2c4-5bd6-4b9e-a0f1-3c6f0e0c1112")));
        assertThat(RulesStateLocks.key(id)).isNotEqualTo(RulesStateLocks.key(UUID.fromString("6d2ad2c4-5bd6-4b9e-a0f1-3c6f0e0c1111")));
    }

    /** Whether a separate transaction could take the lock now (it releases it at once on commit). */
    private boolean lockableElsewhere(final int namespace, final UUID id) throws SQLException {
        try (Connection other = dataSource.getConnection()) {
            other.setAutoCommit(false);
            try (PreparedStatement tryLock = other.prepareStatement("SELECT pg_try_advisory_xact_lock(?, ?)")) {
                tryLock.setInt(1, namespace);
                tryLock.setInt(2, RulesStateLocks.key(id));
                try (ResultSet rs = tryLock.executeQuery()) {
                    rs.next();
                    return rs.getBoolean(1);
                }
            } finally {
                other.rollback();
            }
        }
    }
}
