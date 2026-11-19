package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.listing.event.PublishCourtListType.WARN;

import uk.gov.moj.cpp.listing.persistence.repository.AbstractRepositoryTest;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Changeset 032 (ADR 015): the publish-court-list request record. */
class PublishCourtListRequestRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private PublishCourtListRequestRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndFindsTheRequestById() {
        final UUID requestId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime requestedTime = ZonedDateTime.of(2026, 11, 19, 10, 0, 0, 0, ZoneOffset.UTC);
        repository.save(new PublishCourtListRequest(requestId, courtCentreId, WARN, LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 27),
                true, requestedTime));
        entityManager.flush();
        entityManager.clear();

        final PublishCourtListRequest found = repository.findById(requestId).orElseThrow();

        assertThat(found.getCourtCentreId()).isEqualTo(courtCentreId);
        assertThat(found.getPublishCourtListType()).isEqualTo(WARN);
        assertThat(found.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 23));
        assertThat(found.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(found.isWeekCommencing()).isTrue();
        assertThat(found.getRequestedTime().toInstant()).isEqualTo(requestedTime.toInstant());
        assertThat(repository.findById(randomUUID())).isEmpty();
    }
}
