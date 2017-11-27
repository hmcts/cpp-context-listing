package uk.gov.moj.cpp.listing.persistence.repository;

import static junit.framework.TestCase.assertTrue;

import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCaseBuilder;

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

    @Inject
    private ListingCaseRepository listingCaseRepository;

    @Test
    public void shouldFindHearingById() {
        final ListingCase actualListingCase = saveListingCase(CASE_ID, URN);
        final ListingCase expectedCase = listingCaseRepository.findBy(actualListingCase.getCaseId());

        assertTrue(EqualsBuilder.reflectionEquals(expectedCase, actualListingCase));
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