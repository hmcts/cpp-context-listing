package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;

import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Real-Postgres repository IT (docker Postgres, throwaway DB). No mocking, no embedded container.
 */
class CourtApplicationRepositoryIT {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("listing-test-persistence-unit");

    private CourtApplicationRepository repository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        repository = new CourtApplicationRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldFindCourtApplicationsByParentApplicationId() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final Hearing hearing = persistedHearing(entityManager);
        final UUID parentApplicationId = randomUUID();
        final UUID childApplicationId1 = randomUUID();
        final UUID childApplicationId2 = randomUUID();

        entityManager.persist(courtApplication(childApplicationId1, hearing, parentApplicationId));
        entityManager.persist(courtApplication(childApplicationId2, hearing, parentApplicationId));
        entityManager.persist(courtApplication(randomUUID(), hearing, randomUUID())); // different parent

        final List<CourtApplications> found = repository.findByParentApplicationId(parentApplicationId);

        assertThat(found, hasSize(2));
        assertThat(found.stream().map(CourtApplications::getApplicationId).sorted().toList(),
                is(java.util.stream.Stream.of(childApplicationId1, childApplicationId2).sorted().toList()));
    }

    @Test
    void shouldReturnEmptyWhenNoCourtApplicationsExistForParentApplicationId() {
        assertThat(repository.findByParentApplicationId(randomUUID()), is(empty()));
    }

    private Hearing persistedHearing(final EntityManager entityManager) {
        final Hearing hearing = new Hearing(randomUUID(),
                JsonNodeFactory.instance.objectNode().put("hearingType", "TRIAL"));
        entityManager.persist(hearing);
        return hearing;
    }

    private CourtApplications courtApplication(final UUID applicationId, final Hearing hearing, final UUID parentApplicationId) {
        return new CourtApplications(randomUUID(), applicationId, hearing, "APPLICATION_TYPE",
                parentApplicationId, "reference", "particulars", Boolean.FALSE);
    }
}
