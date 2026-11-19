package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.listing.event.PublishCourtListType.DRAFT;
import static uk.gov.justice.listing.event.PublishCourtListType.FINAL;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_REQUESTED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_FAILED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_SUCCESSFUL;

import uk.gov.moj.cpp.listing.persistence.repository.AbstractRepositoryTest;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CourtListPublishStatusJdbcRepositoryTest extends AbstractRepositoryTest {

    private static final LocalDate PUBLISH_DATE = LocalDate.of(2026, 3, 10);
    private static final ZonedDateTime T0 = ZonedDateTime.of(2026, 3, 9, 10, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private CourtListPublishStatusJdbcRepository repository;

    @Test
    void returnsTheLatestStatusPerListType() {
        final UUID courtCentreId = randomUUID();
        repository.save(new CourtListPublishStatus(randomUUID(), courtCentreId, DRAFT, COURT_LIST_REQUESTED, T0, PUBLISH_DATE, false));
        repository.save(new CourtListPublishStatus(randomUUID(), courtCentreId, DRAFT, EXPORT_SUCCESSFUL, T0.plusMinutes(5), PUBLISH_DATE, false));
        repository.save(new CourtListPublishStatus(randomUUID(), courtCentreId, FINAL, EXPORT_FAILED, T0.plusMinutes(1),
                null, null, "boom", PUBLISH_DATE, false));

        final List<CourtListPublishStatusResult> results = repository.courtListPublishStatuses(courtCentreId, Set.of(DRAFT, FINAL), PUBLISH_DATE, false);

        assertThat(results).hasSize(2);
        final CourtListPublishStatusResult draft = results.stream().filter(r -> r.getPublishCourtListType() == DRAFT).findFirst().orElseThrow();
        final CourtListPublishStatusResult fin = results.stream().filter(r -> r.getPublishCourtListType() == FINAL).findFirst().orElseThrow();
        assertThat(draft.getPublishStatus()).isEqualTo(EXPORT_SUCCESSFUL);
        assertThat(draft.getLastUpdated()).isEqualTo(T0.plusMinutes(5));
        assertThat(draft.getCourtCentreId()).isEqualTo(courtCentreId);
        assertThat(fin.getPublishStatus()).isEqualTo(EXPORT_FAILED);
        assertThat(fin.getFailureMessage()).isEqualTo("boom");
    }

    @Test
    void filtersOnWeekCommencingAndPublishDate() {
        final UUID courtCentreId = randomUUID();
        repository.save(new CourtListPublishStatus(randomUUID(), courtCentreId, DRAFT, EXPORT_SUCCESSFUL, T0, PUBLISH_DATE, true));

        assertThat(repository.courtListPublishStatuses(courtCentreId, Set.of(DRAFT), PUBLISH_DATE, true)).hasSize(1);
        assertThat(repository.courtListPublishStatuses(courtCentreId, Set.of(DRAFT), PUBLISH_DATE, false)).isEmpty();
        assertThat(repository.courtListPublishStatuses(courtCentreId, Set.of(DRAFT), PUBLISH_DATE.plusDays(1), true)).isEmpty();
    }
}
