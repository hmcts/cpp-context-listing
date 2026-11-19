package uk.gov.moj.cpp.listing.persistence.repository.snapshot;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.persistence.repository.AbstractRepositoryTest;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Runs inside the @DataJpaTest transaction (the repository requires one: MANDATORY). */
class AggregateSnapshotJdbcRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private AggregateSnapshotJdbcRepository repository;

    @Test
    void upsertsAndReadsForUpdate() {
        final UUID aggregateId = randomUUID();
        assertThat(repository.findForUpdate(aggregateId, "Hearing")).isEmpty();

        repository.save(aggregateId, "Hearing", "v1".getBytes(UTF_8));
        repository.save(aggregateId, "Hearing", "v2".getBytes(UTF_8));

        assertThat(repository.findForUpdate(aggregateId, "Hearing")).hasValueSatisfying(state -> assertThat(new String(state, UTF_8)).isEqualTo("v2"));
        assertThat(repository.findForUpdate(aggregateId, "Case")).isEmpty();
    }
}
