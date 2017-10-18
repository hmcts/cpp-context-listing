package uk.gov.moj.cpp.listing.persistence.persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(CdiTestRunner.class)
public class HearingRepositoryTest {

    private static final boolean ALLOCATED = true;
    private static final Boolean UNALLOCATED = false;
    private static final String COURT_CENTRE_ID = "Cardiff";
    private static final String OTHER_COURT_CENTRE = "Liverpool";

    @Inject
    private HearingRepository hearingRepository;

    @Test
    public void shouldFindHearingById() {
        final Hearing actualHearing = saveHearing();

        final Hearing expectedHearing = hearingRepository.findBy(actualHearing.getId());

        assertTrue(EqualsBuilder.reflectionEquals(expectedHearing, actualHearing));
    }

    @Test
    public void shouldFindHearingsByAllocatedAndCourtCentreId() {
        final Hearing expectedHearingToBeReturned = saveHearing(COURT_CENTRE_ID, UNALLOCATED);
        final Hearing expectedHearingNotToBeReturned = saveHearing(OTHER_COURT_CENTRE, ALLOCATED);

        final List<Hearing> actualHearings = hearingRepository.findByAllocatedAndCourtCentreId(UNALLOCATED, COURT_CENTRE_ID);

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getAllocated(), is(UNALLOCATED));
        assertThat(actualHearings.get(0).getCourtCentreId(), is(COURT_CENTRE_ID));
    }

    private Hearing saveHearing(final String courtCentreId, final boolean allocated) {
        final Hearing hearing = createHearing(courtCentreId,allocated) ;

        hearingRepository.save(hearing);

        return hearing;
    }

    private Hearing saveHearing() {
        final Hearing hearing = createHearing() ;

        hearingRepository.save(hearing);

        return hearing;
    }

    private Hearing createHearing(final String courtCentreId, final boolean allocated) {
        return new HearingBuilder()
                .setId(UUID.randomUUID())
                .setAllocated(allocated)
                .setCourtCentreId(courtCentreId)
                .build();
    }


    private Hearing createHearing() {
        return new HearingBuilder()
                .setId(UUID.randomUUID())
                .build();
    }
}