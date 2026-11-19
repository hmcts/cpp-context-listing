package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CacheRefDataCourtroomRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private CacheRefDataCourtroomRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesFindsAndBulkDeletes() {
        final UUID id = randomUUID();
        repository.save(new CacheRefDataCourtroom(id, "Court 1"));
        repository.save(new CacheRefDataCourtroom(randomUUID(), "Court 2"));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findBy(id).getCourtroomName()).isEqualTo("Court 1");
        assertThat(repository.findBy(randomUUID())).isNull();
        assertThat(repository.count()).isEqualTo(2);

        repository.deleteAll();
        entityManager.clear();

        assertThat(repository.count()).isZero();
    }
}
