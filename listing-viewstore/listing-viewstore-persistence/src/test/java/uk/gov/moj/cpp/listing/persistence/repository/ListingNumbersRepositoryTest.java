package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;


import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.runner.*;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.listing.persistence.entity.ListingNumbers;

import java.util.UUID;

import javax.inject.Inject;

import org.junit.Test;

@RunWith(CdiTestRunner.class)
public class ListingNumbersRepositoryTest extends BaseTransactionalTest {

    @Inject
    ListingNumbersRepository listingNumbersRepository;

    @Test
    public void shouldFindOffenceByOffenceId(){
        final UUID offenceId = randomUUID();

        final ListingNumbers expectedOffence = new ListingNumbers(offenceId, 2);
        listingNumbersRepository.save(expectedOffence);

        final ListingNumbers offence = listingNumbersRepository.findBy(offenceId);

        assertThat(offence, is(expectedOffence));

    }

}
