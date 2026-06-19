package uk.gov.moj.cpp.listing.event.listener;

import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.listing.events.Offence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("squid:S2187")
public class OffenceComparatorTest {

    @InjectMocks
    private OffenceComparator offenceComparator;

    @Test
    public void shouldReturnPassedInOffenceWhichHasListingNumberHigherThanDBVersion() {
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        final Offence dbListedOffenceId1WithListingNumber2 = getOffence(offenceId1, 2);
        final Offence dbListedOffenceId2WithListingNumber3 = getOffence(offenceId2, 3);
        final List<Offence> dbListedOffences = new ArrayList();
        dbListedOffences.add(dbListedOffenceId1WithListingNumber2);
        dbListedOffences.add(dbListedOffenceId2WithListingNumber3);

        final Offence offenceId1WithListingNumber4 = getOffence(offenceId1, 4);

        final Offence offenceToAddOrReplace = offenceComparator.getLatestVersionOfOffence(offenceId1WithListingNumber4, dbListedOffences);
        assertThat(offenceToAddOrReplace.getListingNumber(), CoreMatchers.is(4));
    }

    @Test
    public void shouldReturnPassedInOffenceWhichHasListingNumberSameAsDBVersion() {
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        final Offence dbListedOffenceId1WithListingNumber2 = getOffence(offenceId1, 2);
        final Offence dbListedOffenceId2WithListingNumber3 = getOffence(offenceId2, 3);
        final List<Offence> dbListedOffences = new ArrayList();
        dbListedOffences.add(dbListedOffenceId1WithListingNumber2);
        dbListedOffences.add(dbListedOffenceId2WithListingNumber3);

        final Offence offenceId1WithListingNumber4 = getOffence(offenceId1, 3);

        final Offence offenceToAddOrReplace = offenceComparator.getLatestVersionOfOffence(offenceId1WithListingNumber4, dbListedOffences);
        assertThat(offenceToAddOrReplace.getListingNumber(), CoreMatchers.is(3));
    }


    @Test
    public void shouldReturnPassedInOffenceWhenDBDoesNotHaveTheOffence() {
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        final Offence dbListedOffenceId1WithListingNumber2 = getOffence(offenceId1, 2);
        final Offence dbListedOffenceId2WithListingNumber3 = getOffence(offenceId2, 3);
        final List<Offence> dbListedOffences = new ArrayList();
        dbListedOffences.add(dbListedOffenceId1WithListingNumber2);
        dbListedOffences.add(dbListedOffenceId2WithListingNumber3);
        final UUID offenceId3 = UUID.randomUUID();
        final Offence offenceId2WithListingNumber1 = getOffence(offenceId3, 1);

        final Offence offenceToAddOrReplace = offenceComparator.getLatestVersionOfOffence(offenceId2WithListingNumber1, dbListedOffences);
        assertThat(offenceToAddOrReplace.getListingNumber(), CoreMatchers.is(1));
    }

    @Test
    public void shouldReturnPreviousDBVersionOfOffence() {
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        final Offence dbListedOffenceId1WithListingNumber2 = getOffence(offenceId1, 2);
        final Offence dbListedOffenceId2WithListingNumber3 = getOffence(offenceId2, 3);
        final List<Offence> dbListedOffences = new ArrayList();
        dbListedOffences.add(dbListedOffenceId1WithListingNumber2);
        dbListedOffences.add(dbListedOffenceId2WithListingNumber3);

        final Offence offenceId2WithListingNumber1 = getOffence(offenceId2, 1);

        final Offence offenceToAddOrReplace = offenceComparator.getLatestVersionOfOffence(offenceId2WithListingNumber1, dbListedOffences);
        assertThat(offenceToAddOrReplace.getListingNumber(), CoreMatchers.is(3));
    }

    private Offence getOffence(final UUID id, final int listingNumber) {
        return Offence.offence().withId(id).withListingNumber(listingNumber).withStartDate("01").build();
    }
}