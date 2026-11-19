package uk.gov.moj.cpp.listing.persistence.repository.hearing;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.persistence.repository.AbstractRepositoryTest;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.RulesStateJdbcRepository.Kind;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Changeset 034 (ADR 015, P2.3.3): the Case / Application / SeedHearing rules state tables. */
class RulesStateJdbcRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private RulesStateJdbcRepository repository;

    @Test
    void savesReplacesAndFindsEachKindSeparately() {
        for (final Kind kind : Kind.values()) {
            final UUID id = randomUUID();
            assertThat(repository.find(kind, id)).isEmpty();

            repository.save(kind, id, "{\"hearingIds\": [\"" + id + "\"]}", ZonedDateTime.now());
            assertThat(repository.find(kind, id)).hasValueSatisfying(json -> assertThat(json).contains(id.toString()));

            repository.save(kind, id, "{\"hearingIds\": []}", ZonedDateTime.now());
            assertThat(repository.find(kind, id)).hasValue("{\"hearingIds\": []}");
        }
    }

    @Test
    void theSameIdIsIndependentAcrossKinds() {
        final UUID id = randomUUID();
        repository.save(Kind.CASE, id, "{\"a\": 1}", ZonedDateTime.now());

        assertThat(repository.find(Kind.APPLICATION, id)).isEmpty();
        assertThat(repository.find(Kind.SEED_HEARING, id)).isEmpty();
    }
}
