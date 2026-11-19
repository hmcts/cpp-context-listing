package uk.gov.moj.cpp.listing.persistence.repository.outbox;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.persistence.repository.AbstractRepositoryTest;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository.OutboxEntry;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Runs inside the @DataJpaTest transaction (the repository requires one: MANDATORY). */
class EventOutboxJdbcRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private EventOutboxJdbcRepository repository;

    @Test
    void appendsReadsInIdOrderCountsAttemptsAndDeletes() {
        repository.append(randomUUID(), "public.listing.one", randomUUID(), "{\"n\":1}", ZonedDateTime.now());
        repository.append(randomUUID(), "public.listing.two", randomUUID(), "{\"n\":2}", ZonedDateTime.now());

        assertThat(repository.tryLockForRelay()).isTrue();

        final List<OutboxEntry> batch = repository.nextBatch(10);
        assertThat(batch).extracting(OutboxEntry::envelopeJson).containsExactly("{\"n\":1}", "{\"n\":2}");
        assertThat(batch).extracting(OutboxEntry::attempts).containsOnly(0);
        assertThat(repository.nextBatch(1)).hasSize(1);

        repository.incrementAttempts(batch.get(0).id());
        assertThat(repository.nextBatch(10).get(0).attempts()).isEqualTo(1);

        repository.delete(List.of(batch.get(0).id()));
        assertThat(repository.nextBatch(10)).extracting(OutboxEntry::id).containsExactly(batch.get(1).id());
    }
}
