package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.listing.event.PublishCourtListType.DRAFT;
import static uk.gov.justice.listing.event.PublishCourtListType.FINAL;

import uk.gov.moj.cpp.listing.persistence.repository.AbstractRepositoryTest;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PublishedCourtListRepositoryTest extends AbstractRepositoryTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 3, 10);

    @Autowired
    private PublishedCourtListRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndFindsByCompositeKeyWithJsonbRoundTrip() {
        final UUID courtCentreId = randomUUID();
        final ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("courtCentreId", courtCentreId.toString());
        json.putArray("courtLists").addObject().put("courtRoomName", "Court 1");
        final ZonedDateTime lastUpdated = ZonedDateTime.of(2026, 3, 9, 10, 0, 0, 0, ZoneOffset.UTC);
        repository.save(new PublishedCourtList(courtCentreId, DRAFT, START_DATE, json, lastUpdated, null, randomUUID()));
        entityManager.flush();
        entityManager.clear();

        final PublishedCourtList found = repository.findBy(new PublishedCourtListPrimaryKey(courtCentreId, DRAFT, START_DATE));

        assertThat(found).isNotNull();
        assertThat(found.getCourtListJson()).isEqualTo(json);
        assertThat(found.getLastUpdated().toInstant()).isEqualTo(lastUpdated.toInstant());
        assertThat(repository.findBy(new PublishedCourtListPrimaryKey(courtCentreId, FINAL, START_DATE))).isNull();
        assertThat(repository.findBy(new PublishedCourtListPrimaryKey(courtCentreId, DRAFT, START_DATE.plusDays(1)))).isNull();
    }
}
