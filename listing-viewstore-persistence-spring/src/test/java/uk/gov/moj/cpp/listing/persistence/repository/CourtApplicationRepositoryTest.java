package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.moj.cpp.listing.persistence.repository.ViewstoreFixtures.application;
import static uk.gov.moj.cpp.listing.persistence.repository.ViewstoreFixtures.hearing;

import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CourtApplicationRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private CourtApplicationRepository courtApplicationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsChildApplicationsByParentApplicationId() {
        final Hearing hearing = hearing(randomUUID(), randomUUID(), LocalDate.of(2026, 3, 10), true, "URN1", randomUUID(), randomUUID());
        entityManager.persist(hearing);
        final UUID parentApplicationId = randomUUID();
        final UUID childApplicationId = randomUUID();
        entityManager.persist(application(hearing, childApplicationId, parentApplicationId, "CHILD1"));
        entityManager.flush();
        entityManager.clear();

        final List<CourtApplications> children = courtApplicationRepository.findByParentApplicationId(parentApplicationId);

        assertThat(children).extracting(CourtApplications::getApplicationId).containsExactly(childApplicationId);
        assertThat(children.get(0).getHearing().getId()).isEqualTo(hearing.getId());
    }

    /**
     * CourtApplications.hearing is LAZY, so the finder above works. Initialising that proxy loads
     * the Hearing by id, and Hibernate selects every mapped column, including the read-only
     * "virtual" ones that only the native queries return (totalCount, hearing_date,
     * hearing_day_count, hearing_day_position): Postgres rejects it. Same mapping as the
     * DeltaSpike entity (legacy-equivalent); the query code never navigates this association.
     */
    @Test
    void initialisingTheLazyHearingFailsOnTheVirtualColumns() {
        final Hearing hearing = hearing(randomUUID(), randomUUID(), LocalDate.of(2026, 3, 10), true, "URN2", randomUUID(), randomUUID());
        entityManager.persist(hearing);
        final UUID parentApplicationId = randomUUID();
        entityManager.persist(application(hearing, randomUUID(), parentApplicationId, "CHILD2"));
        entityManager.flush();
        entityManager.clear();

        final CourtApplications child = courtApplicationRepository.findByParentApplicationId(parentApplicationId).get(0);

        assertThatThrownBy(() -> child.getHearing().getProperties())
                .hasMessageContaining("hearing_date does not exist");
    }
}
