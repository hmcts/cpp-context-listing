package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Real-Postgres repository IT — validates the composite @IdClass and the jsonb court_list_json
 * column round-trip against docker Postgres. No mocking.
 */
class PublishedCourtListRepositoryIT {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("listing-test-persistence-unit");

    private PublishedCourtListRepository repository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        repository = new PublishedCourtListRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldSaveAndRetrieveAPublishedCourtListByPrimaryKey() {
        final UUID courtCentreId = randomUUID();
        final LocalDate startDate = LocalDate.of(2026, 4, 1);
        final JsonNode courtListJson = JsonNodeFactory.instance.objectNode().put("courtName", "Central");

        repository.save(new PublishedCourtList(courtCentreId, PublishCourtListType.DRAFT, startDate,
                courtListJson, ZonedDateTime.now(), null, randomUUID()));

        final PublishedCourtList found = repository.findBy(
                new PublishedCourtListPrimaryKey(courtCentreId, PublishCourtListType.DRAFT, startDate));

        assertThat(found, is(notNullValue()));
        assertThat(found.getCourtListJson().get("courtName").asText(), is("Central"));
    }

    @Test
    void shouldReturnNullWhenNoPublishedCourtListExistsForPrimaryKey() {
        final PublishedCourtList found = repository.findBy(
                new PublishedCourtListPrimaryKey(randomUUID(), PublishCourtListType.FINAL, LocalDate.of(2026, 4, 1)));

        assertThat(found, is(nullValue()));
    }

    @Test
    void shouldCountPublishedCourtLists() {
        repository.save(published(randomUUID(), PublishCourtListType.DRAFT, LocalDate.of(2026, 4, 1)));
        repository.save(published(randomUUID(), PublishCourtListType.FINAL, LocalDate.of(2026, 4, 2)));

        assertThat(repository.count(), is(2L));
    }

    @Test
    void shouldFindAllPublishedCourtLists() {
        repository.save(published(randomUUID(), PublishCourtListType.DRAFT, LocalDate.of(2026, 4, 1)));
        repository.save(published(randomUUID(), PublishCourtListType.WARN, LocalDate.of(2026, 4, 2)));

        assertThat(repository.findAll(), hasSize(2));
    }

    @Test
    void shouldRemoveAPublishedCourtList() {
        final UUID courtCentreId = randomUUID();
        final LocalDate startDate = LocalDate.of(2026, 4, 1);
        repository.save(published(courtCentreId, PublishCourtListType.DRAFT, startDate));

        final PublishedCourtListPrimaryKey pk =
                new PublishedCourtListPrimaryKey(courtCentreId, PublishCourtListType.DRAFT, startDate);
        repository.remove(repository.findBy(pk));

        assertThat(repository.findBy(pk), is(nullValue()));
    }

    private PublishedCourtList published(final UUID courtCentreId, final PublishCourtListType type, final LocalDate startDate) {
        return new PublishedCourtList(courtCentreId, type, startDate,
                JsonNodeFactory.instance.objectNode().put("courtName", "Court"), ZonedDateTime.now(), null, randomUUID());
    }
}
