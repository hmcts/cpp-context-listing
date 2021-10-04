package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.Offence;

import java.util.List;
import java.util.Optional;


public class OffenceComparator {

    private static final int INITIAL_OFFENCE_LISTING_NUMBER = 0;

    /**
     * If the offence from db listing number < offence from event then use event offence otherwise use
     * offence from db
     * <p>
     * If the offence does not exist in DB then add the offence from event
     *
     * @param offence  an offence from the event
     * @param offences List of offences from the DB for the defendant
     * @return the  Offence based on greatest listing number
     * @see Offence
     */
    public Offence getLatestVersionOfOffence(final Offence offence, final List<Offence> offences) {

        final Optional<Offence> offenceFromOffencesList = offences.stream()
                .filter(ofence -> ofence.getId().equals(offence.getId())).findFirst();

        final int listOffenceListingNumber = offenceFromOffencesList.isPresent() ? getListingNumber(offenceFromOffencesList.get()) : INITIAL_OFFENCE_LISTING_NUMBER;

        if (getListingNumber(offence) >= listOffenceListingNumber) {
            return offence;
        }

        if (offenceFromOffencesList.isPresent()) {
            return offenceFromOffencesList.get();
        }

        return offence;
    }

    private int getListingNumber(final Offence offence) {
        return offence.getListingNumber().orElse(INITIAL_OFFENCE_LISTING_NUMBER);
    }
}
