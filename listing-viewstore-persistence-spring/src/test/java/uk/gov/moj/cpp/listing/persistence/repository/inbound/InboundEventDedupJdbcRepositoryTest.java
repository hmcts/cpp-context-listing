package uk.gov.moj.cpp.listing.persistence.repository.inbound;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.persistence.repository.AbstractRepositoryTest;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Runs inside the @DataJpaTest transaction (the repository requires one: MANDATORY). */
class InboundEventDedupJdbcRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private InboundEventDedupJdbcRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void firstDeliveryIsRecordedAndADuplicateIsReportedWithoutAnError() {
        final UUID eventId = randomUUID();

        assertThat(repository.contains(eventId)).isFalse();
        assertThat(repository.recordIfAbsent(eventId, "public.progression.case-linked", ZonedDateTime.now())).isTrue();
        assertThat(repository.recordIfAbsent(eventId, "public.progression.case-linked", ZonedDateTime.now())).isFalse();
        assertThat(repository.contains(eventId)).isTrue();

        // ON CONFLICT DO NOTHING: the transaction is still usable after the duplicate (§11 pitfall 2).
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inbound_event_dedup WHERE event_id = ?",
                Integer.class, eventId)).isEqualTo(1);
    }

    @Test
    void differentIdsAreIndependent() {
        assertThat(repository.recordIfAbsent(randomUUID(), "public.hearing.trial-vacated", ZonedDateTime.now())).isTrue();
        assertThat(repository.recordIfAbsent(randomUUID(), "public.hearing.trial-vacated", ZonedDateTime.now())).isTrue();
    }

    @Test
    void purgeDeletesRowsOlderThanTheCutoffInBoundedBatchesAndKeepsNewerOnes() {
        jdbcTemplate.update("DELETE FROM inbound_event_dedup");
        final Instant now = Instant.parse("2026-11-19T12:00:00Z");
        final Instant cutoff = now.minus(Duration.ofDays(30));
        final UUID old1 = record(cutoff.minus(Duration.ofDays(5)));
        final UUID old2 = record(cutoff.minus(Duration.ofDays(1)));
        final UUID old3 = record(cutoff.minusMillis(1));
        final UUID atCutoff = record(cutoff);
        final UUID recent = record(now.minus(Duration.ofDays(1)));

        // Oldest first, at most two per call.
        assertThat(repository.deleteReceivedBefore(cutoff, 2)).isEqualTo(2);
        assertThat(repository.contains(old1)).isFalse();
        assertThat(repository.contains(old2)).isFalse();
        assertThat(repository.contains(old3)).isTrue();

        assertThat(repository.deleteReceivedBefore(cutoff, 2)).isEqualTo(1);
        assertThat(repository.deleteReceivedBefore(cutoff, 2)).isZero();

        assertThat(repository.contains(old3)).isFalse();
        assertThat(repository.contains(atCutoff)).isTrue();
        assertThat(repository.contains(recent)).isTrue();
    }

    @Test
    void purgeLockIsNotGrantedWhileAnotherTransactionHoldsIt() throws Exception {
        try (Connection other = dataSource.getConnection()) {
            other.setAutoCommit(false);
            try (Statement statement = other.createStatement()) {
                statement.execute("SELECT pg_advisory_xact_lock(" + InboundEventDedupJdbcRepository.PURGE_LOCK_KEY + ")");
            }
            assertThat(repository.tryLockForPurge()).isFalse();
            other.rollback();
        }
        assertThat(repository.tryLockForPurge()).isTrue();
    }

    private UUID record(final Instant receivedAt) {
        final UUID eventId = randomUUID();
        assertThat(repository.recordIfAbsent(eventId, "public.hearing.trial-vacated", receivedAt.atZone(ZoneOffset.UTC))).isTrue();
        return eventId;
    }
}
