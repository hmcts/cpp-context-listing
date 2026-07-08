package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;

import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Real-Postgres repository IT (docker Postgres, dedicated throwaway DB listingviewstoretest).
 * No mocking, no embedded container.
 */
class CacheRefDataCourtroomRepositoryIT {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("listing-test-persistence-unit");

    private CacheRefDataCourtroomRepository repository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        repository = new CacheRefDataCourtroomRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldSaveAndRetrieveACourtroomById() {
        final UUID id = randomUUID();
        repository.save(new CacheRefDataCourtroom(id, "Courtroom 1"));

        final CacheRefDataCourtroom found = repository.findBy(id);

        assertThat(found, is(notNullValue()));
        assertThat(found.getId(), is(id));
        assertThat(found.getCourtroomName(), is("Courtroom 1"));
    }

    @Test
    void shouldReturnNullWhenNoCourtroomExistsForId() {
        assertThat(repository.findBy(randomUUID()), is(nullValue()));
    }

    @Test
    void shouldRemoveACourtroom() {
        final UUID id = randomUUID();
        repository.save(new CacheRefDataCourtroom(id, "Courtroom 2"));

        repository.remove(repository.findBy(id));

        assertThat(repository.findBy(id), is(nullValue()));
    }

    @Test
    void shouldDeleteAllCourtrooms() {
        repository.save(new CacheRefDataCourtroom(randomUUID(), "Courtroom A"));
        repository.save(new CacheRefDataCourtroom(randomUUID(), "Courtroom B"));

        final int deleted = repository.deleteAll();

        assertThat(deleted, is(2));
    }
}
