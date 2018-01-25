package uk.gov.moj.cpp.listing.persistence.repository;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCaseBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(CdiTestRunner.class)
public class ListingCaseRepositoryTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final String URN = RandomGenerator.STRING.next();

    private static final UUID CASE_ID_2 = UUID.randomUUID();
    private static final String URN_2 = RandomGenerator.STRING.next();


    @Inject
    private ListingCaseRepository listingCaseRepository;

    @Test
    public void shouldFindHearingById() {
        final ListingCase actualListingCase = saveListingCase(CASE_ID, URN);
        final ListingCase expectedCase = listingCaseRepository.findBy(actualListingCase.getCaseId());

        assertTrue(EqualsBuilder.reflectionEquals(expectedCase, actualListingCase));
    }

    @Test
    public void shouldFindCaseByIds() {
        //Given
        final ListingCase arbitraryCaseOne = saveListingCase(CASE_ID, URN);
        final ListingCase arbitraryCaseTwo = saveListingCase(CASE_ID_2, URN_2);
        List<UUID> caseIds = Arrays.asList(CASE_ID,CASE_ID_2);

        //when
        final List<ListingCase> expectedresult = listingCaseRepository.findByCaseIds(caseIds);

        //then
        assertThat(expectedresult.size(), equalTo(2));
        assertThat(expectedresult.contains(arbitraryCaseOne), equalTo(true));
        assertThat(expectedresult.contains(arbitraryCaseTwo), equalTo(true));
    }

    private ListingCase saveListingCase(final UUID courtCentreId, final String urn) {
        final ListingCase hearing = createListingCase(courtCentreId, urn);
        listingCaseRepository.save(hearing);
        return hearing;
    }

    private ListingCase createListingCase(final UUID caseId, final String urn) {
        return new ListingCaseBuilder()
                .setCaseId(caseId)
                .setUrn(urn)
                .build();
    }

}